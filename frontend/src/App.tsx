import { Routes, Route, Navigate, Outlet } from 'react-router-dom';
import AppShell from './components/AppShell';
import DeckListPage from './pages/DeckListPage';
import CardManagementPage from './pages/CardManagementPage';
import StudyPage from './pages/StudyPage';
import StudyCompletePage from './pages/StudyCompletePage';
import SettingsPage from './pages/SettingsPage';
import StatisticsPage from './pages/StatisticsPage';

/** Layout route: the app-shell pages render inside the sidebar shell via {@code <Outlet/>}. */
function ShellLayout() {
  return (
    <AppShell>
      <Outlet />
    </AppShell>
  );
}

/**
 * Application router. Shell pages ({@code /decks}, {@code /decks/:id}) render inside the AppShell
 * sidebar via a layout route; the full-screen study routes ({@code /study/:deckId} and {@code
 * /study/:deckId/complete}) are siblings outside the layout so they render without the sidebar.
 * Root redirects to the deck list.
 */
export default function App() {
  return (
    <Routes>
      <Route element={<ShellLayout />}>
        <Route path="/" element={<Navigate to="/decks" replace />} />
        <Route path="/decks" element={<DeckListPage />} />
        <Route path="/decks/:id" element={<CardManagementPage />} />
        <Route path="/stats" element={<StatisticsPage />} />
        <Route path="/settings" element={<SettingsPage />} />
      </Route>
      <Route path="/study/:deckId" element={<StudyPage />} />
      <Route path="/study/:deckId/complete" element={<StudyCompletePage />} />
      <Route path="*" element={<Navigate to="/decks" replace />} />
    </Routes>
  );
}
