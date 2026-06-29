package com.wordbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wordbook.domain.Card;
import com.wordbook.domain.Deck;
import com.wordbook.domain.LearningState;
import com.wordbook.domain.StudySession;
import com.wordbook.exception.NotFoundException;
import com.wordbook.repository.CardRepository;
import com.wordbook.repository.DeckRepository;
import com.wordbook.repository.StudySessionRepository;
import com.wordbook.web.dto.CardStat;
import com.wordbook.web.dto.StatisticsResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Service-level tests for U4 statistics aggregation: today sums from session counters within the
 * local-day window, overall counts by state (incl. total), forecast buckets, recent top-10 desc,
 * per-card 0-safe accuracy, empty → zeros, and the Unknown Deck fallback for a deleted deck.
 */
@SpringBootTest
class StatisticsServiceTest {

  @Autowired private StatisticsService statisticsService;
  @Autowired private DeckRepository deckRepository;
  @Autowired private CardRepository cardRepository;
  @Autowired private StudySessionRepository studySessionRepository;

  @BeforeEach
  void clean() {
    studySessionRepository.deleteAll();
    cardRepository.deleteAll();
    deckRepository.deleteAll();
  }

  private Deck saveDeck(String name) {
    return deckRepository.save(new Deck(name, null, null));
  }

  private Card saveCard(Deck deck, LearningState state, Instant nextReview) {
    Card card = new Card("front-" + UUID.randomUUID(), "back");
    card.setDeck(deck);
    card.setLearningState(state);
    card.setNextReviewDate(nextReview);
    return cardRepository.save(card);
  }

  private StudySession saveSession(
      UUID deckId, Instant startedAt, int reviewed, int correct, double time) {
    StudySession session = new StudySession(deckId);
    session.setCardsReviewed(reviewed);
    session.setCorrectAnswers(correct);
    session.setTotalTimeSpent(time);
    // startedAt defaults to now in the constructor; force it via reflection-free re-save path.
    setStartedAt(session, startedAt);
    return studySessionRepository.save(session);
  }

  /** StudySession has no startedAt setter; set it reflectively for test fixtures only. */
  private void setStartedAt(StudySession session, Instant startedAt) {
    try {
      var field = StudySession.class.getDeclaredField("startedAt");
      field.setAccessible(true);
      field.set(session, startedAt);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void todaySumsFromSessionCountersWithinLocalDayWindow() {
    Deck deck = saveDeck("Today");
    Instant noonToday =
        LocalDate.now(ZoneId.systemDefault())
            .atTime(12, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant();
    saveSession(deck.getId(), noonToday, 20, 18, 300);
    saveSession(deck.getId(), noonToday, 10, 8, 120);
    // A session from yesterday must NOT count.
    saveSession(deck.getId(), noonToday.minus(1, ChronoUnit.DAYS), 99, 99, 999);

    StatisticsResponse stats = statisticsService.getStatistics();

    assertThat(stats.today().cardsReviewed()).isEqualTo(30);
    assertThat(stats.today().correctAnswers()).isEqualTo(26);
    assertThat(stats.today().sessionCount()).isEqualTo(2);
    assertThat(stats.today().studyTimeSeconds()).isEqualTo(420.0);
    assertThat(stats.today().accuracy()).isEqualTo(26.0 / 30.0);
  }

  @Test
  void overallCountsByStateIncludingTotal() {
    Deck deck = saveDeck("Overall");
    Instant future = Instant.now().plus(30, ChronoUnit.DAYS);
    saveCard(deck, LearningState.NEW, future);
    saveCard(deck, LearningState.NEW, future);
    saveCard(deck, LearningState.LEARNING, future);
    saveCard(deck, LearningState.MASTERED, future);
    saveCard(deck, LearningState.RELEARNING, future);

    StatisticsResponse stats = statisticsService.getStatistics();

    assertThat(stats.overall().totalCards()).isEqualTo(5);
    assertThat(stats.overall().newCards()).isEqualTo(2);
    assertThat(stats.overall().learningCards()).isEqualTo(1);
    assertThat(stats.overall().masteredCards()).isEqualTo(1);
  }

  @Test
  void forecastBucketsByLocalBoundaries() {
    Deck deck = saveDeck("Forecast");
    ZoneId zone = ZoneId.systemDefault();
    LocalDate today = LocalDate.now(zone);
    Instant earlierToday = today.atStartOfDay(zone).toInstant().plus(1, ChronoUnit.HOURS);
    Instant tomorrowNoon = today.plusDays(1).atTime(12, 0).atZone(zone).toInstant();
    Instant inFiveDays = today.plusDays(5).atTime(12, 0).atZone(zone).toInstant();
    Instant inThirtyDays = today.plusDays(30).atStartOfDay(zone).toInstant();

    saveCard(deck, LearningState.LEARNING, earlierToday); // dueToday + dueThisWeek
    saveCard(deck, LearningState.LEARNING, tomorrowNoon); // dueTomorrow + dueThisWeek
    saveCard(deck, LearningState.LEARNING, inFiveDays); // dueThisWeek only
    saveCard(deck, LearningState.LEARNING, inThirtyDays); // none

    StatisticsResponse stats = statisticsService.getStatistics();

    assertThat(stats.upcoming().dueToday()).isEqualTo(1);
    assertThat(stats.upcoming().dueTomorrow()).isEqualTo(1);
    // cumulative today..+7d: earlierToday + tomorrowNoon + inFiveDays = 3.
    assertThat(stats.upcoming().dueThisWeek()).isEqualTo(3);
  }

  @Test
  void recentSessionsAreNewestFirstAndCappedAtTen() {
    Deck deck = saveDeck("Recent");
    Instant base = Instant.now().minus(20, ChronoUnit.DAYS);
    for (int i = 0; i < 12; i++) {
      saveSession(deck.getId(), base.plus(i, ChronoUnit.HOURS), i, i, 1);
    }

    StatisticsResponse stats = statisticsService.getStatistics();

    assertThat(stats.recentSessions()).hasSize(10);
    // Newest first: the i=11 session (latest startedAt) leads.
    assertThat(stats.recentSessions().get(0).cardsReviewed()).isEqualTo(11);
    assertThat(stats.recentSessions().get(0).startedAt())
        .isAfter(stats.recentSessions().get(1).startedAt());
    assertThat(stats.recentSessions().get(0).deckName()).isEqualTo("Recent");
  }

  @Test
  void recentSessionFallsBackToUnknownDeckForDeletedDeck() {
    UUID ghostDeckId = UUID.randomUUID();
    saveSession(ghostDeckId, Instant.now(), 5, 4, 10);

    StatisticsResponse stats = statisticsService.getStatistics();

    assertThat(stats.recentSessions()).hasSize(1);
    assertThat(stats.recentSessions().get(0).deckName()).isEqualTo("Unknown Deck");
    assertThat(stats.recentSessions().get(0).accuracy()).isEqualTo(4.0 / 5.0);
  }

  @Test
  void emptyDataYieldsZerosAndEmptyList() {
    StatisticsResponse stats = statisticsService.getStatistics();

    assertThat(stats.today().cardsReviewed()).isZero();
    assertThat(stats.today().accuracy()).isZero();
    assertThat(stats.today().sessionCount()).isZero();
    assertThat(stats.overall().totalCards()).isZero();
    assertThat(stats.upcoming().dueToday()).isZero();
    assertThat(stats.recentSessions()).isEmpty();
  }

  @Test
  void perCardAccuracyIsZeroSafe() {
    Deck deck = saveDeck("PerCard");
    Card reviewed = saveCard(deck, LearningState.LEARNING, Instant.now());
    reviewed.setTotalReviews(4);
    reviewed.setCorrectReviews(3);
    reviewed.setInterval(6);
    reviewed.setEaseFactor(2.5);
    cardRepository.save(reviewed);
    // A never-reviewed card → accuracy 0, not a divide-by-zero error.
    saveCard(deck, LearningState.NEW, Instant.now());

    List<CardStat> stats = statisticsService.perCard(deck.getId());

    assertThat(stats).hasSize(2);
    CardStat reviewedStat =
        stats.stream().filter(s -> s.totalReviews() == 4).findFirst().orElseThrow();
    assertThat(reviewedStat.accuracy()).isEqualTo(3.0 / 4.0);
    assertThat(reviewedStat.interval()).isEqualTo(6);
    CardStat newStat = stats.stream().filter(s -> s.totalReviews() == 0).findFirst().orElseThrow();
    assertThat(newStat.accuracy()).isZero();
  }

  @Test
  void perCardUnknownDeckThrowsNotFound() {
    assertThatThrownBy(() -> statisticsService.perCard(UUID.randomUUID()))
        .isInstanceOf(NotFoundException.class);
  }
}
