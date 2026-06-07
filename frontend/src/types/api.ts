// ===== 推文 DTO =====
export interface TweetDto {
  id: string;
  createdAt: string;
  fullText: string;
  lang: string;
  favoriteCount: number;
  retweetCount: number;
  replyCount: number;
  quoteCount: number;
  bookmarkCount: number;
  viewCount: string;
  authorName: string;
  authorScreenName: string;
  authorAvatarUrl: string;
}

// ===== 限流信息 =====
export interface RateLimitDto {
  limit: number;
  remaining: number;
  resetEpochSeconds: number;
}

// ===== 缓存元信息 =====
export interface CacheMetadataDto {
  hit: boolean;
  stale: boolean;
  ttlSeconds: number;
  key: string;
}

// ===== 分页响应 =====
export interface UserTweetsPage {
  tweets: TweetDto[];
  nextCursor: string | null;
  rateLimit: RateLimitDto | null;
  cache: CacheMetadataDto | null;
}

// ===== API 错误响应 =====
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  resetEpochSeconds?: number;
}
