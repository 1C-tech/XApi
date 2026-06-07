import { Skeleton } from '../ui/Skeleton';

export function TweetSkeleton() {
  return (
    <div className="bg-secondary rounded-xl border border-border p-4 shadow-card-sm">
      <div className="flex gap-3">
        {/* 头像骨架 */}
        <Skeleton className="w-12 h-12" rounded="full" />

        {/* 内容骨架 */}
        <div className="flex-1 space-y-3">
          {/* 作者行 */}
          <div className="flex items-center gap-2">
            <Skeleton className="h-4 w-24" rounded="sm" />
            <Skeleton className="h-3 w-20" rounded="sm" />
            <Skeleton className="h-3 w-12" rounded="sm" />
          </div>

          {/* 正文 */}
          <Skeleton className="h-4 w-full" rounded="sm" />
          <Skeleton className="h-4 w-3/4" rounded="sm" />
          <Skeleton className="h-4 w-1/2" rounded="sm" />

          {/* 互动行 */}
          <div className="flex items-center gap-4 pt-3 border-t border-border">
            <Skeleton className="h-4 w-10" rounded="sm" />
            <Skeleton className="h-4 w-10" rounded="sm" />
            <Skeleton className="h-4 w-10" rounded="sm" />
            <Skeleton className="h-4 w-10" rounded="sm" />
            <Skeleton className="h-4 w-10" rounded="sm" />
            <Skeleton className="h-4 w-10" rounded="sm" />
          </div>
        </div>
      </div>
    </div>
  );
}
