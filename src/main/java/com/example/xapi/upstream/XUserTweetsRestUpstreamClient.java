package com.example.xapi.upstream;

import com.example.xapi.api.XApiRequestException;
import com.example.xapi.config.XUserTweetsProperties;
import com.example.xapi.dto.RateLimitDto;
import com.example.xapi.dto.TweetDto;
import com.example.xapi.dto.UserTweetsPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class XUserTweetsRestUpstreamClient implements XUserTweetsUpstreamClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final XUserTweetsProperties properties;

    public XUserTweetsRestUpstreamClient(
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            XUserTweetsProperties properties
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public UserTweetsPage fetchUserTweets(String userId, int count, String cursor) {
        if (!StringUtils.hasText(properties.getBearerToken())) {
            throw new IllegalStateException("x.api.bearer-token must not be blank");
        }

        String uri = buildUri(userId, count, cursor);
        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.exchange(
                    URI.create(uri),
                    HttpMethod.GET,
                    new HttpEntity<>(buildHeaders()),
                    JsonNode.class
            );
        } catch (HttpStatusCodeException e) {
            throw new XApiRequestException(
                    "X API request failed, status=" + e.getStatusCode().value()
                            + ", body=" + e.getResponseBodyAsString(),
                    e
            );
        } catch (RestClientException e) {
            throw new XApiRequestException("X API request failed", e);
        }

        JsonNode body = response.getBody();
        if (body == null) {
            throw new XApiRequestException("X API returned empty body");
        }

        return parsePage(body, response.getHeaders());
    }

    private String buildUri(String userId, int count, String cursor) {
        try {
            Map<String, Object> variables = new LinkedHashMap<>();
            variables.put("userId", userId);
            variables.put("count", count);
            if (StringUtils.hasText(cursor)) {
                variables.put("cursor", cursor);
            }
            variables.put("includePromotedContent", true);
            variables.put("withQuickPromoteEligibilityTweetFields", true);
            variables.put("withVoice", true);

            String variablesJson = objectMapper.writeValueAsString(variables);
            String featuresJson = objectMapper.writeValueAsString(defaultFeatures());
            String fieldTogglesJson = objectMapper.writeValueAsString(Map.of("withArticlePlainText", false));

            return stripTrailingSlash(properties.getBaseUrl())
                    + "/graphql/" + properties.getEndpointId() + "/UserTweets"
                    + "?variables=" + encode(variablesJson)
                    + "&features=" + encode(featuresJson)
                    + "&fieldToggles=" + encode(fieldTogglesJson);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to build X API query JSON", e);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("accept", "*/*");
        headers.add("content-type", "application/json");
        headers.add("authorization", authorizationHeader(properties.getBearerToken()));
        headers.add("x-twitter-active-user", "yes");
        headers.add("x-twitter-client-language", properties.getLanguage());
        headers.add("x-client-transaction-id", newTransactionId());
        headers.add("referer", "https://x.com/");
        headers.add("user-agent", properties.getUserAgent());

        if (StringUtils.hasText(properties.getCookie())) {
            headers.add("cookie", properties.getCookie());
        }
        if (StringUtils.hasText(properties.getCsrfToken())) {
            headers.add("x-csrf-token", properties.getCsrfToken());
            headers.add("x-twitter-auth-type", "OAuth2Session");
        }
        if (StringUtils.hasText(properties.getGuestToken())) {
            headers.add("x-guest-token", properties.getGuestToken());
        }
        return headers;
    }

    private UserTweetsPage parsePage(JsonNode root, HttpHeaders headers) {
        List<TweetDto> tweets = new ArrayList<>();
        String bottomCursor = null;

        JsonNode instructions = root.path("data")
                .path("user")
                .path("result")
                .path("timeline")
                .path("timeline")
                .path("instructions");

        if (instructions.isArray()) {
            for (JsonNode instruction : instructions) {
                String type = instruction.path("type").asText();
                if ("TimelinePinEntry".equals(type)) {
                    JsonNode tweetResult = instruction.path("entry").path("content")
                            .path("itemContent").path("tweet_results").path("result");
                    addTweetIfPresent(tweets, tweetResult);
                } else if ("TimelineAddEntries".equals(type)) {
                    JsonNode entries = instruction.path("entries");
                    if (entries.isArray()) {
                        for (JsonNode entry : entries) {
                            JsonNode content = entry.path("content");
                            String cursorType = content.path("operation").path("cursorType").asText();
                            String entryId = entry.path("entryId").asText();
                            if ("Bottom".equals(cursorType) || entryId.startsWith("cursor-bottom")) {
                                bottomCursor = textOrNull(content.path("value"));
                                continue;
                            }

                            JsonNode tweetResult = content.path("itemContent")
                                    .path("tweet_results")
                                    .path("result");
                            addTweetIfPresent(tweets, tweetResult);
                        }
                    }
                }
            }
        }

        RateLimitDto rateLimit = new RateLimitDto(
                intHeader(headers, "x-rate-limit-limit"),
                intHeader(headers, "x-rate-limit-remaining"),
                longHeader(headers, "x-rate-limit-reset")
        );
        return new UserTweetsPage(tweets, bottomCursor, rateLimit, root);
    }

    private static void addTweetIfPresent(List<TweetDto> tweets, JsonNode result) {
        JsonNode tweetNode = unwrapTweet(result);
        JsonNode legacy = tweetNode.path("legacy");
        String id = firstText(
                tweetNode.path("rest_id"),
                legacy.path("id_str"),
                legacy.path("conversation_id_str")
        );
        String fullText = textOrNull(legacy.path("full_text"));
        if (id == null && fullText == null) {
            return;
        }

        JsonNode user = tweetNode.path("core").path("user_results").path("result");
        JsonNode userCore = user.path("core");
        JsonNode userLegacy = user.path("legacy");
        JsonNode views = tweetNode.path("views");

        tweets.add(new TweetDto(
                id,
                textOrNull(legacy.path("created_at")),
                fullText,
                textOrNull(legacy.path("lang")),
                legacy.path("favorite_count").asInt(0),
                legacy.path("retweet_count").asInt(0),
                legacy.path("reply_count").asInt(0),
                legacy.path("quote_count").asInt(0),
                legacy.path("bookmark_count").asInt(0),
                firstText(views.path("count"), legacy.path("ext_views").path("count")),
                firstText(userCore.path("name"), userLegacy.path("name")),
                firstText(userCore.path("screen_name"), userLegacy.path("screen_name")),
                firstText(user.path("avatar").path("image_url"), userLegacy.path("profile_image_url_https")),
                tweetNode
        ));
    }

    private static JsonNode unwrapTweet(JsonNode result) {
        if (result == null || result.isMissingNode() || result.isNull()) {
            return result;
        }
        if (result.has("tweet")) {
            return result.path("tweet");
        }
        if (result.has("tweet_results")) {
            return result.path("tweet_results").path("result");
        }
        return result;
    }

    private static Map<String, Object> defaultFeatures() {
        Map<String, Object> features = new LinkedHashMap<>();
        features.put("rweb_video_screen_enabled", false);
        features.put("rweb_cashtags_enabled", true);
        features.put("profile_label_improvements_pcf_label_in_post_enabled", true);
        features.put("responsive_web_profile_redirect_enabled", false);
        features.put("rweb_tipjar_consumption_enabled", false);
        features.put("verified_phone_label_enabled", false);
        features.put("creator_subscriptions_tweet_preview_api_enabled", true);
        features.put("responsive_web_graphql_timeline_navigation_enabled", true);
        features.put("responsive_web_graphql_skip_user_profile_image_extensions_enabled", false);
        features.put("premium_content_api_read_enabled", false);
        features.put("communities_web_enable_tweet_community_results_fetch", true);
        features.put("c9s_tweet_anatomy_moderator_badge_enabled", true);
        features.put("responsive_web_grok_analyze_button_fetch_trends_enabled", false);
        features.put("responsive_web_grok_analyze_post_followups_enabled", false);
        features.put("rweb_cashtags_composer_attachment_enabled", true);
        features.put("responsive_web_jetfuel_frame", true);
        features.put("responsive_web_grok_share_attachment_enabled", true);
        features.put("responsive_web_grok_annotations_enabled", true);
        features.put("articles_preview_enabled", true);
        features.put("responsive_web_edit_tweet_api_enabled", true);
        features.put("rweb_conversational_replies_downvote_enabled", false);
        features.put("graphql_is_translatable_rweb_tweet_is_translatable_enabled", true);
        features.put("view_counts_everywhere_api_enabled", true);
        features.put("longform_notetweets_consumption_enabled", true);
        features.put("responsive_web_twitter_article_tweet_consumption_enabled", true);
        features.put("content_disclosure_indicator_enabled", true);
        features.put("content_disclosure_ai_generated_indicator_enabled", true);
        features.put("responsive_web_grok_show_grok_translated_post", true);
        features.put("responsive_web_grok_analysis_button_from_backend", true);
        features.put("post_ctas_fetch_enabled", false);
        features.put("freedom_of_speech_not_reach_fetch_enabled", true);
        features.put("standardized_nudges_misinfo", true);
        features.put("tweet_with_visibility_results_prefer_gql_limited_actions_policy_enabled", true);
        features.put("longform_notetweets_rich_text_read_enabled", true);
        features.put("longform_notetweets_inline_media_enabled", false);
        features.put("responsive_web_grok_image_annotation_enabled", true);
        features.put("responsive_web_grok_imagine_annotation_enabled", true);
        features.put("responsive_web_grok_community_note_auto_translation_is_enabled", true);
        features.put("responsive_web_enhance_cards_enabled", false);
        return features;
    }

    private static String authorizationHeader(String bearerToken) {
        if (bearerToken.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            return bearerToken;
        }
        return "Bearer " + bearerToken;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String newTransactionId() {
        return UUID.randomUUID().toString().replace("-", "")
                + "/"
                + UUID.randomUUID().toString().replace("-", "");
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String firstText(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            String value = textOrNull(node);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return StringUtils.hasText(value) ? value : null;
    }

    private static Integer intHeader(HttpHeaders headers, String name) {
        String value = headers.getFirst(name);
        return StringUtils.hasText(value) ? Integer.valueOf(value) : null;
    }

    private static Long longHeader(HttpHeaders headers, String name) {
        String value = headers.getFirst(name);
        return StringUtils.hasText(value) ? Long.valueOf(value) : null;
    }
}

