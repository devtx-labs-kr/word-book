package com.wordbook.service;

import com.wordbook.domain.LearningState;
import com.wordbook.domain.StudySession;
import com.wordbook.exception.NotFoundException;
import com.wordbook.repository.CardRepository;
import com.wordbook.repository.DeckRepository;
import com.wordbook.repository.StudySessionRepository;
import com.wordbook.web.dto.CardStat;
import com.wordbook.web.dto.OverallStats;
import com.wordbook.web.dto.SessionView;
import com.wordbook.web.dto.StatisticsResponse;
import com.wordbook.web.dto.TodayStats;
import com.wordbook.web.dto.UpcomingForecast;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only statistics aggregation (FR-6, U4). Computes today's totals, the overall card
 * distribution, the upcoming-review forecast, and recent sessions from existing entities — no new
 * entity, no writes, no SM-2 recompute (INV-S3, BR-ST10). Today's numbers are summed from the
 * {@code StudySession} counters (not a {@code ReviewRecord} re-sum), matching the original Swift
 * app (domain-entities §3 note). Day/forecast boundaries use the server-local timezone ({@link
 * ZoneId#systemDefault()}), consistent with nextReviewDate (FR-3.7, BR-ST1/ST5).
 */
@Service
@Transactional(readOnly = true)
public class StatisticsService {

  private static final String UNKNOWN_DECK = "Unknown Deck";

  private final StudySessionRepository studySessionRepository;
  private final CardRepository cardRepository;
  private final DeckRepository deckRepository;

  public StatisticsService(
      StudySessionRepository studySessionRepository,
      CardRepository cardRepository,
      DeckRepository deckRepository) {
    this.studySessionRepository = studySessionRepository;
    this.cardRepository = cardRepository;
    this.deckRepository = deckRepository;
  }

  /** Assembles all four sections in a single read-only transaction (FR-6.1~6.4, PD-D-U4-1). */
  public StatisticsResponse getStatistics() {
    return new StatisticsResponse(
        computeToday(), computeOverall(), computeUpcoming(), recentSessions(10));
  }

  /** Today's aggregates summed from session counters within the local-day window (BR-ST1/ST2). */
  private TodayStats computeToday() {
    ZoneId zone = ZoneId.systemDefault();
    LocalDate today = LocalDate.now(zone);
    Instant start = today.atStartOfDay(zone).toInstant();
    Instant end = today.plusDays(1).atStartOfDay(zone).toInstant();

    List<StudySession> sessions =
        studySessionRepository.findByStartedAtGreaterThanEqualAndStartedAtLessThan(start, end);

    int cardsReviewed = 0;
    int correctAnswers = 0;
    double studyTime = 0;
    for (StudySession s : sessions) {
      cardsReviewed += s.getCardsReviewed();
      correctAnswers += s.getCorrectAnswers();
      studyTime += s.getTotalTimeSpent();
    }
    // double division, 0 when no cards reviewed (BR-ST8).
    double accuracy = cardsReviewed > 0 ? (double) correctAnswers / cardsReviewed : 0;
    return new TodayStats(cardsReviewed, correctAnswers, accuracy, studyTime, sessions.size());
  }

  /** Overall card distribution by learning state (BR-ST3). long counts narrowed to int (BR #4). */
  private OverallStats computeOverall() {
    int totalCards = (int) cardRepository.count();
    int newCards = (int) cardRepository.countByLearningState(LearningState.NEW);
    int learningCards = (int) cardRepository.countByLearningState(LearningState.LEARNING);
    int masteredCards = (int) cardRepository.countByLearningState(LearningState.MASTERED);
    return new OverallStats(totalCards, newCards, learningCards, masteredCards);
  }

  /** Upcoming-review forecast over local-timezone boundaries (BR-ST5). */
  private UpcomingForecast computeUpcoming() {
    ZoneId zone = ZoneId.systemDefault();
    LocalDate today = LocalDate.now(zone);
    Instant endToday = today.plusDays(1).atStartOfDay(zone).toInstant();
    Instant endTomorrow = today.plusDays(2).atStartOfDay(zone).toInstant();
    // "This week" boundary fixed at today + 7 days local midnight (review #1).
    Instant endWeek = today.plusDays(7).atStartOfDay(zone).toInstant();

    int dueToday = (int) cardRepository.countByNextReviewDateLessThan(endToday);
    int dueTomorrow =
        (int)
            cardRepository.countByNextReviewDateGreaterThanEqualAndNextReviewDateLessThan(
                endToday, endTomorrow);
    int dueThisWeek = (int) cardRepository.countByNextReviewDateLessThan(endWeek);
    return new UpcomingForecast(dueToday, dueTomorrow, dueThisWeek);
  }

  /** Up to {@code limit} most recent sessions, newest first; deck name falls back per DR-3. */
  private List<SessionView> recentSessions(int limit) {
    return studySessionRepository.findTop10ByOrderByStartedAtDesc().stream()
        .limit(limit)
        .map(
            s ->
                SessionView.from(
                    s,
                    deckRepository
                        .findById(s.getDeckId())
                        .map(com.wordbook.domain.Deck::getName)
                        .orElse(UNKNOWN_DECK)))
        .toList();
  }

  /**
   * Per-card statistics for a deck (FR-6.5, BR-ST6). Throws {@link NotFoundException} (→ 404) when
   * the deck does not exist (BR-ST11).
   */
  public List<CardStat> perCard(UUID deckId) {
    if (!deckRepository.existsById(deckId)) {
      throw new NotFoundException("Deck not found: " + deckId);
    }
    return cardRepository.findByDeckId(deckId).stream().map(CardStat::from).toList();
  }
}
