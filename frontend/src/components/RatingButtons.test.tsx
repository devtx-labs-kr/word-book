import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import RatingButtons from './RatingButtons';

describe('RatingButtons', () => {
  it('maps each button to its integer wire rating (0-3)', async () => {
    const onRate = vi.fn();
    render(<RatingButtons disabled={false} onRate={onRate} />);

    await userEvent.click(screen.getByTestId('rating-again'));
    await userEvent.click(screen.getByTestId('rating-hard'));
    await userEvent.click(screen.getByTestId('rating-good'));
    await userEvent.click(screen.getByTestId('rating-easy'));

    expect(onRate.mock.calls.map((c) => c[0])).toEqual([0, 1, 2, 3]);
  });

  it('disables all buttons when disabled', () => {
    render(<RatingButtons disabled onRate={vi.fn()} />);
    expect(screen.getByTestId('rating-again')).toBeDisabled();
    expect(screen.getByTestId('rating-hard')).toBeDisabled();
    expect(screen.getByTestId('rating-good')).toBeDisabled();
    expect(screen.getByTestId('rating-easy')).toBeDisabled();
  });

  it('shows the 1–4 shortcut hints', () => {
    render(<RatingButtons disabled={false} onRate={vi.fn()} />);
    expect(screen.getByTestId('rating-again')).toHaveTextContent('1');
    expect(screen.getByTestId('rating-easy')).toHaveTextContent('4');
  });
});
