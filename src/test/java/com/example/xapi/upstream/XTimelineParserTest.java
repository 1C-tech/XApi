package com.example.xapi.upstream;

import com.example.xapi.dto.TweetCommentsPage;
import com.example.xapi.dto.TweetDto;
import com.example.xapi.dto.UserTweetsPage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

class XTimelineParserTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XTimelineParser parser = new XTimelineParser();

    @Test
    void parsesPhotoAndVideoPreviewMediaFromUserTweets() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "data": {
                    "user": {
                      "result": {
                        "timeline": {
                          "timeline": {
                            "instructions": [
                              {
                                "type": "TimelineAddEntries",
                                "entries": [
                                  {
                                    "entryId": "tweet-1",
                                    "content": {
                                      "itemContent": {
                                        "tweet_results": {
                                          "result": {
                                            "rest_id": "1",
                                            "core": {
                                              "user_results": {
                                                "result": {
                                                  "core": { "name": "Author", "screen_name": "author" },
                                                  "avatar": { "image_url": "https://img.example/avatar.jpg" }
                                                }
                                              }
                                            },
                                            "legacy": {
                                              "created_at": "Sun Jun 07 10:00:00 +0000 2026",
                                              "full_text": "post with media",
                                              "lang": "en",
                                              "extended_entities": {
                                                "media": [
                                                  {
                                                    "type": "photo",
                                                    "media_url_https": "https://img.example/photo.jpg",
                                                    "sizes": { "large": { "w": 1200, "h": 800 } },
                                                    "ext_alt_text": "chart"
                                                  },
                                                  {
                                                    "type": "video",
                                                    "media_url_https": "https://img.example/video-preview.jpg",
                                                    "sizes": { "large": { "w": 1280, "h": 720 } }
                                                  }
                                                ]
                                              }
                                            }
                                          }
                                        }
                                      }
                                    }
                                  }
                                ]
                              }
                            ]
                          }
                        }
                      }
                    }
                  }
                }
                """);

        UserTweetsPage page = parser.parseUserTweetsPage(root, new HttpHeaders());

        assertThat(page.getTweets()).hasSize(1);
        TweetDto tweet = page.getTweets().get(0);
        assertThat(tweet.getMedia()).hasSize(2);
        assertThat(tweet.getMedia().get(0).getType()).isEqualTo("photo");
        assertThat(tweet.getMedia().get(0).getUrl()).isEqualTo("https://img.example/photo.jpg");
        assertThat(tweet.getMedia().get(0).getWidth()).isEqualTo(1200);
        assertThat(tweet.getMedia().get(0).getHeight()).isEqualTo(800);
        assertThat(tweet.getMedia().get(0).getAltText()).isEqualTo("chart");
        assertThat(tweet.getMedia().get(1).getType()).isEqualTo("video");
        assertThat(tweet.getMedia().get(1).getPreviewImageUrl()).isEqualTo("https://img.example/video-preview.jpg");
    }

    @Test
    void parsesTweetDetailRepliesAndBottomCursor() throws Exception {
        JsonNode root = objectMapper.readTree("""
                {
                  "data": {
                    "threaded_conversation_with_injections_v2": {
                      "instructions": [
                        {
                          "type": "TimelineAddEntries",
                          "entries": [
                            {
                              "entryId": "tweet-root",
                              "content": {
                                "itemContent": {
                                  "tweet_results": {
                                    "result": {
                                      "rest_id": "root",
                                      "legacy": {
                                        "conversation_id_str": "root",
                                        "full_text": "root post"
                                      }
                                    }
                                  }
                                }
                              }
                            },
                            {
                              "entryId": "conversationthread-root",
                              "content": {
                                "items": [
                                  {
                                    "item": {
                                      "itemContent": {
                                        "tweet_results": {
                                          "result": {
                                            "rest_id": "reply-1",
                                            "core": {
                                              "user_results": {
                                                "result": {
                                                  "core": { "name": "Reply Author", "screen_name": "replyauthor" }
                                                }
                                              }
                                            },
                                            "legacy": {
                                              "conversation_id_str": "root",
                                              "in_reply_to_status_id_str": "root",
                                              "created_at": "Sun Jun 07 10:01:00 +0000 2026",
                                              "full_text": "first reply",
                                              "lang": "en"
                                            }
                                          }
                                        }
                                      }
                                    }
                                  }
                                ]
                              }
                            },
                            {
                              "entryId": "cursor-bottom-1",
                              "content": {
                                "operation": { "cursorType": "Bottom" },
                                "value": "next-cursor"
                              }
                            }
                          ]
                        }
                      ]
                    }
                  }
                }
                """);

        TweetCommentsPage page = parser.parseTweetCommentsPage(root, "root", new HttpHeaders());

        assertThat(page.getComments()).extracting(TweetDto::getId).containsExactly("reply-1");
        assertThat(page.getComments().get(0).getFullText()).isEqualTo("first reply");
        assertThat(page.getNextCursor()).isEqualTo("next-cursor");
    }
}
