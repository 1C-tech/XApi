import { useState } from 'react';
import { User } from 'lucide-react';

interface AvatarProps {
  src: string;
  alt: string;
  size?: number;
  className?: string;
}

export function Avatar({ src, alt, size = 48, className = '' }: AvatarProps) {
  const [error, setError] = useState(false);

  if (error || !src) {
    return (
      <div
        className={`
          rounded-full bg-gray-200 dark:bg-gray-700
          flex items-center justify-center
          flex-shrink-0 ${className}
        `}
        style={{ width: size, height: size }}
        title={alt}
      >
        <User
          size={size * 0.5}
          className="text-gray-400 dark:text-gray-500"
        />
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      width={size}
      height={size}
      onError={() => setError(true)}
      className={`
        rounded-full object-cover flex-shrink-0
        bg-gray-200 dark:bg-gray-700
        ${className}
      `}
      style={{ width: size, height: size }}
    />
  );
}
