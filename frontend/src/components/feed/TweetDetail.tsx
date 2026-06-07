import { ArrowLeft, MessageCircle } from 'lucide-react';
import { useEffect } from 'react';
import type { TweetDto } from '../../types/api';
import type { TranslationState } from '../../hooks/useTweetTranslations';
import { useTweetComments } from '../../hooks/useTweetComments';
import { Avatar } from '../common/Avatar';
import { EngagementStats } from '../common/EngagementStats';
import { Button } from '../ui/Button';
import { relativeTime, TweetMediaGrid } from './TweetCard';

interface TweetDetailProps {
  tweet: TweetDto;
  translation?: TranslationState;
  onBack: () => void;
  onToggleTranslation: (tweetId: string, text: string, lang: string) => void;
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

export function TweetDetail({
  tweet,
  translation,
  onBack,
  onToggleTranslation,
}: TweetDetailProps) {
  const comments = useTweetComments(tweet.id);
  const { loadInitial } = comments;
  const translationVisible = Boolean(translation?.visible);
  const buttonLabel = translation?.text && translationVisible ? '收起译文' : '翻译';

  useEffect(() => {
    loadInitial();
  }, [loadInitial]);

  return (
    <div className="space-y-3">
      <Button type="button" variant="ghost" size="sm" onClick={onBack} className="px-2.5">
        <ArrowLeft size={16} />
        返回首页
      </Button>

      <article className="rounded-xl border border-border bg-secondary p-4 shadow-card-sm">
        <div className="flex gap-3">
          <Avatar src={tweet.authorAvatarUrl} alt={tweet.authorName} size={48} />

          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-1.5">
              <span className="max-w-[180px] truncate text-sm font-semibold text-text-primary">
                {tweet.authorName}
              </span>
              <span className="max-w-[140px] truncate text-sm text-text-secondary">
                @{tweet.authorScreenName}
              </span>
              <span className="text-sm text-text-secondary">·</span>
              <span className="whitespace-nowrap text-sm text-text-secondary">
                {relativeTime(tweet.createdAt)}
              </span>
            </div>

            <p className="mt-2 whitespace-pre-wrap break-words text-base leading-relaxed text-text-primary">
              {tweet.fullText}
            </p>

            <TweetMediaGrid media={tweet.media || []} />

            <div className="mt-3">
              <Button
                type="button"
                variant="ghost"
                size="sm"
                loading={Boolean(translation?.loading)}
                onClick={() => onToggleTranslation(tweet.id, tweet.fullText, tweet.lang)}
                className="px-2.5 py-1 text-xs"
              >
                {buttonLabel}
              </Button>
            </div>

            {translationVisible && translation?.text && (
              <div className="mt-3 rounded-lg border border-accent/15 bg-accent/5 p-3">
                <p className="mb-1 text-xs font-semibold text-accent">中文译文</p>
                <p className="whitespace-pre-wrap break-words text-sm leading-relaxed text-text-primary">
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

      <section className="rounded-xl border border-border bg-secondary px-4 shadow-card-sm">
        <div className="flex items-center gap-2 py-3">
          <MessageCircle size={16} className="text-text-secondary" />
          <h3 className="text-sm font-semibold text-text-primary">评论</h3>
          {comments.loading && (
            <span className="text-xs text-text-secondary">加载中...</span>
          )}
        </div>

        {comments.error && (
          <p className="border-t border-border py-3 text-sm text-red-700 dark:text-red-300">
            {comments.error}
          </p>
        )}

        {!comments.loading && !comments.error && comments.comments.length === 0 && (
          <p className="border-t border-border py-3 text-sm text-text-secondary">
            暂无评论
          </p>
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
      </section>
    </div>
  );
}
