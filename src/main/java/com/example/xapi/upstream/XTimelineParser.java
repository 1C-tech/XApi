package com.example.xapi.upstream;

import com.example.xapi.dto.RateLimitDto;
import com.example.xapi.dto.TweetCommentsPage;
import com.example.xapi.dto.TweetDto;
import com.example.xapi.dto.TweetMediaDto;
import com.example.xapi.dto.UserTweetsPage;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class XTimelineParser {
    public UserTweetsPage parseUserTweetsPage(JsonNode root, HttpHeaders headers) {
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

        return new UserTweetsPage(tweets, bottomCursor, rateLimit(headers), root);
    }

    public TweetCommentsPage parseTweetCommentsPage(JsonNode root, String tweetId, HttpHeaders headers) {
        List<TweetDto> comments = new ArrayList<>();
        String bottomCursor = null;

        JsonNode instructions = root.path("data")
                .path("threaded_conversation_with_injections_v2")
                .path("instructions");

        if (instructions.isArray()) {
            for (JsonNode instruction : instructions) {
                if (!"TimelineAddEntries".equals(instruction.path("type").asText())) {
                    continue;
                }
                JsonNode entries = instruction.path("entries");
                if (!entries.isArray()) {
                    continue;
                }
                for (JsonNode entry : entries) {
                    JsonNode content = entry.path("content");
                    String cursorType = content.path("operation").path("cursorType").asText();
                    String entryId = entry.path("entryId").asText();
                    if ("Bottom".equals(cursorType) || entryId.startsWith("cursor-bottom")) {
                        bottomCursor = textOrNull(content.path("value"));
                        continue;
                    }

                    addCommentIfPresent(comments, content.path("itemContent").path("tweet_results").path("result"), tweetId);

                    JsonNode items = content.path("items");
                    if (items.isArray()) {
                        for (JsonNode item : items) {
                            JsonNode tweetResult = item.path("item")
                                    .path("itemContent")
                                    .path("tweet_results")
                                    .path("result");
                            addCommentIfPresent(comments, tweetResult, tweetId);
                        }
                    }
                }
            }
        }

        return new TweetCommentsPage(comments, bottomCursor, rateLimit(headers), root);
    }

    private static void addCommentIfPresent(List<TweetDto> comments, JsonNode result, String rootTweetId) {
        TweetDto tweet = toTweet(result);
        if (tweet == null || rootTweetId.equals(tweet.getId())) {
            return;
        }
        JsonNode legacy = unwrapTweet(result).path("legacy");
        String conversationId = textOrNull(legacy.path("conversation_id_str"));
        String replyToStatusId = textOrNull(legacy.path("in_reply_to_status_id_str"));
        if (rootTweetId.equals(conversationId) || rootTweetId.equals(replyToStatusId)) {
            comments.add(tweet);
        }
    }

    private static void addTweetIfPresent(List<TweetDto> tweets, JsonNode result) {
        TweetDto tweet = toTweet(result);
        if (tweet != null) {
            tweets.add(tweet);
        }
    }

    private static TweetDto toTweet(JsonNode result) {
        JsonNode tweetNode = unwrapTweet(result);
        JsonNode legacy = tweetNode.path("legacy");
        String id = firstText(
                tweetNode.path("rest_id"),
                legacy.path("id_str"),
                legacy.path("conversation_id_str")
        );
        String fullText = textOrNull(legacy.path("full_text"));
        if (id == null && fullText == null) {
            return null;
        }

        JsonNode user = tweetNode.path("core").path("user_results").path("result");
        JsonNode userCore = user.path("core");
        JsonNode userLegacy = user.path("legacy");
        JsonNode views = tweetNode.path("views");

        return new TweetDto(
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
                parseMedia(legacy),
                tweetNode
        );
    }

    private static List<TweetMediaDto> parseMedia(JsonNode legacy) {
        JsonNode mediaNodes = legacy.path("extended_entities").path("media");
        if (!mediaNodes.isArray()) {
            mediaNodes = legacy.path("entities").path("media");
        }
        if (!mediaNodes.isArray()) {
            return List.of();
        }

        List<TweetMediaDto> media = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (JsonNode node : mediaNodes) {
            String type = textOrNull(node.path("type"));
            String imageUrl = textOrNull(node.path("media_url_https"));
            if (!StringUtils.hasText(type) || !StringUtils.hasText(imageUrl) || !seen.add(type + ":" + imageUrl)) {
                continue;
            }

            JsonNode large = node.path("sizes").path("large");
            Integer width = large.path("w").isNumber() ? large.path("w").asInt() : null;
            Integer height = large.path("h").isNumber() ? large.path("h").asInt() : null;
            String altText = textOrNull(node.path("ext_alt_text"));

            media.add(new TweetMediaDto(
                    type,
                    "photo".equals(type) ? imageUrl : null,
                    imageUrl,
                    width,
                    height,
                    altText
            ));
        }
        return media;
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

    private static RateLimitDto rateLimit(HttpHeaders headers) {
        return new RateLimitDto(
                intHeader(headers, "x-rate-limit-limit"),
                intHeader(headers, "x-rate-limit-remaining"),
                longHeader(headers, "x-rate-limit-reset")
        );
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
