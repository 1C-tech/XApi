import { Activity } from 'lucide-react';
import type { RateLimitDto } from '../../types/api';

interface RateLimitBarProps {
  rateLimit: RateLimitDto | null;
}

export function RateLimitBar({ rateLimit }: RateLimitBarProps) {
  if (!rateLimit) return null;

  const { limit, remaining, resetEpochSeconds } = rateLimit;
  const pct = limit > 0 ? Math.round((remaining / limit) * 100) : 0;

  const barColor =
    pct <= 15
      ? 'bg-[var(--color-danger)]'
      : pct <= 30
        ? 'bg-[var(--color-warning)]'
        : 'bg-[var(--color-success)]';

  const resetTime = new Date(resetEpochSeconds * 1000);
  const now = new Date();
  const diffMin = Math.max(0, Math.round((resetTime.getTime() - now.getTime()) / 60000));

  return (
    <div className="p-4 bg-secondary rounded-xl border border-border shadow-card-sm">
      <div className="flex items-center gap-2 mb-3">
        <Activity size={16} className="text-text-secondary" />
        <span className="text-sm font-semibold text-text-primary">API 限额</span>
      </div>

      {/* 进度条 */}
      <div className="h-2 bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden mb-2">
        <div
          className={`h-full rounded-full transition-all duration-500 ease-out ${barColor}`}
          style={{ width: `${pct}%` }}
        />
      </div>

      {/* 数字 */}
      <div className="flex justify-between items-center text-xs text-text-secondary">
        <span>
          剩余 <strong className="text-text-primary">{remaining}</strong> / {limit}
        </span>
        <span>{pct}%</span>
      </div>

      {/* 重置时间 */}
      <div className="mt-2 text-xs text-text-secondary">
        重置：{resetTime.toLocaleTimeString('zh-CN')}（约 {diffMin} 分钟）
      </div>
    </div>
  );
}
