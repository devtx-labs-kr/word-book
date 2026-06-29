import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { apiClient } from '../api/apiClient';
import { DeckResponse } from '../api/types';
import { useSettings } from '../context/SettingsContext';
import { useStudySession } from '../hooks/useStudySession';
import Flashcard from '../components/Flashcard';
import RatingButtons from '../components/RatingButtons';
import ProgressIndicator from '../components/ProgressIndicator';
import ConfirmDialog from '../components/ConfirmDialog';
import EmptyState from '../components/EmptyState';

/** Tags that should swallow study shortcuts so typing in a field isn't hijacked. */
function isEditableTarget(target: EventTarget | null): boolean {
  const el = target as HTMLElement | null;
  if (!el) return false;
  const tag = el.tagName;
  return tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || el.isContentEditable;
}

/**
 * Full-screen study screen (US-C1·C3 / FR-4). Renders the flashcard, rating buttons, and progress.
 * Keyboard: Space reveals the answer (front → back); 1–4 rate the card but only once revealed.
 * Mid-session exit is confirmed via the shared ConfirmDialog and reuses the same complete path.
 */
export default function StudyPage() {
  const { deckId } = useParams<{ deckId: string }>();
  const navigate = useNavigate();
  const session = useStudySession(deckId);
  const { card, isFlipped, flip, rate, exit, loading, error, isEmpty } = session;
  const { settings } = useSettings();

  const [deckName, setDeckName] = useState<string>('');
  const [confirmExit, setConfirmExit] = useState(false);

  useEffect(() => {
    if (!deckId) return;
    let cancelled = false;
    apiClient
      .get<DeckResponse>(`/api/decks/${deckId}`)
      .then((d) => {
        if (!cancelled) setDeckName(d.name);
      })
      .catch(() => {
        /* heading falls back to "Study"; the session load surfaces real errors */
      });
    return () => {
      cancelled = true;
    };
  }, [deckId]);

  // Keyboard shortcuts (FR-5.2 / NFR-8): Space reveals, 1–4 rate when revealed. Registered on
  // mount, removed on unmount; ignored while a form field is focused or a dialog is open.
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (isEditableTarget(e.target) || confirmExit) return;
      if (e.key === ' ' || e.code === 'Space') {
        if (!isFlipped && card) {
          e.preventDefault();
          flip();
        }
        return;
      }
      if (!isFlipped) return;
      if (e.key === '1') {
        e.preventDefault();
        void rate(0);
      } else if (e.key === '2') {
        e.preventDefault();
        void rate(1);
      } else if (e.key === '3') {
        e.preventDefault();
        void rate(2);
      } else if (e.key === '4') {
        e.preventDefault();
        void rate(3);
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [isFlipped, card, flip, rate, confirmExit]);

  return (
    <main className="study-page" data-testid="study-page" aria-label="Study session">
      <header className="study-header">
        <h1 className="study-deck-name">{deckName || 'Study'}</h1>
        {!loading && !isEmpty && (
          <ProgressIndicator
            current={session.current}
            total={session.total}
            remaining={session.remaining}
          />
        )}
        <button
          type="button"
          className="button-link"
          data-testid="exit-study"
          onClick={() => setConfirmExit(true)}
        >
          ✕ Exit
        </button>
      </header>

      {error && (
        <p className="error" role="alert" data-testid="error-message">
          {error}
        </p>
      )}

      {loading ? (
        <p data-testid="loading">Loading…</p>
      ) : isEmpty ? (
        <EmptyState
          icon="🎉"
          title="No cards to study"
          description="Nothing is due in this deck right now."
          action={
            <button data-testid="study-empty" onClick={() => navigate('/decks')}>
              Back to decks
            </button>
          }
        />
      ) : card ? (
        <div className="study-body">
          <Flashcard
            card={card}
            isFlipped={isFlipped}
            showPronunciation={settings?.showPronunciation ?? true}
            onFlip={flip}
          />
          <RatingButtons disabled={!isFlipped} onRate={(r) => void rate(r)} />
        </div>
      ) : null}

      {confirmExit && (
        <ConfirmDialog
          title="End this session?"
          message="Your progress so far is saved. You can review the summary on the next screen."
          confirmLabel="End session"
          cancelLabel="Keep studying"
          onConfirm={() => {
            setConfirmExit(false);
            void exit();
          }}
          onCancel={() => setConfirmExit(false)}
        />
      )}
    </main>
  );
}
