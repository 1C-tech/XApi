import {
  Heart,
  Repeat2,
  MessageCircle,
  Quote,
  Eye,
  Bookmark,
} from 'lucide-react';
import type { TweetDto } from '../../types/api';

interface EngagementStatsProps {
  tweet: TweetDto;
}

function formatCount(n: number | string): string {
  const num = typeof n === 'string' ? parseInt(n, 10) : n;
  if (isNaN(num)) return '0';
  if (num >= 10000) {
    const wan = num / 10000;
    return wan >= 100 ? `${Math.floor(wan)}万` : `${wan.toFixed(1)}万`;
  }
  if (num >= 1000) return `${(num / 1000).toFixed(1)}千`;
  return String(num);
}

interface StatItemProps {
  icon: React.ReactNode;
  count: number | string;
  label: string;
  colorClass: string;
}

function StatItem({ icon, count, label, colorClass }: StatItemProps) {
  return (
    <div
      className={`
        group flex items-center gap-1 cursor-pointer
        transition-colors duration-200 ${colorClass}
      `}
      title={`${label}: ${formatCount(count)}`}
    >
      <span className="transition-transform duration-200 group-hover:scale-110">
        {icon}
      </span>
      <span className="text-xs font-medium tabular-nums">
        {formatCount(count)}
      </span>
    </div>
  );
}

export function EngagementStats({ tweet }: EngagementStatsProps) {
  return (
    <div className="flex items-center gap-4 mt-3 pt-3 border-t border-border flex-wrap">
      <StatItem
        icon={<MessageCircle size={16} />}
        count={tweet.replyCount}
        label="回复"
        colorClass="text-text-secondary hover:text-accent"
      />
      <StatItem
        icon={<Repeat2 size={16} />}
        count={tweet.retweetCount}
        label="转发"
        colorClass="text-text-secondary hover:text-[var(--color-retweet)]"
      />
      <StatItem
        icon={<Heart size={16} />}
        count={tweet.favoriteCount}
        label="点赞"
        colorClass="text-text-secondary hover:text-[var(--color-like)]"
      />
      <StatItem
        icon={<Quote size={16} />}
        count={tweet.quoteCount}
        label="引用"
        colorClass="text-text-secondary hover:text-accent"
      />
      <StatItem
        icon={<Eye size={16} />}
        count={tweet.viewCount}
        label="浏览"
        colorClass="text-text-secondary hover:text-[var(--color-view)]"
      />
      <StatItem
        icon={<Bookmark size={16} />}
        count={tweet.bookmarkCount}
        label="收藏"
        colorClass="text-text-secondary hover:text-accent"
      />
    </div>
  );
}
