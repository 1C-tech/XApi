from pydantic import BaseModel, Field


class AgentAskRequest(BaseModel):
    message: str = Field(min_length=1)
    user_id: str | None = None
    symbols: list[str] | None = None


class StockQuote(BaseModel):
    market: str
    symbol: str
    name: str | None = None
    price: float | None = None
    change_percent: float | None = None
    volume: float | None = None
    updated_at: str | None = None
    source: str


class PostItem(BaseModel):
    id: str
    author_name: str | None = None
    author_screen_name: str | None = None
    text: str
    created_at: str | None = None


class AgentAskResponse(BaseModel):
    answer: str
    quotes: list[StockQuote] = Field(default_factory=list)
    posts: list[PostItem] = Field(default_factory=list)
    used_tradingagents: bool = False
    warnings: list[str] = Field(default_factory=list)
