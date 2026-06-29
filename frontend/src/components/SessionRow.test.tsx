import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import SessionRow, { relativeTime } from './SessionRow';
import { SessionView } from '../api/types';

function session(overrides: Partial<SessionView> = {}): SessionView {
  return {
    sessionId: 's1',
    deckName: 'English',
    cardsReviewed: 12,
    accuracy: 0.9,
    startedAt: new Date(Date.now() - 2 * 60 * 1000).toISOString(),
    ...overrides,
  };
}

describe('SessionRow', () => {
  it('shows deck name and meta (cards, rounded percent, relative time)', () => {
    render(
      <ul>
        <SessionRow session={session({ deckName: 'English', cardsReviewed: 12, accuracy: 0.83 })} />
      </ul>,
    );
    expect(screen.getByTestId('session-deck')).toHaveTextContent('English');
    const meta = screen.getByTestId('session-meta');
    expect(meta).toHaveTextContent('12 cards');
    // 0.83 → 83% (Math.round).
    expect(meta).toHaveTextContent('83%');
  });

  it('uses the success color dot at/above 0.8 accuracy', () => {
    render(
      <ul>
        <SessionRow session={session({ accuracy: 0.8 })} />
      </ul>,
    );
    const dot = screen.getByTestId('session-row').querySelector('.session-dot') as HTMLElement;
    expect(dot).toHaveStyle({ background: 'var(--success)' });
  });

  it('uses the warning color dot below 0.8 accuracy', () => {
    render(
      <ul>
        <SessionRow session={session({ accuracy: 0.5 })} />
      </ul>,
    );
    const dot = screen.getByTestId('session-row').querySelector('.session-dot') as HTMLElement;
    expect(dot).toHaveStyle({ background: 'var(--warning)' });
  });
});

describe('relativeTime', () => {
  const now = Date.parse('2026-06-29T12:00:00Z');

  it('reports "just now" under a minute', () => {
    expect(relativeTime('2026-06-29T11:59:30Z', now)).toBe('just now');
  });

  it('pluralizes minutes and hours', () => {
    expect(relativeTime('2026-06-29T11:59:00Z', now)).toBe('1 minute ago');
    expect(relativeTime('2026-06-29T11:00:00Z', now)).toBe('1 hour ago');
    expect(relativeTime('2026-06-29T09:00:00Z', now)).toBe('3 hours ago');
  });
});
