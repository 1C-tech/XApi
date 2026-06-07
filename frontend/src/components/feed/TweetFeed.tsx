import { Inbox } from 'lucide-react';
import type { TweetDto } from '../../types/api';
import type { TranslationState } from '../../hooks/useTweetTranslations';
import { TweetCard } from './TweetCard';
import { TweetSkeleton } from './TweetSkeleton';
import { Button } from '../ui/Button';

interface TweetFeedProps {
  tweets: TweetDto[];
  loading: boolean;
  loadingMore: boolean;
  hasMore: boolean;
  onLoadMore: () => void;
  translations: Record<string, TranslationState>;
  onToggleTranslation: (tweetId: string, text: string, lang: string) => void;
  onSelectTweet: (tweet: TweetDto) => void;
}

export function TweetFeed({
  tweets,
  loading,
  loadingMore,
  hasMore,
  onLoadMore,
  translations,
  onToggleTranslation,
  onSelectTweet,
}: TweetFeedProps) {
  if (loading && tweets.length === 0) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, i) => (
          <TweetSkeleton key={i} />
        ))}
      </div>
    );
  }

  if (!loading && tweets.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-20 text-center">
        <div className="w-16 h-16 rounded-full bg-tertiary flex items-center justify-center mb-4">
          <Inbox size={28} className="text-text-secondary" />
        </div>
        <p className="text-text-primary font-medium text-lg">暂无推文</p>
        <p className="text-text-secondary text-sm mt-1">
          输入用户 ID 或选择财经博主开始浏览
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {tweets.map((tweet) => (
        <TweetCard
          key={tweet.id}
          tweet={tweet}
          translation={translations[tweet.id]}
          onToggleTranslation={onToggleTranslation}
          onSelectTweet={onSelectTweet}
        />
      ))}

      {loadingMore && (
        <div className="space-y-3">
          {Array.from({ length: 2 }).map((_, i) => (
            <TweetSkeleton key={`more-skeleton-${i}`} />
          ))}
        </div>
      )}

      {hasMore && !loadingMore && (
        <div className="flex justify-center py-2">
          <Button variant="secondary" onClick={onLoadMore} size="lg">
            加载更多推文
          </Button>
        </div>
      )}

      {!hasMore && tweets.length > 0 && (
        <p className="text-center text-text-secondary text-sm py-6">
          已加载全部推文
        </p>
      )}
    </div>
  );
}
