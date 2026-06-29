import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CardFormModal from './CardFormModal';

describe('CardFormModal', () => {
  it('disables submit until both front and back are filled (VR-2)', async () => {
    render(<CardFormModal mode="add" onSubmit={vi.fn()} onClose={vi.fn()} />);
    const submit = screen.getByTestId('card-form-submit');
    expect(submit).toBeDisabled();

    await userEvent.type(screen.getByTestId('card-front-input'), 'apple');
    expect(submit).toBeDisabled(); // back still empty
    await userEvent.type(screen.getByTestId('card-back-input'), '사과');
    expect(submit).toBeEnabled();
  });

  it('submits all fields including pronunciation (FR-2.1)', async () => {
    const onSubmit = vi.fn();
    render(<CardFormModal mode="add" onSubmit={onSubmit} onClose={vi.fn()} />);
    await userEvent.type(screen.getByTestId('card-front-input'), 'apple');
    await userEvent.type(screen.getByTestId('card-back-input'), '사과');
    await userEvent.type(screen.getByTestId('card-pronunciation-input'), '/ap/');
    await userEvent.click(screen.getByTestId('card-form-submit'));

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ front: 'apple', back: '사과', pronunciation: '/ap/' }),
    );
  });
});
