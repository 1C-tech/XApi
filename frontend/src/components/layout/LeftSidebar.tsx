import { Compass, Home, Settings } from 'lucide-react';
import { ThemeToggle } from '../common/ThemeToggle';
import { UserSearch } from '../common/UserSearch';
import { financeBloggers } from '../../data/financeBloggers';

interface LeftSidebarProps {
  theme: 'light' | 'dark';
  onToggleTheme: () => void;
  onSearch: (userId: string) => void;
  loading: boolean;
  activeUserId: string;
}

const navItems = [
  { icon: Home, label: '首页', active: true },
  { icon: Compass, label: '探索', active: false },
  { icon: Settings, label: '设置', active: false },
];

export function LeftSidebar({
  theme,
  onToggleTheme,
  onSearch,
  loading,
  activeUserId,
}: LeftSidebarProps) {
  return (
    <aside
      className="
        hidden lg:flex flex-col
        w-[280px] h-screen sticky top-0
        border-r border-border
        bg-secondary/50 backdrop-blur-sm
        px-4 py-6
      "
    >
      <div className="mb-6 px-2">
        <h1
          className="
            text-2xl font-bold tracking-tight
            text-text-primary
            select-none
          "
        >
          <span className="text-accent">X</span> API
        </h1>
        <p className="text-xs text-text-secondary mt-0.5">推文数据浏览器</p>
      </div>

      <div className="mb-4">
        <UserSearch onSearch={onSearch} loading={loading} />
      </div>

      <section className="mb-5">
        <div className="px-2 mb-2">
          <p className="text-xs font-semibold text-text-primary">财经博主</p>
          <p className="text-[11px] text-text-secondary mt-0.5">点击快速加载用户帖子</p>
        </div>
        <div className="space-y-1">
          {financeBloggers.map((blogger) => {
            const active = activeUserId === blogger.userId;
            return (
              <button
                key={blogger.userId}
                type="button"
                onClick={() => onSearch(blogger.userId)}
                disabled={loading}
                className={`
                  w-full text-left rounded-lg px-3 py-2
                  border transition-all duration-200
                  disabled:cursor-not-allowed disabled:opacity-50
                  ${
                    active
                      ? 'bg-accent/10 border-accent/30'
                      : 'bg-tertiary/70 border-border hover:border-accent/30 hover:bg-tertiary'
                  }
                `}
              >
                <span className="block text-sm font-semibold text-text-primary truncate">
                  {blogger.name}
                </span>
                <span className="mt-0.5 flex items-center justify-between gap-2 text-xs text-text-secondary">
                  <span className="truncate">@{blogger.handle}</span>
                  <span className="shrink-0">{blogger.category}</span>
                </span>
              </button>
            );
          })}
        </div>
      </section>

      <nav className="flex-1 space-y-1">
        {navItems.map((item) => (
          <button
            key={item.label}
            disabled={!item.active}
            className={`
              w-full flex items-center gap-3 px-3 py-2.5
              rounded-lg text-sm font-medium
              transition-all duration-200
              ${
                item.active
                  ? 'bg-accent/10 text-accent'
                  : 'text-text-secondary hover:bg-tertiary hover:text-text-primary cursor-not-allowed opacity-50'
              }
            `}
          >
            <item.icon size={20} />
            {item.label}
          </button>
        ))}
      </nav>

      <div className="pt-4 border-t border-border">
        <div className="flex items-center justify-between px-2">
          <span className="text-sm text-text-secondary">
            {theme === 'dark' ? '暗黑模式' : '明亮模式'}
          </span>
          <ThemeToggle theme={theme} onToggle={onToggleTheme} />
        </div>
      </div>
    </aside>
  );
}
