import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import DeckFormModal from './DeckFormModal';

describe('DeckFormModal', () => {
  it('disables submit until a name is entered (VR-1)', async () => {
    render(<DeckFormModal mode="create" onSubmit={vi.fn()} onClose={vi.fn()} />);
    const submit = screen.getByTestId('deck-form-submit');
    expect(submit).toBeDisabled();
    expect(screen.getByTestId('deck-name-error')).toBeInTheDocument();

    await userEvent.type(screen.getByTestId('deck-name-input'), 'Spanish');
    expect(submit).toBeEnabled();
  });

  it('submits the name, description and selected color', async () => {
    const onSubmit = vi.fn();
    render(<DeckFormModal mode="create" onSubmit={onSubmit} onClose={vi.fn()} />);
    await userEvent.type(screen.getByTestId('deck-name-input'), 'French');
    await userEvent.type(screen.getByTestId('deck-description-input'), 'lang');
    await userEvent.click(screen.getByTestId('deck-color-#34C759'));
    await userEvent.click(screen.getByTestId('deck-form-submit'));

    expect(onSubmit).toHaveBeenCalledWith({
      name: 'French',
      descriptionText: 'lang',
      color: '#34C759',
    });
  });

  it('prefills fields in edit mode', () => {
    render(
      <DeckFormModal
        mode="edit"
        initial={{
          id: 'd1',
          name: 'Existing',
          descriptionText: 'desc',
          color: '#FF3B30',
          createdAt: '',
          updatedAt: '',
          totalCards: 0,
        }}
        onSubmit={vi.fn()}
        onClose={vi.fn()}
      />,
    );
    expect(screen.getByTestId('deck-name-input')).toHaveValue('Existing');
    expect(screen.getByTestId('deck-form-submit')).toHaveTextContent('Save');
  });
});
