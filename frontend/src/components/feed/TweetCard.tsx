import { Image as ImageIcon, Languages, MessageCircle, Play } from 'lucide-react';
import type { TweetDto, TweetMediaDto } from '../../types/api';
import type { TranslationState } from '../../hooks/useTweetTranslations';
import { useTweetComments } from '../../hooks/useTweetComments';
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
  if (diffMin < 60) return `${diffMin} 分钟前`;
  if (diffHour < 24) return `${diffHour} 小时前`;
  if (diffDay < 7) return `${diffDay} 天前`;

  return date.toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric',
  });
}

function mediaSrc(media: TweetMediaDto): string | undefined {
  return media.url || media.previewImageUrl;
}

function TweetMediaGrid({ media }: { media: TweetMediaDto[] }) {
  const visibleMedia = media.filter(mediaSrc).slice(0, 4);
  if (visibleMedia.length === 0) return null;

  const gridClass =
    visibleMedia.length === 1
      ? 'grid-cols-1'
      : visibleMedia.length === 2
        ? 'grid-cols-2'
        : 'grid-cols-2';

  return (
    <div
      className={`mt-3 grid ${gridClass} gap-1 overflow-hidden rounded-lg border border-border bg-tertiary`}
    >
      {visibleMedia.map((item, index) => {
        const src = mediaSrc(item);
        const isVideo = item.type === 'video' || item.type === 'animated_gif';
        return (
          <a
            key={`${src}-${index}`}
            href={src}
            target="_blank"
            rel="noreferrer"
            className={`
              group relative block bg-tertiary
              ${visibleMedia.length === 1 ? 'aspect-[16/10]' : 'aspect-square'}
              ${visibleMedia.length === 3 && index === 0 ? 'row-span-2' : ''}
            `}
            title={item.altText || item.type}
          >
            <img
              src={src}
              alt={item.altText || 'tweet media'}
              className="h-full w-full object-cover transition-transform duration-200 group-hover:scale-[1.02]"
              loading="eager"
            />
            {isVideo && (
              <span className="absolute left-2 top-2 inline-flex h-7 w-7 items-center justify-center rounded-full bg-black/65 text-white">
                <Play size={14} fill="currentColor" />
              </span>
            )}
            {visibleMedia.length === 4 && index === 3 && media.length > 4 && (
              <span className="absolute inset-0 flex items-center justify-center bg-black/55 text-sm font-semibold text-white">
                +{media.length - 4}
              </span>
            )}
          </a>
        );
      })}
    </div>
  );
}

function CommentItem({ comment }: { comment: TweetDto }) {
  return (
    <div className="flex gap-2 border-t border-border py-3 first:border-t-0">
      <Avatar src={comment.authorAvatarUrl} alt={comment.authorName} size={32} />
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-1 text-xs">
          <span className="max-w-[140px] truncate font-semibold text-text-primary">
            {comment.authorName}
          </span>
          <span className="max-w-[110px] truncate text-text-secondary">
            @{comment.authorScreenName}
          </span>
          <span className="text-text-secondary">·</span>
          <span className="whitespace-nowrap text-text-secondary">
            {relativeTime(comment.createdAt)}
          </span>
        </div>
        <p className="mt-1 whitespace-pre-wrap break-words text-sm leading-relaxed text-text-primary">
          {comment.fullText}
        </p>
        <TweetMediaGrid media={comment.media || []} />
        <EngagementStats tweet={comment} />
      </div>
    </div>
  );
}

function TweetComments({ tweetId }: { tweetId: string }) {
  const comments = useTweetComments(tweetId);

  return (
    <div className="mt-3">
      <Button
        type="button"
        variant="ghost"
        size="sm"
        loading={comments.loading}
        onClick={comments.toggle}
        className="px-2.5 py-1 text-xs"
      >
        <MessageCircle size={14} />
        {comments.expanded ? '收起评论' : '展开评论'}
      </Button>

      {comments.expanded && (
        <div className="mt-3 rounded-lg border border-border bg-primary/40 px-3">
          {comments.error && (
            <p className="py-3 text-sm text-red-700 dark:text-red-300">
              {comments.error}
            </p>
          )}

          {!comments.loading && !comments.error && comments.comments.length === 0 && (
            <p className="py-3 text-sm text-text-secondary">暂无评论</p>
          )}

          {comments.comments.map((comment) => (
            <CommentItem key={comment.id} comment={comment} />
          ))}

          {comments.hasMore && (
            <div className="flex justify-center border-t border-border py-3">
              <Button
                type="button"
                variant="secondary"
                size="sm"
                loading={comments.loadingMore}
                onClick={comments.loadMore}
              >
                加载更多评论
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
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

          <TweetMediaGrid media={tweet.media || []} />

          <div className="mt-3 flex flex-wrap items-center gap-2">
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

            {tweet.media?.length > 0 && (
              <span className="inline-flex items-center gap-1 text-xs text-text-secondary">
                <ImageIcon size={14} />
                {tweet.media.length}
              </span>
            )}
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
          <TweetComments tweetId={tweet.id} />
        </div>
      </div>
    </article>
  );
}
