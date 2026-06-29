import { SessionView } from '../api/types';

/** Accuracy at/above this ratio shows the success color dot; below shows warning (original app). */
const GOOD_ACCURACY = 0.8;

/** A coarse "n unit(s) ago" relative time from an ISO-8601 instant (display-only). */
export function relativeTime(iso: string, now: number = Date.now()): string {
  const then = new Date(iso).getTime();
  const seconds = Math.max(0, Math.floor((now - then) / 1000));
  if (seconds < 60) return 'just now';
  const units: [number, string][] = [
    [60, 'minute'],
    [60, 'hour'],
    [24, 'day'],
    [7, 'week'],
  ];
  let value = seconds;
  let unit = 'second';
  for (const [size, name] of units) {
    if (value < size) break;
    value = Math.floor(value / size);
    unit = name;
  }
  return `${value} ${unit}${value === 1 ? '' : 's'} ago`;
}

/**
 * One recent-session row: a colored accuracy dot, the deck name, then "{n} cards · {pct}% ·
 * {relative}" (frontend-components §3/§5). The dot is paired with the percent text, so meaning
 * never depends on color alone (NFR-8). References tokens only → dark-theme adaptive.
 */
export default function SessionRow({ session }: { session: SessionView }) {
  const pct = Math.round(session.accuracy * 100);
  const dotColor = session.accuracy >= GOOD_ACCURACY ? 'var(--success)' : 'var(--warning)';

  return (
    <li className="session-row" data-testid="session-row">
      <span className="session-dot" style={{ background: dotColor }} aria-hidden="true" />
      <span className="session-deck" data-testid="session-deck">
        {session.deckName}
      </span>
      <span className="session-meta" data-testid="session-meta">
        {session.cardsReviewed} cards · {pct}% · {relativeTime(session.startedAt)}
      </span>
    </li>
  );
}
