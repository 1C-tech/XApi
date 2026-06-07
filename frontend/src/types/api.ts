export interface TweetMediaDto {
  type: 'photo' | 'video' | 'animated_gif' | string;
  url?: string;
  previewImageUrl?: string;
  width?: number;
  height?: number;
  altText?: string;
}

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
  media: TweetMediaDto[];
}

export interface RateLimitDto {
  limit: number;
  remaining: number;
  resetEpochSeconds: number;
}

export interface CacheMetadataDto {
  hit: boolean;
  stale: boolean;
  ttlSeconds: number;
  key: string;
}

export interface UserTweetsPage {
  tweets: TweetDto[];
  nextCursor: string | null;
  rateLimit: RateLimitDto | null;
  cache: CacheMetadataDto | null;
}

export interface TweetCommentsPage {
  comments: TweetDto[];
  nextCursor: string | null;
  rateLimit: RateLimitDto | null;
  cache: CacheMetadataDto | null;
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  resetEpochSeconds?: number;
}
