export interface FinanceBlogger {
  name: string;
  handle: string;
  userId: string;
  category: string;
}

export const financeBloggers: FinanceBlogger[] = [
  {
    name: 'Serenity',
    handle: 'aleabitoreddit',
    userId: '1940360837547565056',
    category: 'AI / Markets',
  },
  {
    name: 'ZeroHedge',
    handle: 'zerohedge',
    userId: '18856867',
    category: 'Macro',
  },
  {
    name: 'Unusual Whales',
    handle: 'unusual_whales',
    userId: '1200616796295847936',
    category: 'Market Data',
  },
  {
    name: 'Justin Sun',
    handle: 'justinsuntron',
    userId: '902839045356744704',
    category: 'Crypto',
  },
];
