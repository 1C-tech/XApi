import { useState, useCallback, useRef } from 'react';
import type { UserTweetsPage, ApiError } from '../types/api';

interface UseUserTweetsState {
  tweets: UserTweetsPage['tweets'];
  nextCursor: string | null;
  rateLimit: UserTweetsPage['rateLimit'];
  cache: UserTweetsPage['cache'];
  loading: boolean;
  loadingMore: boolean;
  error: string | null;
  hasMore: boolean;
}

interface UseUserTweetsReturn extends UseUserTweetsState {
  search: (userId: string) => void;
  loadMore: () => void;
  clearError: () => void;
}

const DEFAULT_USER_ID = '1940360837547565056';
const PAGE_SIZE = 20;

export function useUserTweets(initialUserId = DEFAULT_USER_ID): UseUserTweetsReturn {
  const [state, setState] = useState<UseUserTweetsState>({
    tweets: [],
    nextCursor: null,
    rateLimit: null,
    cache: null,
    loading: false,
    loadingMore: false,
    error: null,
    hasMore: true,
  });

  const userIdRef = useRef(initialUserId);
  const cursorRef = useRef<string | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  const fetchTweets = useCallback(
    async (userId: string, cursor: string | null, isLoadMore: boolean) => {
      if (abortRef.current) {
        abortRef.current.abort();
      }
      const controller = new AbortController();
      abortRef.current = controller;

      const loadingKey = isLoadMore ? 'loadingMore' : 'loading';
      setState((prev) => ({
        ...prev,
        [loadingKey]: true,
        error: null,
        ...(isLoadMore ? {} : { tweets: [], nextCursor: null, hasMore: true }),
      }));

      try {
        const params = new URLSearchParams({ userId, count: String(PAGE_SIZE) });
        if (cursor) params.set('cursor', cursor);

        const response = await fetch(`/api/x/user-tweets?${params}`, {
          signal: controller.signal,
        });

        if (!response.ok) {
          const errorBody: ApiError = await response.json().catch(() => ({
            timestamp: new Date().toISOString(),
            status: response.status,
            error: response.statusText,
            message: `HTTP ${response.status}: ${response.statusText}`,
          }));
          throw new Error(errorBody.message || `请求失败 (${response.status})`);
        }

        const page: UserTweetsPage = await response.json();

        setState((prev) => ({
          ...prev,
          tweets: isLoadMore ? [...prev.tweets, ...page.tweets] : page.tweets,
          nextCursor: page.nextCursor,
          rateLimit: page.rateLimit ?? prev.rateLimit,
          cache: page.cache ?? prev.cache,
          loading: false,
          loadingMore: false,
          hasMore: page.nextCursor !== null && page.tweets.length > 0,
          error: null,
        }));

        cursorRef.current = page.nextCursor;
      } catch (err: unknown) {
        if (err instanceof DOMException && err.name === 'AbortError') return;
        const message = err instanceof Error ? err.message : '未知错误';
        setState((prev) => ({
          ...prev,
          loading: false,
          loadingMore: false,
          error: message,
        }));
      }
    },
    []
  );

  const search = useCallback(
    (userId: string) => {
      userIdRef.current = userId;
      cursorRef.current = null;
      fetchTweets(userId, null, false);
    },
    [fetchTweets]
  );

  const loadMore = useCallback(() => {
    if (state.loadingMore || !state.hasMore) return;
    fetchTweets(userIdRef.current, cursorRef.current, true);
  }, [fetchTweets, state.loadingMore, state.hasMore]);

  const clearError = useCallback(() => {
    setState((prev) => ({ ...prev, error: null }));
  }, []);

  return {
    ...state,
    search,
    loadMore,
    clearError,
  };
}
