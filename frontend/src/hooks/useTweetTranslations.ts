import { useCallback, useState } from 'react';

interface TranslationState {
  text?: string;
  loading?: boolean;
  error?: string;
  visible?: boolean;
}

interface TranslateResponse {
  translatedText: string;
  sourceLang: string;
  targetLang: string;
  provider: string;
}

export function useTweetTranslations() {
  const [translations, setTranslations] = useState<Record<string, TranslationState>>({});

  const toggleTranslation = useCallback(async (tweetId: string, text: string, lang: string) => {
    const current = translations[tweetId];
    if (current?.text) {
      setTranslations((prev) => ({
        ...prev,
        [tweetId]: { ...prev[tweetId], visible: !prev[tweetId]?.visible },
      }));
      return;
    }

    setTranslations((prev) => ({
      ...prev,
      [tweetId]: { loading: true, visible: true },
    }));

    try {
      const response = await fetch('/api/x/translate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          text,
          sourceLang: lang || 'auto',
          targetLang: 'zh-CN',
        }),
      });

      if (!response.ok) {
        const errorBody = await response.json().catch(() => null);
        throw new Error(errorBody?.message || `翻译失败 (${response.status})`);
      }

      const body: TranslateResponse = await response.json();
      setTranslations((prev) => ({
        ...prev,
        [tweetId]: {
          text: body.translatedText,
          loading: false,
          visible: true,
        },
      }));
    } catch (error) {
      const message = error instanceof Error ? error.message : '翻译失败';
      setTranslations((prev) => ({
        ...prev,
        [tweetId]: {
          loading: false,
          visible: true,
          error: message,
        },
      }));
    }
  }, [translations]);

  return { translations, toggleTranslation };
}

export type { TranslationState };
