export interface AgentAskRequest {
  message: string;
  userId?: string | null;
  symbols?: string[];
}

export interface AgentStockQuote {
  market: string;
  symbol: string;
  name?: string | null;
  price?: number | null;
  change_percent?: number | null;
  changePercent?: number | null;
  volume?: number | null;
  updated_at?: string | null;
  updatedAt?: string | null;
  source: string;
}

export interface AgentPost {
  id: string;
  author_name?: string | null;
  authorName?: string | null;
  author_screen_name?: string | null;
  authorScreenName?: string | null;
  text: string;
  created_at?: string | null;
  createdAt?: string | null;
}

export interface AgentAskResponse {
  answer: string;
  quotes: AgentStockQuote[];
  posts: AgentPost[];
  used_tradingagents?: boolean;
  usedTradingAgents?: boolean;
  warnings: string[];
}
