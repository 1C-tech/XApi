import { Languages } from 'lucide-react';
import type { TweetDto } from '../../types/api';
import type { TranslationState } from '../../hooks/useTweetTranslations';
import { Avatar } from '../common/Avatar';
import { EngagementStats } from '../common/EngagementStats';
import { Button } from '../ui/Button';

interface TweetCardProps {
  tweet: TweetDto;
  translation?: TranslationState;
  onToggleTranslation: (tweetId: string, text: string, lang: string) => void;
}

function relativeTime(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSec = Math.floor(diffMs / 1000);
  const diffMin = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay = Math.floor(diffHour / 24);

  if (Number.isNaN(date.getTime())) return '';
  if (diffSec < 60) return '刚刚';
  if (diffMin < 60) return `${diffMin}分钟前`;
  if (diffHour < 24) return `${diffHour}小时前`;
  if (diffDay < 7) return `${diffDay}天前`;

  return date.toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric',
  });
}

export function TweetCard({
  tweet,
  translation,
  onToggleTranslation,
}: TweetCardProps) {
  const translationVisible = Boolean(translation?.visible);
  const buttonLabel = translation?.text && translationVisible ? '收起译文' : '翻译';

  return (
    <article
      className="
        bg-secondary rounded-xl border border-border
        p-4 shadow-card-sm
        transition-all duration-200 ease-out
        hover:shadow-card-md hover:border-text-secondary/20
      "
    >
      <div className="flex gap-3">
        <Avatar
          src={tweet.authorAvatarUrl}
          alt={tweet.authorName}
          size={48}
        />

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="font-semibold text-sm text-text-primary truncate max-w-[160px]">
              {tweet.authorName}
            </span>
            <span className="text-text-secondary text-sm truncate max-w-[120px]">
              @{tweet.authorScreenName}
            </span>
            <span className="text-text-secondary text-sm">·</span>
            <span className="text-text-secondary text-sm whitespace-nowrap">
              {relativeTime(tweet.createdAt)}
            </span>
          </div>

          <p className="mt-1.5 text-sm text-text-primary leading-relaxed whitespace-pre-wrap break-words">
            {tweet.fullText}
          </p>

          <div className="mt-3">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              loading={Boolean(translation?.loading)}
              onClick={() => onToggleTranslation(tweet.id, tweet.fullText, tweet.lang)}
              className="px-2.5 py-1 text-xs"
            >
              <Languages size={14} />
              {buttonLabel}
            </Button>
          </div>

          {translationVisible && translation?.text && (
            <div className="mt-3 rounded-lg border border-accent/15 bg-accent/5 p-3">
              <p className="text-xs font-semibold text-accent mb-1">中文译文</p>
              <p className="text-sm text-text-primary leading-relaxed whitespace-pre-wrap break-words">
                {translation.text}
              </p>
            </div>
          )}

          {translationVisible && translation?.error && (
            <div className="mt-3 rounded-lg border border-red-200 bg-red-50 p-3 dark:border-red-900/50 dark:bg-red-950/20">
              <p className="text-sm text-red-700 dark:text-red-300">
                {translation.error}
              </p>
            </div>
          )}

          <EngagementStats tweet={tweet} />
        </div>
      </div>
    </article>
  );
}
