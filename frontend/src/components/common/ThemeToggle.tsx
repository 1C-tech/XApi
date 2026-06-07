import { Sun, Moon } from 'lucide-react';

interface ThemeToggleProps {
  theme: 'light' | 'dark';
  onToggle: () => void;
}

export function ThemeToggle({ theme, onToggle }: ThemeToggleProps) {
  return (
    <button
      onClick={onToggle}
      title={theme === 'dark' ? '切换至明亮模式' : '切换至暗黑模式'}
      className="
        relative w-10 h-10 rounded-full
        flex items-center justify-center
        bg-tertiary border border-border
        text-text-secondary hover:text-text-primary
        transition-all duration-300 ease-out
        hover:shadow-card-sm active:scale-95
      "
    >
      <div
        className={`
          absolute transition-all duration-300 ease-out
          ${theme === 'dark' ? 'rotate-0 scale-100' : 'rotate-90 scale-0'}
        `}
      >
        <Moon size={18} />
      </div>
      <div
        className={`
          absolute transition-all duration-300 ease-out
          ${theme === 'light' ? 'rotate-0 scale-100' : '-rotate-90 scale-0'}
        `}
      >
        <Sun size={18} />
      </div>
    </button>
  );
}
