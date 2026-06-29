import { ReactNode } from 'react';
import { NavLink } from 'react-router-dom';
import ThemeToggle from './ThemeToggle';

interface NavItem {
  to: string;
  icon: string;
  label: string;
  enabled: boolean;
}

const NAV_ITEMS: NavItem[] = [
  { to: '/decks', icon: '📚', label: 'Decks', enabled: true },
  { to: '/stats', icon: '📊', label: 'Statistics', enabled: true },
  { to: '/settings', icon: '⚙️', label: 'Settings', enabled: true },
];

/**
 * Sidebar + main layout. The primary nav (Decks/Stats/Settings) marks the active route with
 * {@code aria-current="page"}. The header carries the real {@link ThemeToggle} (U6); the
 * Statistics link is enabled in U4 ({@code /stats}).
 */
export default function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="app-shell" data-testid="app-shell">
      <header className="topbar">
        <span className="topbar-brand">WordBook</span>
        <ThemeToggle />
      </header>
      <div className="app-body">
        <nav className="app-sidebar" aria-label="Primary">
          <ul className="app-nav" role="list">
            {NAV_ITEMS.map((item) =>
              item.enabled ? (
                <li key={item.to}>
                  <NavLink
                    to={item.to}
                    data-testid={`nav-${item.label.toLowerCase()}`}
                    className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
                  >
                    <span aria-hidden="true">{item.icon}</span> {item.label}
                  </NavLink>
                </li>
              ) : (
                <li key={item.to}>
                  <span className="nav-link disabled" aria-disabled="true" title="Coming soon">
                    <span aria-hidden="true">{item.icon}</span> {item.label}
                  </span>
                </li>
              ),
            )}
          </ul>
        </nav>
        <main className="app-main">{children}</main>
      </div>
    </div>
  );
}
