package com.example.xapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AgentPostDto(
        String id,
        @JsonProperty("author_name") String authorName,
        @JsonProperty("author_screen_name") String authorScreenName,
        String text,
        @JsonProperty("created_at") String createdAt
) {
}
