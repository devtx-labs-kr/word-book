import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Flashcard from './Flashcard';
import { naverDictUrl } from '../lib/naverDict';
import { CardResponse } from '../api/types';

const card: CardResponse = {
  id: 'c1',
  front: 'apple pie',
  back: '사과 파이',
  example: 'I ate apple pie',
  pronunciation: '/ˈæpəl paɪ/',
  notes: 'dessert',
  learningState: 'new',
  easeFactor: 2.5,
  interval: 3,
  repetitions: 1,
  nextReviewDate: '2026-01-01T00:00:00Z',
  lastReviewedAt: null,
  totalReviews: 4,
  correctReviews: 3,
};

describe('Flashcard', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('builds an encoded Naver dictionary URL', () => {
    expect(naverDictUrl('apple pie')).toBe(
      'https://dict.naver.com/enkodict/#/search?query=apple%20pie',
    );
  });

  it('shows the front word and pronunciation when enabled', () => {
    render(<Flashcard card={card} isFlipped={false} showPronunciation onFlip={vi.fn()} />);
    expect(screen.getByTestId('flashcard-front')).toBeInTheDocument();
    expect(screen.getByText('apple pie')).toBeInTheDocument();
    expect(screen.getByTestId('flashcard-pronunciation')).toBeInTheDocument();
    expect(screen.queryByTestId('flashcard-back')).not.toBeInTheDocument();
  });

  it('hides pronunciation when disabled', () => {
    render(<Flashcard card={card} isFlipped={false} showPronunciation={false} onFlip={vi.fn()} />);
    expect(screen.queryByTestId('flashcard-pronunciation')).not.toBeInTheDocument();
  });

  it('opens the dictionary in a new tab with noopener,noreferrer', async () => {
    const openSpy = vi.spyOn(window, 'open').mockReturnValue(null);
    render(<Flashcard card={card} isFlipped showPronunciation onFlip={vi.fn()} />);

    await userEvent.click(screen.getByTestId('dict-link'));

    expect(openSpy).toHaveBeenCalledWith(
      'https://dict.naver.com/enkodict/#/search?query=apple%20pie',
      '_blank',
      'noopener,noreferrer',
    );
  });

  it('renders the back with definition, example, and card stats', () => {
    render(<Flashcard card={card} isFlipped showPronunciation onFlip={vi.fn()} />);
    expect(screen.getByTestId('flashcard-back')).toBeInTheDocument();
    expect(screen.getByText('사과 파이')).toBeInTheDocument();
    expect(screen.getByText('I ate apple pie')).toBeInTheDocument();
    // 3/4 correct -> 75% accuracy.
    expect(screen.getByTestId('flashcard-stats')).toHaveTextContent('75%');
  });
});
