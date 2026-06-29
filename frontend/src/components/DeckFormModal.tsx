import { FormEvent, useRef, useState } from 'react';
import { DeckRequest, DeckResponse } from '../api/types';
import { useFocusTrap } from '../hooks/useFocusTrap';

const COLOR_PALETTE = [
  '#007AFF',
  '#34C759',
  '#FF9500',
  '#FF3B30',
  '#AF52DE',
  '#5856D6',
  '#FF2D55',
  '#8E8E93',
];

interface DeckFormModalProps {
  mode: 'create' | 'edit';
  initial?: DeckResponse;
  onSubmit: (payload: DeckRequest) => void;
  onClose: () => void;
}

/** Create/edit deck modal ({@code role="dialog"}, focus trap, Esc to close). */
export default function DeckFormModal({ mode, initial, onSubmit, onClose }: DeckFormModalProps) {
  const [name, setName] = useState(initial?.name ?? '');
  const [description, setDescription] = useState(initial?.descriptionText ?? '');
  const [color, setColor] = useState(initial?.color ?? COLOR_PALETTE[0]);

  const dialogRef = useRef<HTMLDivElement>(null);
  const nameRef = useRef<HTMLInputElement>(null);
  useFocusTrap(dialogRef, true, onClose, nameRef);

  const nameValid = name.trim() !== '';

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!nameValid) return;
    onSubmit({ name: name.trim(), descriptionText: description, color });
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="deck-form-title"
        data-testid="deck-form-modal"
        ref={dialogRef}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="deck-form-title">{mode === 'create' ? 'New Deck' : 'Edit Deck'}</h2>
        <form onSubmit={handleSubmit}>
          <label htmlFor="deck-name">Name</label>
          <input
            id="deck-name"
            data-testid="deck-name-input"
            value={name}
            ref={nameRef}
            onChange={(e) => setName(e.target.value)}
            aria-invalid={!nameValid}
            required
          />
          {!nameValid && (
            <p className="field-error" data-testid="deck-name-error">
              Name is required.
            </p>
          )}

          <label htmlFor="deck-description">Description</label>
          <input
            id="deck-description"
            data-testid="deck-description-input"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />

          <fieldset className="color-palette">
            <legend>Color</legend>
            {COLOR_PALETTE.map((c) => (
              <button
                type="button"
                key={c}
                className={`color-swatch${c === color ? ' selected' : ''}`}
                style={{ background: c }}
                aria-label={`Color ${c}`}
                aria-pressed={c === color}
                data-testid={`deck-color-${c}`}
                onClick={() => setColor(c)}
              />
            ))}
          </fieldset>

          <div className="modal-actions">
            <button
              type="button"
              className="button-secondary"
              data-testid="deck-form-cancel"
              onClick={onClose}
            >
              Cancel
            </button>
            <button type="submit" data-testid="deck-form-submit" disabled={!nameValid}>
              {mode === 'create' ? 'Create' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
