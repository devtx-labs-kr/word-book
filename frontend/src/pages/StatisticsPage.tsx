import { useEffect, useState } from 'react';
import { apiClient } from '../api/apiClient';
import { StatisticsResponse } from '../api/types';
import StatCard from '../components/StatCard';
import SessionRow from '../components/SessionRow';

/** Renders an accuracy ratio (0.0–1.0) as a whole-percent string (original {@code Int(acc*100)}). */
function formatPercent(accuracy: number): string {
  return `${Math.round(accuracy * 100)}%`;
}

/** Formats seconds as whole minutes, matching the original {@code formatTime} (e.g. 150s → "2m"). */
function formatStudyTime(seconds: number): string {
  return `${Math.round(seconds / 60)}m`;
}

/**
 * Statistics dashboard (/stats, FR-6). Fetches the assembled {@code /api/statistics} response on
 * mount and renders Today / Overall / Upcoming Reviews / Recent Sessions. Empty data renders as
 * zeros and a "No study sessions yet" message (BR-ST7, not an error); a fetch failure surfaces via
 * an error toast (apiClient throws — never swallowed). Heading hierarchy is h1 → section h2s for
 * accessibility (NFR-8).
 */
export default function StatisticsPage() {
  const [stats, setStats] = useState<StatisticsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    apiClient
      .get<StatisticsResponse>('/api/statistics')
      .then((result) => {
        if (active) setStats(result);
      })
      .catch((e: unknown) => {
        if (active) setError(e instanceof Error ? e.message : String(e));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!error) return;
    const timer = setTimeout(() => setError(null), 3000);
    return () => clearTimeout(timer);
  }, [error]);

  if (loading && !stats) {
    return (
      <main className="stats-page" aria-label="Statistics">
        <h1>Statistics</h1>
        <p data-testid="stats-loading">Loading…</p>
        {error && (
          <p className="error" role="alert" data-testid="stats-error-toast">
            {error}
          </p>
        )}
      </main>
    );
  }

  if (!stats) {
    return (
      <main className="stats-page" aria-label="Statistics">
        <h1>Statistics</h1>
        {error && (
          <p className="error" role="alert" data-testid="stats-error-toast">
            {error}
          </p>
        )}
      </main>
    );
  }

  const { today, overall, upcoming, recentSessions } = stats;

  return (
    <main className="stats-page" aria-label="Statistics">
      <h1>Statistics</h1>

      <section aria-labelledby="today-heading" className="stats-section">
        <h2 id="today-heading">Today</h2>
        <div className="stat-grid" data-testid="today-grid">
          <StatCard
            label="Cards Reviewed"
            value={String(today.cardsReviewed)}
            color="var(--accent)"
            icon="🃏"
            testId="today-cards-reviewed"
          />
          <StatCard
            label="Accuracy"
            value={formatPercent(today.accuracy)}
            color="var(--success)"
            icon="🎯"
            testId="today-accuracy"
          />
          <StatCard
            label="Study Time"
            value={formatStudyTime(today.studyTimeSeconds)}
            color="var(--warning)"
            icon="⏱️"
            testId="today-study-time"
          />
          <StatCard
            label="Sessions"
            value={String(today.sessionCount)}
            color="var(--danger)"
            icon="📚"
            testId="today-sessions"
          />
        </div>
      </section>

      <section aria-labelledby="overall-heading" className="stats-section">
        <h2 id="overall-heading">Overall</h2>
        <div className="stat-grid" data-testid="overall-grid">
          <StatCard
            label="Total Cards"
            value={String(overall.totalCards)}
            color="var(--purple)"
            icon="🗂️"
            testId="overall-total"
          />
          <StatCard
            label="New"
            value={String(overall.newCards)}
            color="var(--accent)"
            testId="overall-new"
          />
          <StatCard
            label="Learning"
            value={String(overall.learningCards)}
            color="var(--warning)"
            testId="overall-learning"
          />
          <StatCard
            label="Mastered"
            value={String(overall.masteredCards)}
            color="var(--success)"
            testId="overall-mastered"
          />
        </div>
      </section>

      <section aria-labelledby="upcoming-heading" className="stats-section">
        <h2 id="upcoming-heading">Upcoming Reviews</h2>
        <p className="stats-upcoming muted" data-testid="upcoming-line">
          Today {upcoming.dueToday} · Tomorrow {upcoming.dueTomorrow} · This week{' '}
          {upcoming.dueThisWeek}
        </p>
      </section>

      <section aria-labelledby="recent-heading" className="stats-section">
        <h2 id="recent-heading">Recent Sessions</h2>
        {recentSessions.length === 0 ? (
          <p className="muted" data-testid="recent-empty">
            No study sessions yet
          </p>
        ) : (
          <ul className="session-list" role="list" data-testid="recent-list">
            {recentSessions.map((session) => (
              <SessionRow key={session.sessionId} session={session} />
            ))}
          </ul>
        )}
      </section>

      {error && (
        <p className="error" role="alert" data-testid="stats-error-toast">
          {error}
        </p>
      )}
    </main>
  );
}
