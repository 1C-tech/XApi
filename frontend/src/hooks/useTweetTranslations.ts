import { useCallback, useRef, useState } from 'react';

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

interface TranslationRequestOptions {
  retryFailed?: boolean;
}

export function useTweetTranslations() {
  const [translations, setTranslations] = useState<Record<string, TranslationState>>({});
  const inFlightIds = useRef<Set<string>>(new Set());

  const requestTranslation = useCallback(async (
    tweetId: string,
    text: string,
    lang: string,
    options: TranslationRequestOptions = {}
  ) => {
    if (!text.trim()) return;

    const current = translations[tweetId];
    if (
      current?.loading
      || current?.text
      || (current?.error && !options.retryFailed)
      || inFlightIds.current.has(tweetId)
    ) {
      return;
    }

    inFlightIds.current.add(tweetId);
    setTranslations((prev) => ({
      ...prev,
      [tweetId]: { ...prev[tweetId], loading: true, visible: true, error: undefined },
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
          ...prev[tweetId],
          loading: false,
          visible: true,
          error: message,
        },
      }));
    } finally {
      inFlightIds.current.delete(tweetId);
    }
  }, [translations]);

  const toggleTranslation = useCallback(async (tweetId: string, text: string, lang: string) => {
    const current = translations[tweetId];
    if (current?.text) {
      setTranslations((prev) => ({
        ...prev,
        [tweetId]: { ...prev[tweetId], visible: !prev[tweetId]?.visible },
      }));
      return;
    }

    await requestTranslation(tweetId, text, lang, { retryFailed: true });
  }, [requestTranslation, translations]);

  return { translations, toggleTranslation, translateIfNeeded: requestTranslation };
}

export type { TranslationState };
