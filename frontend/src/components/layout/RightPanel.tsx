import { Hash } from 'lucide-react';
import type { RateLimitDto, CacheMetadataDto } from '../../types/api';
import { RateLimitBar } from '../common/RateLimitBar';
import { CacheBadge } from '../common/CacheBadge';
import { StockAgentPanel } from '../agent/StockAgentPanel';

interface RightPanelProps {
  rateLimit: RateLimitDto | null;
  cache: CacheMetadataDto | null;
  tweetCount: number;
  userId: string;
}

export function RightPanel({
  rateLimit,
  cache,
  tweetCount,
  userId,
}: RightPanelProps) {
  return (
    <aside
      className="
        hidden xl:flex flex-col
        w-[320px] h-screen sticky top-0
        border-l border-border
        bg-secondary/50 backdrop-blur-sm
        px-4 py-6 gap-4
        overflow-y-auto
      "
    >
      {userId && (
        <div className="p-4 bg-secondary rounded-xl border border-border shadow-card-sm">
          <div className="flex items-center gap-2 mb-3">
            <Hash size={16} className="text-text-secondary" />
            <span className="text-sm font-semibold text-text-primary">当前查询</span>
          </div>
          <div className="text-xs text-text-secondary space-y-1.5">
            <div className="flex justify-between gap-3">
              <span>用户 ID</span>
              <span className="text-text-primary font-mono text-xs truncate max-w-[180px]" title={userId}>
                {userId}
              </span>
            </div>
            <div className="flex justify-between">
              <span>已加载</span>
              <span className="text-text-primary font-mono">{tweetCount} 条推文</span>
            </div>
          </div>
        </div>
      )}

      <StockAgentPanel currentUserId={userId} />
      <RateLimitBar rateLimit={rateLimit} />
      <CacheBadge cache={cache} />

      <div className="p-4 bg-accent/5 rounded-xl border border-accent/10">
        <p className="text-xs text-text-secondary leading-relaxed">
          数据来自 X API，通过 Redis 缓存加速访问。频率限制由后端自动管理，右侧状态显示剩余调用次数和缓存情况。
        </p>
      </div>
    </aside>
  );
}
