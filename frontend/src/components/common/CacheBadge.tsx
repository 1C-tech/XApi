import { Database, Clock, Zap } from 'lucide-react';
import type { CacheMetadataDto } from '../../types/api';

interface CacheBadgeProps {
  cache: CacheMetadataDto | null;
}

export function CacheBadge({ cache }: CacheBadgeProps) {
  if (!cache) return null;

  const { hit, stale, ttlSeconds } = cache;

  // 确实缓存状态
  let icon: React.ReactNode;
  let label: string;
  let variant: 'info' | 'warning' | 'success';

  if (hit && !stale) {
    icon = <Database size={14} />;
    label = '缓存命中';
    variant = 'info';
  } else if (hit && stale) {
    icon = <Clock size={14} />;
    label = '过期缓存';
    variant = 'warning';
  } else {
    icon = <Zap size={14} />;
    label = '实时数据';
    variant = 'success';
  }

  const variantBg: Record<string, string> = {
    success: 'bg-green-50 border-green-200 text-green-700 dark:bg-green-900/30 dark:border-green-800 dark:text-green-400',
    warning: 'bg-orange-50 border-orange-200 text-orange-700 dark:bg-orange-900/30 dark:border-orange-800 dark:text-orange-400',
    info: 'bg-blue-50 border-blue-200 text-blue-700 dark:bg-blue-900/30 dark:border-blue-800 dark:text-blue-400',
  };

  const ttlMin = Math.max(0, Math.round(ttlSeconds / 60));

  return (
    <div className="p-4 bg-secondary rounded-xl border border-border shadow-card-sm">
      <div className="flex items-center gap-2 mb-3">
        {icon}
        <span className="text-sm font-semibold text-text-primary">缓存状态</span>
      </div>

      <span
        className={`
          inline-flex items-center gap-1.5 px-3 py-1 text-xs font-medium
          rounded-full border ${variantBg[variant]}
        `}
      >
        {label}
      </span>

      <div className="mt-2 text-xs text-text-secondary">
        TTL 剩余：约 {ttlMin} 分钟
      </div>
    </div>
  );
}
