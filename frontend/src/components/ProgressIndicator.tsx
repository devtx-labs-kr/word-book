import { CSSProperties } from 'react';

interface ProgressIndicatorProps {
  current: number;
  total: number;
  remaining: number;
}

/** Study progress (FR-4.8): "current / total" text plus an accessible progressbar (BR-PRG-*). */
export default function ProgressIndicator({ current, total, remaining }: ProgressIndicatorProps) {
  return (
    <div
      className="progress-indicator"
      data-testid="progress-indicator"
      role="progressbar"
      aria-valuenow={current}
      aria-valuemin={0}
      aria-valuemax={total}
      aria-label="Study progress"
    >
      <span className="progress-text" data-testid="progress-text">
        {current} / {total}
      </span>
      <span className="progress-remaining">{remaining} left</span>
      <span
        className="progress-bar"
        aria-hidden="true"
        style={{ '--progress': total === 0 ? 0 : current / total } as CSSProperties}
      />
      <span className="progress-percent" aria-hidden="true">
        {total === 0 ? 0 : Math.round((current / total) * 100)}%
      </span>
    </div>
  );
}
