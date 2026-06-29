import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import StudyCompletePage from './StudyCompletePage';
import { SessionSummary } from '../api/types';

function renderComplete(summary: SessionSummary | undefined) {
  return render(
    <MemoryRouter
      initialEntries={[{ pathname: '/study/deck-1/complete', state: summary ? { summary } : null }]}
    >
      <Routes>
        <Route path="/study/:deckId/complete" element={<StudyCompletePage />} />
        <Route path="/decks" element={<div data-testid="decks-page">Decks</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('StudyCompletePage', () => {
  it('renders the summary stats (accuracy as %, time as m:ss)', () => {
    renderComplete({ cardsReviewed: 8, correctAnswers: 6, accuracy: 0.75, totalTimeSpent: 75 });
    expect(screen.getByTestId('study-complete')).toBeInTheDocument();
    expect(screen.getByTestId('summary-reviewed')).toHaveTextContent('8');
    expect(screen.getByTestId('summary-correct')).toHaveTextContent('6');
    expect(screen.getByTestId('summary-accuracy')).toHaveTextContent('75%');
    expect(screen.getByTestId('summary-time')).toHaveTextContent('1:15');
  });

  it('redirects to the deck list when there is no summary', () => {
    renderComplete(undefined);
    expect(screen.getByTestId('decks-page')).toBeInTheDocument();
    expect(screen.queryByTestId('study-complete')).not.toBeInTheDocument();
  });
});
