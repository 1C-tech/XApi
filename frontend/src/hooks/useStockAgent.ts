import { useCallback, useState } from 'react';
import type { AgentAskResponse } from '../types/agent';

interface StockAgentState {
  loading: boolean;
  error: string | null;
  result: AgentAskResponse | null;
}

export function useStockAgent() {
  const [state, setState] = useState<StockAgentState>({
    loading: false,
    error: null,
    result: null,
  });

  const ask = useCallback(async (message: string, userId?: string) => {
    setState({ loading: true, error: null, result: null });
    try {
      const response = await fetch('/api/agent/ask', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message,
          userId: userId || null,
          symbols: [],
        }),
      });

      if (!response.ok) {
        const errorBody = await response.json().catch(() => null);
        throw new Error(errorBody?.message || `Agent 请求失败 (${response.status})`);
      }

      const result: AgentAskResponse = await response.json();
      setState({ loading: false, error: null, result });
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Agent 请求失败';
      setState({ loading: false, error: message, result: null });
    }
  }, []);

  return { ...state, ask };
}
