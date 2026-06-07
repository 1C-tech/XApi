from __future__ import annotations

import importlib.util
from pathlib import Path


def tradingagents_available() -> bool:
    root = Path(__file__).resolve().parents[1] / "vendor" / "TradingAgents"
    return (root / "tradingagents").exists() and importlib.util.find_spec("tradingagents") is not None


def should_use_tradingagents(message: str) -> bool:
    lower = message.lower()
    return any(keyword in lower for keyword in ["深度分析", "复杂分析", "tradingagents", "多agent", "multi-agent"])
