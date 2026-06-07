import { useEffect, useCallback, useState } from 'react';
import { useTheme } from './hooks/useTheme';
import { useUserTweets } from './hooks/useUserTweets';
import { useTweetTranslations } from './hooks/useTweetTranslations';
import { AppLayout } from './components/layout/AppLayout';
import { LeftSidebar } from './components/layout/LeftSidebar';
import { RightPanel } from './components/layout/RightPanel';
import { TweetFeed } from './components/feed/TweetFeed';
import { ErrorToast } from './components/common/ErrorToast';
import { StockAgentPanel } from './components/agent/StockAgentPanel';

export default function App() {
  const { theme, toggleTheme } = useTheme();
  const { translations, toggleTranslation } = useTweetTranslations();
  const [selectedUserId, setSelectedUserId] = useState('1940360837547565056');
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
    search(selectedUserId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = useCallback(
    (userId: string) => {
      setSelectedUserId(userId);
      search(userId);
    },
    [search]
  );

  const currentUserId = selectedUserId;

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
              <h2 className="text-xl font-bold text-text-primary">首页</h2>
              <p className="text-sm text-text-secondary mt-1">
                {tweets.length > 0
                  ? `显示 ${tweets.length} 条推文${nextCursor ? ' · 还有更多' : ''}`
                  : '输入用户 ID 或选择财经博主加载推文'}
              </p>
            </div>

            <StockAgentPanel currentUserId={currentUserId} />

            <TweetFeed
              tweets={tweets}
              loading={loading}
              loadingMore={loadingMore}
              hasMore={hasMore}
              onLoadMore={loadMore}
              translations={translations}
              onToggleTranslation={toggleTranslation}
            />
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
