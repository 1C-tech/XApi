from __future__ import annotations

import httpx

from .market_tools import MarketDataClient, extract_symbols
from .models import AgentAskRequest, AgentAskResponse, StockQuote
from .post_tools import XPostClient
from .tradingagents_adapter import should_use_tradingagents, tradingagents_available


class StockAgentService:
    def __init__(
        self,
        market_client: MarketDataClient | None = None,
        post_client: XPostClient | None = None,
    ) -> None:
        self.market_client = market_client or MarketDataClient()
        self.post_client = post_client or XPostClient()

    def ask(self, request: AgentAskRequest) -> AgentAskResponse:
        warnings: list[str] = []
        quotes = []
        for symbol in extract_symbols(request.message, request.symbols):
            try:
                quotes.append(self.market_client.get_quote(symbol))
            except httpx.HTTPError as exc:
                quotes.append(StockQuote(
                    market=symbol.market,
                    symbol=symbol.symbol,
                    name=symbol.symbol,
                    source="unavailable",
                ))
                warnings.append(f"{symbol.symbol} 行情数据暂不可用：{exc.__class__.__name__}")

        posts = []
        if request.user_id:
            try:
                posts = self.post_client.get_user_posts(request.user_id)
            except httpx.HTTPError as exc:
                warnings.append(f"用户 {request.user_id} 帖子暂不可用：{exc.__class__.__name__}")

        use_tradingagents = should_use_tradingagents(request.message)
        if use_tradingagents and not tradingagents_available():
            warnings.append("TradingAgents 源码已放入 vendor，但当前 Python 环境未安装 tradingagents 包，已使用轻量工具回答。")
            use_tradingagents = False

        answer = self._summarize(request.message, quotes, posts, use_tradingagents)
        return AgentAskResponse(
            answer=answer,
            quotes=quotes,
            posts=posts,
            used_tradingagents=use_tradingagents,
            warnings=warnings,
        )

    @staticmethod
    def _summarize(message: str, quotes, posts, used_tradingagents: bool) -> str:
        lines = [f"已处理问题：{message}"]
        if quotes:
            lines.append("股票数据：")
            for quote in quotes:
                price = "暂无价格" if quote.price is None else f"{quote.price:g}"
                change = "" if quote.change_percent is None else f"，涨跌幅 {quote.change_percent:g}%"
                lines.append(f"- {quote.symbol}（{quote.market}）：{price}{change}，来源 {quote.source}")
        if posts:
            lines.append("指定用户帖子：")
            for post in posts[:3]:
                author = f"@{post.author_screen_name}" if post.author_screen_name else "用户"
                lines.append(f"- {author}: {post.text[:160]}")
        if not quotes and not posts:
            lines.append("没有识别到股票代码，也没有提供 userId。")
        if used_tradingagents:
            lines.append("已启用 TradingAgents 深度分析。")
        return "\n".join(lines)
