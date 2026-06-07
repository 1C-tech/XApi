import { useCallback, useRef, useState } from 'react';
import type { ApiError, TweetCommentsPage, TweetDto } from '../types/api';

interface TweetCommentsState {
  comments: TweetDto[];
  nextCursor: string | null;
  loading: boolean;
  loadingMore: boolean;
  error: string | null;
  expanded: boolean;
  hasMore: boolean;
}

interface UseTweetCommentsReturn extends TweetCommentsState {
  toggle: () => void;
  loadMore: () => void;
}

const PAGE_SIZE = 20;

export function useTweetComments(tweetId: string): UseTweetCommentsReturn {
  const [state, setState] = useState<TweetCommentsState>({
    comments: [],
    nextCursor: null,
    loading: false,
    loadingMore: false,
    error: null,
    expanded: false,
    hasMore: false,
  });

  const cursorRef = useRef<string | null>(null);
  const loadedRef = useRef(false);
  const abortRef = useRef<AbortController | null>(null);

  const fetchComments = useCallback(
    async (cursor: string | null, isLoadMore: boolean) => {
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
      }));

      try {
        const params = new URLSearchParams({ tweetId, count: String(PAGE_SIZE) });
        if (cursor) params.set('cursor', cursor);

        const response = await fetch(`/api/x/tweet-comments?${params}`, {
          signal: controller.signal,
        });

        if (!response.ok) {
          const errorBody: ApiError = await response.json().catch(() => ({
            timestamp: new Date().toISOString(),
            status: response.status,
            error: response.statusText,
            message: `HTTP ${response.status}: ${response.statusText}`,
          }));
          throw new Error(errorBody.message || `Request failed (${response.status})`);
        }

        const page: TweetCommentsPage = await response.json();
        loadedRef.current = true;
        cursorRef.current = page.nextCursor;

        setState((prev) => ({
          ...prev,
          comments: isLoadMore ? [...prev.comments, ...page.comments] : page.comments,
          nextCursor: page.nextCursor,
          loading: false,
          loadingMore: false,
          hasMore: page.nextCursor !== null && page.comments.length > 0,
          error: null,
        }));
      } catch (err: unknown) {
        if (err instanceof DOMException && err.name === 'AbortError') return;
        const message = err instanceof Error ? err.message : 'Unknown error';
        setState((prev) => ({
          ...prev,
          loading: false,
          loadingMore: false,
          error: message,
        }));
      }
    },
    [tweetId]
  );

  const toggle = useCallback(() => {
    setState((prev) => ({ ...prev, expanded: !prev.expanded }));
    if (!loadedRef.current) {
      fetchComments(null, false);
    }
  }, [fetchComments]);

  const loadMore = useCallback(() => {
    if (state.loadingMore || !state.hasMore) return;
    fetchComments(cursorRef.current, true);
  }, [fetchComments, state.loadingMore, state.hasMore]);

  return {
    ...state,
    toggle,
    loadMore,
  };
}
