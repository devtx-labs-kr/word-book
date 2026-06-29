import { CardResponse } from '../api/types';
import { naverDictUrl } from '../lib/naverDict';

interface FlashcardProps {
  card: CardResponse;
  isFlipped: boolean;
  showPronunciation: boolean;
  onFlip: () => void;
}

/**
 * The study flashcard (FR-5). Front shows the word (+ pronunciation when enabled); the back adds
 * the definition, example, notes, card stats, and a Naver dictionary link. Flipping is
 * client-side (no server round trip). The flip state is announced via {@code aria-live}.
 */
export default function Flashcard({ card, isFlipped, showPronunciation, onFlip }: FlashcardProps) {
  const openDict = () => {
    // noopener prevents window.opener reverse-reference (tabnabbing); noreferrer hides the
    // referrer (BR-DICT-2 / SD-U3-4).
    window.open(naverDictUrl(card.front), '_blank', 'noopener,noreferrer');
  };

  return (
    <div className="flashcard" data-testid="flashcard">
      <div className="flashcard-inner" aria-live="polite">
        {!isFlipped ? (
          <div className="flashcard-face flashcard-front" data-testid="flashcard-front">
            <p className="flashcard-word">{card.front}</p>
            {showPronunciation && card.pronunciation && (
              <p className="flashcard-pronunciation" data-testid="flashcard-pronunciation">
                {card.pronunciation}
              </p>
            )}
            <button
              type="button"
              className="flashcard-reveal"
              data-testid="flashcard-reveal"
              onClick={onFlip}
            >
              Show answer <span aria-hidden="true">(Space)</span>
            </button>
          </div>
        ) : (
          <div className="flashcard-face flashcard-back" data-testid="flashcard-back">
            <div className="flashcard-back-head">
              <p className="flashcard-word">{card.front}</p>
              {card.pronunciation && (
                <span className="flashcard-pronunciation">{card.pronunciation}</span>
              )}
            </div>
            <button
              type="button"
              className="flashcard-dict"
              data-testid="dict-link"
              onClick={openDict}
              aria-label={`Look up "${card.front}" in the Naver dictionary (opens in a new tab)`}
            >
              🔗 Search in Naver Dictionary ↗
            </button>
            <hr className="flashcard-divider" />
            <p className="flashcard-definition">{card.back}</p>
            {card.example && <p className="flashcard-example">{card.example}</p>}
            {card.notes && <p className="flashcard-notes">{card.notes}</p>}
            <dl className="flashcard-stats" data-testid="flashcard-stats">
              <div>
                <dt>Interval</dt>
                <dd>{card.interval}d</dd>
              </div>
              <div>
                <dt>Reviews</dt>
                <dd>{card.totalReviews}</dd>
              </div>
              <div>
                <dt>Accuracy</dt>
                <dd>
                  {card.totalReviews === 0
                    ? '—'
                    : `${Math.round((card.correctReviews / card.totalReviews) * 100)}%`}
                </dd>
              </div>
            </dl>
          </div>
        )}
      </div>
    </div>
  );
}
