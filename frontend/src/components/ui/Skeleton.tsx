interface SkeletonProps {
  className?: string;
  rounded?: 'sm' | 'md' | 'lg' | 'full';
}

const roundedMap: Record<string, string> = {
  sm: 'rounded-sm',
  md: 'rounded-md',
  lg: 'rounded-lg',
  full: 'rounded-full',
};

export function Skeleton({ className = '', rounded = 'md' }: SkeletonProps) {
  return (
    <div
      className={`
        animate-skeleton bg-gray-200 dark:bg-gray-700
        ${roundedMap[rounded]}
        ${className}
      `}
    />
  );
}
