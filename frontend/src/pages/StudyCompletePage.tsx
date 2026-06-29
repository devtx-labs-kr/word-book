import { useEffect, useRef } from 'react';
import { Navigate, useLocation, useNavigate, useParams } from 'react-router-dom';
import { SessionSummary } from '../api/types';

/** Formats seconds as "m:ss" (e.g. 75 -> "1:15"). */
function formatDuration(totalSeconds: number): string {
  const seconds = Math.round(totalSeconds);
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return `${minutes}:${rest.toString().padStart(2, '0')}`;
}

/**
 * Study completion summary (US-C3 / FR-4.5). Reads the {@link SessionSummary} from the router
 * navigation state (set by useStudySession.finish). With no summary — e.g. a direct visit — it
 * redirects back to the deck list.
 */
export default function StudyCompletePage() {
  const { deckId } = useParams<{ deckId: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const headingRef = useRef<HTMLHeadingElement>(null);

  const summary = (location.state as { summary?: SessionSummary } | null)?.summary;

  useEffect(() => {
    headingRef.current?.focus();
  }, []);

  if (!summary) {
    return <Navigate to="/decks" replace />;
  }

  const accuracyPercent = Math.round(summary.accuracy * 100);

  return (
    <main className="study-complete" data-testid="study-complete" aria-label="Study complete">
      <h1 ref={headingRef} tabIndex={-1}>
        🎉 Study complete
      </h1>
      <dl className="summary-stats" data-testid="summary-stats">
        <div>
          <dt>Cards reviewed</dt>
          <dd data-testid="summary-reviewed">{summary.cardsReviewed}</dd>
        </div>
        <div>
          <dt>Correct</dt>
          <dd data-testid="summary-correct">{summary.correctAnswers}</dd>
        </div>
        <div>
          <dt>Accuracy</dt>
          <dd data-testid="summary-accuracy">{accuracyPercent}%</dd>
        </div>
        <div>
          <dt>Time</dt>
          <dd data-testid="summary-time">{formatDuration(summary.totalTimeSpent)}</dd>
        </div>
      </dl>
      <div className="study-complete-actions">
        <button type="button" data-testid="complete-back-decks" onClick={() => navigate('/decks')}>
          Back to decks
        </button>
        <button
          type="button"
          className="button-secondary"
          data-testid="complete-view-stats"
          onClick={() => navigate('/stats')}
        >
          View stats
        </button>
        {deckId && (
          <button
            type="button"
            className="button-secondary"
            data-testid="complete-study-again"
            onClick={() => navigate(`/study/${deckId}`)}
          >
            Study again
          </button>
        )}
      </div>
    </main>
  );
}
