import { KeyboardEvent, useId } from 'react';

interface ToggleProps {
  /** Visible label text. */
  label: string;
  /** Optional secondary description, linked via `aria-describedby`. */
  description?: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
  disabled?: boolean;
  'data-testid'?: string;
}

/**
 * Accessible switch (NFR-8). Renders `role="switch"` with `aria-checked`, a visible label, and an
 * optional description linked via `aria-describedby`. Toggles on click and on Space/Enter
 * (frontend-components §7). Used for showPronunciation and darkMode.
 */
export default function Toggle({
  label,
  description,
  checked,
  onChange,
  disabled = false,
  'data-testid': testId,
}: ToggleProps) {
  const labelId = useId();
  const descId = useId();

  const toggle = () => {
    if (!disabled) onChange(!checked);
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLButtonElement>) => {
    if (e.key === ' ' || e.key === 'Enter') {
      e.preventDefault();
      toggle();
    }
  };

  return (
    <div className="toggle-row">
      <div className="toggle-text">
        <span id={labelId} className="toggle-label">
          {label}
        </span>
        {description && (
          <span id={descId} className="toggle-description muted">
            {description}
          </span>
        )}
      </div>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        aria-labelledby={labelId}
        aria-describedby={description ? descId : undefined}
        disabled={disabled}
        onClick={toggle}
        onKeyDown={handleKeyDown}
        data-testid={testId}
        className={checked ? 'toggle-switch on' : 'toggle-switch'}
      >
        <span className="toggle-knob" aria-hidden="true" />
      </button>
    </div>
  );
}
