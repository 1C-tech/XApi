import type { ReactNode } from 'react';

interface AppLayoutProps {
  sidebar: ReactNode;
  main: ReactNode;
  panel: ReactNode;
}

export function AppLayout({ sidebar, main, panel }: AppLayoutProps) {
  return (
    <div className="min-h-screen bg-primary">
      <div className="mx-auto flex justify-center">
        {/* 左侧栏 */}
        {sidebar}

        {/* 主内容区 */}
        <main className="flex-1 max-w-[600px] w-full min-h-screen border-r border-border px-4 py-6">
          {main}
        </main>

        {/* 右侧面板 */}
        {panel}
      </div>
    </div>
  );
}
