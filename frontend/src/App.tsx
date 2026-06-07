import { useEffect, useCallback, useState } from 'react';
import { useTheme } from './hooks/useTheme';
import { useUserTweets } from './hooks/useUserTweets';
import { useTweetTranslations } from './hooks/useTweetTranslations';
import type { TweetDto } from './types/api';
import { AppLayout } from './components/layout/AppLayout';
import { LeftSidebar } from './components/layout/LeftSidebar';
import { RightPanel } from './components/layout/RightPanel';
import { TweetFeed } from './components/feed/TweetFeed';
import { TweetDetail } from './components/feed/TweetDetail';
import { ErrorToast } from './components/common/ErrorToast';

export default function App() {
  const { theme, toggleTheme } = useTheme();
  const { translations, toggleTranslation } = useTweetTranslations();
  const [selectedTweet, setSelectedTweet] = useState<TweetDto | null>(null);
  const {
    tweets,
    nextCursor,
    rateLimit,
    cache,
    loading,
    loadingMore,
    error,
    hasMore,
    search,
    loadMore,
    clearError,
  } = useUserTweets();

  useEffect(() => {
    search('1940360837547565056');
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = useCallback(
    (userId: string) => {
      setSelectedTweet(null);
      search(userId);
    },
    [search]
  );

  const handleSelectTweet = useCallback((tweet: TweetDto) => {
    setSelectedTweet(tweet);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, []);

  const handleBackToFeed = useCallback(() => {
    setSelectedTweet(null);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, []);

  const cacheKeyParts = cache?.key?.split(':') ?? [];
  const currentUserId = cacheKeyParts.includes('user-tweets')
    ? cacheKeyParts[cacheKeyParts.indexOf('user-tweets') + 1] ?? ''
    : '';

  return (
    <>
      <AppLayout
        sidebar={
          <LeftSidebar
            theme={theme}
            onToggleTheme={toggleTheme}
            onSearch={handleSearch}
            loading={loading}
            activeUserId={currentUserId}
          />
        }
        main={
          <div>
            <div className="mb-6">
              <h2 className="text-xl font-bold text-text-primary">
                {selectedTweet ? '帖子详情' : '首页'}
              </h2>
              <p className="text-sm text-text-secondary mt-1">
                {selectedTweet
                  ? `@${selectedTweet.authorScreenName} 的帖子和评论`
                  : tweets.length > 0
                  ? `显示 ${tweets.length} 条推文${nextCursor ? ' · 还有更多' : ''}`
                  : '输入用户 ID 或选择财经博主加载推文'}
              </p>
            </div>

            {selectedTweet ? (
              <TweetDetail
                tweet={selectedTweet}
                translation={translations[selectedTweet.id]}
                onBack={handleBackToFeed}
                onToggleTranslation={toggleTranslation}
              />
            ) : (
              <TweetFeed
                tweets={tweets}
                loading={loading}
                loadingMore={loadingMore}
                hasMore={hasMore}
                onLoadMore={loadMore}
                translations={translations}
                onToggleTranslation={toggleTranslation}
                onSelectTweet={handleSelectTweet}
              />
            )}
          </div>
        }
        panel={
          <RightPanel
            rateLimit={rateLimit}
            cache={cache}
            tweetCount={tweets.length}
            userId={currentUserId}
          />
        }
      />

      {error && <ErrorToast message={error} onDismiss={clearError} />}
    </>
  );
}
