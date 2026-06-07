package com.example.xapi.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TweetMediaDto {
    private final String type;
    private final String url;
    private final String previewImageUrl;
    private final Integer width;
    private final Integer height;
    private final String altText;

    @JsonCreator
    public TweetMediaDto(
            @JsonProperty("type") String type,
            @JsonProperty("url") String url,
            @JsonProperty("previewImageUrl") String previewImageUrl,
            @JsonProperty("width") Integer width,
            @JsonProperty("height") Integer height,
            @JsonProperty("altText") String altText
    ) {
        this.type = type;
        this.url = url;
        this.previewImageUrl = previewImageUrl;
        this.width = width;
        this.height = height;
        this.altText = altText;
    }

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getPreviewImageUrl() {
        return previewImageUrl;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public String getAltText() {
        return altText;
    }
}
