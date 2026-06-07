from __future__ import annotations

import os
import re
from dataclasses import dataclass

import httpx

from .models import StockQuote


US_SYMBOL_RE = re.compile(r"\b[A-Z]{1,5}\b")
A_SHARE_RE = re.compile(r"\b(?:[036]\d{5})(?:\.(?:SH|SZ))?\b", re.IGNORECASE)


@dataclass(frozen=True)
class MarketSymbol:
    market: str
    symbol: str


def normalize_symbol(symbol: str) -> MarketSymbol:
    cleaned = symbol.strip().upper()
    if re.fullmatch(r"[036]\d{5}(?:\.(?:SH|SZ))?", cleaned):
        code = cleaned.split(".")[0]
        suffix = cleaned.split(".")[1] if "." in cleaned else ("SH" if code.startswith("6") else "SZ")
        return MarketSymbol("A_SHARE", f"{code}.{suffix}")
    return MarketSymbol("US", cleaned)


def extract_symbols(message: str, explicit: list[str] | None = None) -> list[MarketSymbol]:
    seen: set[str] = set()
    result: list[MarketSymbol] = []
    candidates = list(explicit or [])
    a_share_matches = A_SHARE_RE.findall(message)
    candidates.extend(a_share_matches)
    us_scan_text = message
    for match in a_share_matches:
        us_scan_text = us_scan_text.replace(match, " ")
    candidates.extend(
        token for token in US_SYMBOL_RE.findall(us_scan_text)
        if token not in {"A", "X", "API", "LLM", "AI", "CEO", "ETF", "SH", "SZ"}
    )
    for candidate in candidates:
        symbol = normalize_symbol(candidate)
        key = f"{symbol.market}:{symbol.symbol}"
        if key not in seen:
            seen.add(key)
            result.append(symbol)
    return result


class MarketDataClient:
    def __init__(self, http_client: httpx.Client | None = None) -> None:
        self.http = http_client or httpx.Client(timeout=10.0)
        self.alpha_vantage_key = os.getenv("ALPHA_VANTAGE_API_KEY", "")

    def get_quote(self, symbol: MarketSymbol) -> StockQuote:
        if symbol.market == "A_SHARE":
            return self._get_a_share_quote(symbol.symbol)
        return self._get_us_quote(symbol.symbol)

    def _get_us_quote(self, symbol: str) -> StockQuote:
        if not self.alpha_vantage_key:
            return StockQuote(
                market="US",
                symbol=symbol,
                source="alpha_vantage",
                name=symbol,
            )
        response = self.http.get(
            "https://www.alphavantage.co/query",
            params={"function": "GLOBAL_QUOTE", "symbol": symbol, "apikey": self.alpha_vantage_key},
        )
        response.raise_for_status()
        quote = response.json().get("Global Quote", {})
        return StockQuote(
            market="US",
            symbol=symbol,
            name=symbol,
            price=_float_or_none(quote.get("05. price")),
            change_percent=_percent_or_none(quote.get("10. change percent")),
            volume=_float_or_none(quote.get("06. volume")),
            updated_at=quote.get("07. latest trading day"),
            source="alpha_vantage",
        )

    def _get_a_share_quote(self, symbol: str) -> StockQuote:
        code, suffix = symbol.split(".")
        secid = f"{'1' if suffix == 'SH' else '0'}.{code}"
        response = self.http.get(
            "https://push2.eastmoney.com/api/qt/stock/get",
            params={
                "secid": secid,
                "fields": "f43,f57,f58,f60,f170,f47,f86",
            },
        )
        response.raise_for_status()
        data = response.json().get("data") or {}
        return StockQuote(
            market="A_SHARE",
            symbol=symbol,
            name=data.get("f58") or symbol,
            price=_eastmoney_price(data.get("f43")),
            change_percent=_eastmoney_price(data.get("f170")),
            volume=_float_or_none(data.get("f47")),
            updated_at=str(data.get("f86")) if data.get("f86") else None,
            source="eastmoney",
        )


def _float_or_none(value: object) -> float | None:
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def _percent_or_none(value: object) -> float | None:
    if value is None:
        return None
    return _float_or_none(str(value).replace("%", ""))


def _eastmoney_price(value: object) -> float | None:
    parsed = _float_or_none(value)
    if parsed is None:
        return None
    return parsed / 100
