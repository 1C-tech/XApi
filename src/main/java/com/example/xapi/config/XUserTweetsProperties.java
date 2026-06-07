package com.example.xapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "x.api")
public class XUserTweetsProperties {
    private String baseUrl = "https://api.x.com";
    private String endpointId = "54_zVtVXJlQtnIBrY2QSXQ";
    private String tweetDetailEndpointId = "H8OOoI-5ZE4NxgRr8lfyWg";
    private String bearerToken;
    private String cookie;
    private String csrfToken;
    private String guestToken;
    private String language = "zh-cn";
    private Duration cacheTtl = Duration.ofMinutes(5);
    private Duration staleTtl = Duration.ofMinutes(30);
    private Duration lockTtl = Duration.ofSeconds(15);
    private Duration lockWait = Duration.ofSeconds(2);
    private int rateLimitMinRemaining = 3;
    private Duration rateLimitSafetyWindow = Duration.ofSeconds(30);
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    public String getTweetDetailEndpointId() {
        return tweetDetailEndpointId;
    }

    public void setTweetDetailEndpointId(String tweetDetailEndpointId) {
        this.tweetDetailEndpointId = tweetDetailEndpointId;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public String getCsrfToken() {
        return csrfToken;
    }

    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }

    public String getGuestToken() {
        return guestToken;
    }

    public void setGuestToken(String guestToken) {
        this.guestToken = guestToken;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public Duration getStaleTtl() {
        return staleTtl;
    }

    public void setStaleTtl(Duration staleTtl) {
        this.staleTtl = staleTtl;
    }

    public Duration getLockTtl() {
        return lockTtl;
    }

    public void setLockTtl(Duration lockTtl) {
        this.lockTtl = lockTtl;
    }

    public Duration getLockWait() {
        return lockWait;
    }

    public void setLockWait(Duration lockWait) {
        this.lockWait = lockWait;
    }

    public int getRateLimitMinRemaining() {
        return rateLimitMinRemaining;
    }

    public void setRateLimitMinRemaining(int rateLimitMinRemaining) {
        this.rateLimitMinRemaining = rateLimitMinRemaining;
    }

    public Duration getRateLimitSafetyWindow() {
        return rateLimitSafetyWindow;
    }

    public void setRateLimitSafetyWindow(Duration rateLimitSafetyWindow) {
        this.rateLimitSafetyWindow = rateLimitSafetyWindow;
    }
}

