package com.example.xapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AgentStockQuoteDto(
        String market,
        String symbol,
        String name,
        Double price,
        @JsonProperty("change_percent") Double changePercent,
        Double volume,
        @JsonProperty("updated_at") String updatedAt,
        String source
) {
}
