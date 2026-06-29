import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import ProgressIndicator from './ProgressIndicator';

describe('ProgressIndicator', () => {
  it('exposes an accessible progressbar with current/total bounds', () => {
    render(<ProgressIndicator current={2} total={5} remaining={4} />);
    const bar = screen.getByTestId('progress-indicator');
    expect(bar).toHaveAttribute('role', 'progressbar');
    expect(bar).toHaveAttribute('aria-valuenow', '2');
    expect(bar).toHaveAttribute('aria-valuemin', '0');
    expect(bar).toHaveAttribute('aria-valuemax', '5');
  });

  it('renders the "current / total" text and remaining count', () => {
    render(<ProgressIndicator current={2} total={5} remaining={4} />);
    expect(screen.getByTestId('progress-text')).toHaveTextContent('2 / 5');
    expect(screen.getByText('4 left')).toBeInTheDocument();
  });
});
