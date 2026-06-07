import { FormEvent, useState } from 'react';
import { Bot, SendHorizonal } from 'lucide-react';
import { useStockAgent } from '../../hooks/useStockAgent';
import { Button } from '../ui/Button';

interface StockAgentPanelProps {
  currentUserId: string;
}

export function StockAgentPanel({ currentUserId }: StockAgentPanelProps) {
  const [message, setMessage] = useState('分析 AAPL 和 000001.SZ');
  const { loading, error, result, ask } = useStockAgent();

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    const trimmed = message.trim();
    if (trimmed && !loading) {
      ask(trimmed, currentUserId || undefined);
    }
  };

  return (
    <section className="rounded-xl border border-border bg-secondary p-4 shadow-card-sm">
      <div className="flex items-start gap-2 mb-3">
        <Bot size={18} className="text-accent mt-0.5 shrink-0" />
        <div className="min-w-0">
          <h3 className="text-sm font-semibold text-text-primary">股票 Agent</h3>
          <p className="text-xs text-text-secondary leading-relaxed">
            查询纳斯达克、A股，并可结合当前 X 用户帖子
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-2">
        <input
          value={message}
          onChange={(event) => setMessage(event.target.value)}
          disabled={loading}
          placeholder="例如：分析 AAPL 和 600519.SH"
          className="
            w-full min-w-0 rounded-lg border border-border bg-tertiary
            px-3 py-2 text-sm text-text-primary placeholder:text-text-secondary/60
            outline-none transition-all focus:border-accent focus:ring-2 focus:ring-accent/20
            disabled:opacity-50
          "
        />
        <Button type="submit" size="sm" loading={loading} disabled={!message.trim()} className="w-full justify-center">
          <SendHorizonal size={14} />
          询问
        </Button>
      </form>

      {error && (
        <div className="mt-3 rounded-lg border border-red-200 bg-red-50 p-3 text-xs text-red-700 dark:border-red-900/50 dark:bg-red-950/20 dark:text-red-300">
          {error}
        </div>
      )}

      {result && (
        <div className="mt-4 space-y-3">
          <pre className="max-h-56 overflow-y-auto whitespace-pre-wrap break-words rounded-lg bg-tertiary p-3 text-xs leading-relaxed text-text-primary font-sans">
            {result.answer}
          </pre>

          {result.warnings.length > 0 && (
            <div className="rounded-lg border border-orange-200 bg-orange-50 p-3 text-xs text-orange-700">
              {result.warnings.join('；')}
            </div>
          )}

          {result.quotes.length > 0 && (
            <div className="grid gap-2">
              {result.quotes.map((quote) => {
                const change = quote.change_percent ?? quote.changePercent;
                return (
                  <div key={`${quote.market}-${quote.symbol}`} className="rounded-lg border border-border bg-tertiary p-3">
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-semibold text-sm text-text-primary">{quote.symbol}</span>
                      <span className="text-xs text-text-secondary">{quote.market}</span>
                    </div>
                    <p className="text-xs text-text-secondary mt-1 truncate">{quote.name || quote.source}</p>
                    <p className="text-lg font-bold text-text-primary mt-2">
                      {quote.price ?? '暂无价格'}
                    </p>
                    {change != null && (
                      <p className={`text-xs mt-1 ${change >= 0 ? 'text-green-700' : 'text-red-700'}`}>
                        {change >= 0 ? '+' : ''}{change}%
                      </p>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}
    </section>
  );
}
