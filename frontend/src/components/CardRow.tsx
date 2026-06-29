import { CardResponse } from '../api/types';

interface CardRowProps {
  card: CardResponse;
  onEdit: (card: CardResponse) => void;
  onDelete: (card: CardResponse) => void;
}

const STATE_LABEL: Record<string, string> = {
  new: 'New',
  learning: 'Learning',
  mastered: 'Mastered',
  relearning: 'Relearning',
};

/** A single card row: status dot + front (bold) + back (muted) + state pill + edit/delete. */
export default function CardRow({ card, onEdit, onDelete }: CardRowProps) {
  return (
    <li className="card-row" data-testid="card-row">
      <span className={`card-state-dot state-${card.learningState}`} aria-hidden="true" />
      <div className="card-row-text">
        <span className="card-front">{card.front}</span>
        <span className="card-back">{card.back}</span>
        {card.pronunciation && <span className="card-pronunciation">{card.pronunciation}</span>}
      </div>
      <span className={`pill state-${card.learningState}`} data-testid="card-state">
        {STATE_LABEL[card.learningState] ?? card.learningState}
      </span>
      <div className="card-row-actions">
        <button
          type="button"
          className="button-icon"
          data-testid="card-edit"
          aria-label={`Edit ${card.front}`}
          onClick={() => onEdit(card)}
        >
          ✎
        </button>
        <button
          type="button"
          className="button-icon"
          data-testid="card-delete"
          aria-label={`Delete ${card.front}`}
          onClick={() => onDelete(card)}
        >
          🗑
        </button>
      </div>
    </li>
  );
}
