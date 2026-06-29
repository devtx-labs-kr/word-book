import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiClient } from '../api/apiClient';
import { CardResponse, SessionSummary, StudyStartResponse } from '../api/types';

export type RatingValue = 0 | 1 | 2 | 3;

export interface StudySession {
  loading: boolean;
  error: string | null;
  cards: CardResponse[];
  currentIndex: number;
  isFlipped: boolean;
  card: CardResponse | null;
  total: number;
  current: number;
  remaining: number;
  isEmpty: boolean;
  flip: () => void;
  rate: (rating: RatingValue) => Promise<void>;
  exit: () => Promise<void>;
  dismissError: () => void;
}

/**
 * Orchestrates a study session (functional-design frontend-components §3): starts the session,
 * holds the server-fixed (shuffled) card order, tracks the current index / flip state / per-card
 * start time, wraps answer + complete, and navigates to the completion page. Errors are surfaced
 * (BR-ERR-4) via {@code error} (shown as a toast) — never swallowed.
 */
export function useStudySession(deckId: string | undefined): StudySession {
  const navigate = useNavigate();

  const [cards, setCards] = useState<CardResponse[]>([]);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Time the current card was first shown; used to compute timeSpentMs on rate.
  const cardStartTime = useRef<number>(Date.now());
  // Guards against StrictMode double-invoke (would otherwise start two sessions).
  const startedRef = useRef(false);
  // Guards against concurrent rate/exit calls while a request is in flight.
  const busyRef = useRef(false);

  useEffect(() => {
    if (!deckId || startedRef.current) return;
    startedRef.current = true;
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const result = await apiClient.post<StudyStartResponse>(`/api/decks/${deckId}/study`);
        if (cancelled) return;
        setCards(result.cards);
        setSessionId(result.sessionId);
        cardStartTime.current = Date.now();
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : String(e));
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [deckId]);

  const total = cards.length;
  const card = currentIndex < cards.length ? cards[currentIndex] : null;
  const current = total === 0 ? 0 : currentIndex + 1;
  const remaining = total - currentIndex;
  const isEmpty = !loading && total === 0;

  const flip = useCallback(() => {
    setIsFlipped(true);
  }, []);

  const finish = useCallback(async () => {
    if (!sessionId) return;
    const summary = await apiClient.post<SessionSummary>(`/api/study/${sessionId}/complete`);
    navigate(`/study/${deckId}/complete`, { replace: true, state: { summary } });
  }, [sessionId, deckId, navigate]);

  const rate = useCallback(
    async (rating: RatingValue) => {
      if (!sessionId || !card || busyRef.current) return;
      busyRef.current = true;
      setError(null);
      try {
        const timeSpentMs = Date.now() - cardStartTime.current;
        await apiClient.post(`/api/study/${sessionId}/answer`, {
          cardId: card.id,
          rating,
          timeSpentMs,
        });
        if (currentIndex + 1 < cards.length) {
          setCurrentIndex((i) => i + 1);
          setIsFlipped(false);
          cardStartTime.current = Date.now();
        } else {
          await finish();
        }
      } catch (e) {
        setError(e instanceof Error ? e.message : String(e));
      } finally {
        busyRef.current = false;
      }
    },
    [sessionId, card, currentIndex, cards.length, finish],
  );

  const exit = useCallback(async () => {
    if (busyRef.current) return;
    // No session or an empty session: just return to the deck list (no complete call).
    if (!sessionId || total === 0) {
      navigate('/decks');
      return;
    }
    busyRef.current = true;
    setError(null);
    try {
      await finish();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      busyRef.current = false;
    }
  }, [sessionId, total, finish, navigate]);

  const dismissError = useCallback(() => setError(null), []);

  return {
    loading,
    error,
    cards,
    currentIndex,
    isFlipped,
    card,
    total,
    current,
    remaining,
    isEmpty,
    flip,
    rate,
    exit,
    dismissError,
  };
}
