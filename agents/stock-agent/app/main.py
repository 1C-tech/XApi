from fastapi import FastAPI

from .agent_service import StockAgentService
from .models import AgentAskRequest, AgentAskResponse

app = FastAPI(title="XApi Stock Agent")
service = StockAgentService()


@app.post("/ask", response_model=AgentAskResponse)
def ask(request: AgentAskRequest) -> AgentAskResponse:
    return service.ask(request)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}
