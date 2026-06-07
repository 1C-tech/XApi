import httpx

from app.agent_service import StockAgentService
from app.market_tools import MarketDataClient, extract_symbols
from app.models import AgentAskRequest
from app.post_tools import XPostClient


def test_extracts_us_and_a_share_symbols():
    symbols = extract_symbols("分析 AAPL 和 600519.SH，再看 000001")

    assert [symbol.symbol for symbol in symbols] == ["600519.SH", "000001.SZ", "AAPL"]


def test_parses_alpha_vantage_quote(monkeypatch):
    monkeypatch.setenv("ALPHA_VANTAGE_API_KEY", "demo")

    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"Global Quote": {
            "05. price": "199.12",
            "10. change percent": "1.23%",
            "06. volume": "1000",
            "07. latest trading day": "2026-06-05",
        }})

    client = MarketDataClient(httpx.Client(transport=httpx.MockTransport(handler)))

    quote = client.get_quote(extract_symbols("AAPL")[0])

    assert quote.market == "US"
    assert quote.symbol == "AAPL"
    assert quote.price == 199.12
    assert quote.change_percent == 1.23


def test_parses_eastmoney_a_share_quote():
    def handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"data": {
            "f43": 1688,
            "f58": "贵州茅台",
            "f170": 123,
            "f47": 999,
            "f86": 20260608103000,
        }})

    client = MarketDataClient(httpx.Client(transport=httpx.MockTransport(handler)))

    quote = client.get_quote(extract_symbols("600519.SH")[0])

    assert quote.market == "A_SHARE"
    assert quote.name == "贵州茅台"
    assert quote.price == 16.88
    assert quote.change_percent == 1.23


def test_agent_combines_quotes_and_posts(monkeypatch):
    monkeypatch.setenv("ALPHA_VANTAGE_API_KEY", "")

    def market_handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"data": {"f43": 1200, "f58": "平安银行", "f170": -50}})

    def post_handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(200, json={"tweets": [{
            "id": "1",
            "authorName": "Justin",
            "authorScreenName": "justinsuntron",
            "fullText": "TRON keeps building",
            "createdAt": "now",
        }]})

    service = StockAgentService(
        market_client=MarketDataClient(httpx.Client(transport=httpx.MockTransport(market_handler))),
        post_client=XPostClient(httpx.Client(transport=httpx.MockTransport(post_handler))),
    )

    response = service.ask(AgentAskRequest(message="分析 000001.SZ 和 AAPL", user_id="902839045356744704"))

    assert len(response.quotes) == 2
    assert response.posts[0].author_screen_name == "justinsuntron"
    assert "股票数据" in response.answer
    assert "指定用户帖子" in response.answer


def test_agent_keeps_answer_when_market_provider_fails():
    def market_handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(502, json={"message": "bad gateway"})

    service = StockAgentService(
        market_client=MarketDataClient(httpx.Client(transport=httpx.MockTransport(market_handler))),
        post_client=XPostClient(httpx.Client(transport=httpx.MockTransport(lambda request: httpx.Response(200, json={})))),
    )

    response = service.ask(AgentAskRequest(message="分析 000001.SZ"))

    assert response.quotes[0].symbol == "000001.SZ"
    assert response.quotes[0].price is None
    assert response.warnings


def test_agent_keeps_answer_when_post_provider_fails(monkeypatch):
    monkeypatch.setenv("ALPHA_VANTAGE_API_KEY", "")

    def post_handler(request: httpx.Request) -> httpx.Response:
        return httpx.Response(503, json={"message": "redis unavailable"})

    service = StockAgentService(
        market_client=MarketDataClient(httpx.Client(transport=httpx.MockTransport(lambda request: httpx.Response(200, json={})))),
        post_client=XPostClient(httpx.Client(transport=httpx.MockTransport(post_handler))),
    )

    response = service.ask(AgentAskRequest(message="分析 AAPL", user_id="902839045356744704"))

    assert response.quotes[0].symbol == "AAPL"
    assert response.posts == []
    assert response.warnings
