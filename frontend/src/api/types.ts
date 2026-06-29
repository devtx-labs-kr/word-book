// Shared API types (mirror the backend DTOs).

export type LearningState = 'new' | 'learning' | 'mastered' | 'relearning';

// Rating wire format = integer 0-3.
export const Rating = {
  Again: 0,
  Hard: 1,
  Good: 2,
  Easy: 3,
} as const;

export interface DeckResponse {
  id: string;
  name: string;
  descriptionText: string;
  color: string;
  createdAt: string;
  updatedAt: string;
  totalCards: number;
}

export interface DeckSummaryResponse extends DeckResponse {
  dueCards: number;
  newCards: number;
}

export interface CardResponse {
  id: string;
  front: string;
  back: string;
  example: string | null;
  pronunciation: string | null;
  notes: string | null;
  learningState: LearningState;
  easeFactor: number;
  interval: number;
  repetitions: number;
  nextReviewDate: string;
  lastReviewedAt: string | null;
  totalReviews: number;
  correctReviews: number;
}

export interface DeckRequest {
  name: string;
  descriptionText?: string;
  color?: string;
}

export interface CardRequest {
  front: string;
  back: string;
  example?: string;
  pronunciation?: string;
  notes?: string;
}

export type CardFilter = 'All' | 'New' | 'Learning' | 'Mastered' | 'Due';
export type DeckSort = 'name' | 'createdAt' | 'due';

export interface StudyStartResponse {
  sessionId: string;
  cards: CardResponse[];
  total?: number;
}

export interface SessionSummary {
  cardsReviewed: number;
  correctAnswers: number;
  accuracy: number;
  totalTimeSpent: number;
}

// Settings (U6 / FR-9). Mirrors the backend SettingsResponse DTO. `studyReminderTime` is an
// ISO-8601 instant or null. `UpdateSettingsRequest` is the full-body PUT payload (BR-S4 — partial
// patches are not supported; the client merges the current settings with its change before sending).
export interface SettingsResponse {
  newCardsPerDay: number;
  maxReviewsPerDay: number;
  showPronunciation: boolean;
  darkMode: boolean;
  autoPlayAudio: boolean;
  studyReminderEnabled: boolean;
  studyReminderTime: string | null;
}

export type UpdateSettingsRequest = SettingsResponse; // full-body send

// Statistics (U4 / FR-6). Mirror the backend DTOs; all counters are numbers, `startedAt` is an
// ISO-8601 instant string, `accuracy` is a 0.0–1.0 ratio.
export interface TodayStats {
  cardsReviewed: number;
  correctAnswers: number;
  accuracy: number;
  studyTimeSeconds: number;
  sessionCount: number;
}

export interface OverallStats {
  totalCards: number;
  newCards: number;
  learningCards: number;
  masteredCards: number;
}

export interface UpcomingForecast {
  dueToday: number;
  dueTomorrow: number;
  dueThisWeek: number;
}

export interface SessionView {
  sessionId: string;
  deckName: string;
  cardsReviewed: number;
  accuracy: number;
  startedAt: string;
}

export interface StatisticsResponse {
  today: TodayStats;
  overall: OverallStats;
  upcoming: UpcomingForecast;
  recentSessions: SessionView[];
}

export interface CardStat {
  word: string;
  interval: number;
  easeFactor: number;
  totalReviews: number;
  accuracy: number;
}

// Import preview (U5 / US-E2). `valid` signals the bytes decode into a WordBook export;
// `duplicateName` carries the final "<name> (Imported)" name when a same-named deck exists.
export interface ImportPreview {
  valid: boolean;
  deckName: string | null;
  cardCount: number;
  duplicateName: string | null;
}
