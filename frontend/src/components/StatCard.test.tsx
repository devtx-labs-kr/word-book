import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import StatCard from './StatCard';

describe('StatCard', () => {
  it('renders the value, label, and icon with the given color', () => {
    render(
      <StatCard label="Accuracy" value="85%" color="var(--success)" icon="🎯" testId="acc-card" />,
    );
    const card = screen.getByTestId('acc-card');
    expect(card).toHaveTextContent('85%');
    expect(card).toHaveTextContent('Accuracy');
    const value = card.querySelector('.stat-card-value') as HTMLElement;
    expect(value).toHaveStyle({ color: 'var(--success)' });
  });

  it('renders without an icon', () => {
    render(<StatCard label="New" value="3" color="var(--accent)" testId="new-card" />);
    const card = screen.getByTestId('new-card');
    expect(card).toHaveTextContent('3');
    expect(card.querySelector('.stat-card-icon')).toBeNull();
  });
});
