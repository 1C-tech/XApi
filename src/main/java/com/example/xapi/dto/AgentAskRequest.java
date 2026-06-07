package com.example.xapi.dto;

import java.util.List;

public record AgentAskRequest(
        String message,
        String userId,
        List<String> symbols
) {
}
