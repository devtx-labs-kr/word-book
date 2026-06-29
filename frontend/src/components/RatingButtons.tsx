import { Rating } from '../api/types';
import { RatingValue } from '../hooks/useStudySession';

interface RatingButtonsProps {
  disabled: boolean;
  onRate: (rating: RatingValue) => void;
}

interface RatingOption {
  rating: RatingValue;
  label: string;
  key: string;
  testId: string;
  className: string;
}

const OPTIONS: RatingOption[] = [
  { rating: Rating.Again, label: 'Again', key: '1', testId: 'rating-again', className: 'again' },
  { rating: Rating.Hard, label: 'Hard', key: '2', testId: 'rating-hard', className: 'hard' },
  { rating: Rating.Good, label: 'Good', key: '3', testId: 'rating-good', className: 'good' },
  { rating: Rating.Easy, label: 'Easy', key: '4', testId: 'rating-easy', className: 'easy' },
];

/**
 * The four rating buttons (FR-5.2): Again/Hard/Good/Easy mapped to the integer wire values 0/1/2/3.
 * Buttons are the primary input; keyboard 1–4 is the secondary path (handled in StudyPage).
 * Disabled until the card is revealed.
 */
export default function RatingButtons({ disabled, onRate }: RatingButtonsProps) {
  return (
    <div
      className="rating-buttons"
      data-testid="rating-buttons"
      role="group"
      aria-label="Rate this card"
    >
      {OPTIONS.map((opt) => (
        <button
          key={opt.rating}
          type="button"
          className={`rating-button rating-${opt.className}`}
          data-testid={opt.testId}
          disabled={disabled}
          onClick={() => onRate(opt.rating)}
        >
          <span className="rating-label">{opt.label}</span>
          <span className="rating-key" aria-hidden="true">
            {opt.key}
          </span>
        </button>
      ))}
    </div>
  );
}
