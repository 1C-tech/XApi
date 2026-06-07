package com.example.xapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AgentAskResponse(
        String answer,
        List<AgentStockQuoteDto> quotes,
        List<AgentPostDto> posts,
        @JsonProperty("used_tradingagents") boolean usedTradingAgents,
        List<String> warnings
) {
}
