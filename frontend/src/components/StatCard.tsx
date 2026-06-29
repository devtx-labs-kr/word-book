interface StatCardProps {
  label: string;
  value: string;
  /** A CSS color (token var, e.g. {@code var(--accent)}) applied to the value text. */
  color: string;
  icon?: string;
  testId?: string;
}

/**
 * A single metric card: a large colored value over a small secondary label (frontend-components §3).
 * Display-only. The value carries the meaning as text; {@code color} is decorative, so the
 * label-value pair stays readable for screen readers (NFR-8). References tokens only, so it adapts
 * to the dark theme automatically (frontend-components §7).
 */
export default function StatCard({ label, value, color, icon, testId }: StatCardProps) {
  return (
    <div className="stat-card" data-testid={testId}>
      <span className="stat-card-value" style={{ color }}>
        {icon && (
          <span className="stat-card-icon" aria-hidden="true">
            {icon}{' '}
          </span>
        )}
        {value}
      </span>
      <span className="stat-card-label">{label}</span>
    </div>
  );
}
