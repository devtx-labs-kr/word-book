package com.wordbook.service;

import com.wordbook.domain.Card;
import com.wordbook.domain.Deck;
import com.wordbook.domain.ReviewRating;
import com.wordbook.domain.ReviewRecord;
import com.wordbook.domain.StudySession;
import com.wordbook.domain.UserSettings;
import com.wordbook.exception.NotFoundException;
import com.wordbook.repository.CardRepository;
import com.wordbook.repository.DeckRepository;
import com.wordbook.repository.ReviewRecordRepository;
import com.wordbook.repository.StudySessionRepository;
import com.wordbook.repository.UserSettingsRepository;
import com.wordbook.web.dto.SessionSummary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Study session orchestration. Implements the U1 walking-skeleton slice: start a session (due+new
 * selection, shuffled) and submit an answer (the SM-2 transaction, business-logic-model §4).
 */
@Service
public class StudySessionService {

  private static final int DEFAULT_NEW_CARDS_PER_DAY = 20;
  private static final int DEFAULT_MAX_REVIEWS_PER_DAY = 200;

  private final SrsEngine srsEngine;
  private final DeckRepository deckRepository;
  private final CardRepository cardRepository;
  private final StudySessionRepository studySessionRepository;
  private final ReviewRecordRepository reviewRecordRepository;
  private final UserSettingsRepository userSettingsRepository;

  public StudySessionService(
      SrsEngine srsEngine,
      DeckRepository deckRepository,
      CardRepository cardRepository,
      StudySessionRepository studySessionRepository,
      ReviewRecordRepository reviewRecordRepository,
      UserSettingsRepository userSettingsRepository) {
    this.srsEngine = srsEngine;
    this.deckRepository = deckRepository;
    this.cardRepository = cardRepository;
    this.studySessionRepository = studySessionRepository;
    this.reviewRecordRepository = reviewRecordRepository;
    this.userSettingsRepository = userSettingsRepository;
  }

  /**
   * Starts a study session for a deck: selects due (up to maxReviewsPerDay) + new (up to
   * newCardsPerDay) cards, shuffles them, persists the session, and returns the ordered cards
   * (BR-3). Falls back to defaults 20/200 when no settings row exists.
   */
  @Transactional
  public StudySessionStart start(UUID deckId) {
    Deck deck =
        deckRepository
            .findById(deckId)
            .orElseThrow(() -> new NotFoundException("Deck not found: " + deckId));

    UserSettings settings = userSettingsRepository.findAll().stream().findFirst().orElse(null);
    int newLimit = settings != null ? settings.getNewCardsPerDay() : DEFAULT_NEW_CARDS_PER_DAY;
    int maxReviews =
        settings != null ? settings.getMaxReviewsPerDay() : DEFAULT_MAX_REVIEWS_PER_DAY;

    Instant now = Instant.now();
    List<Card> due =
        cardRepository.findByDeckIdAndNextReviewDateLessThanEqualOrderByNextReviewDateAsc(
            deck.getId(), now, PageRequest.of(0, maxReviews));
    List<Card> fresh =
        cardRepository.findByDeckIdAndLearningStateOrderByCreatedAtAsc(
            deck.getId(), com.wordbook.domain.LearningState.NEW, PageRequest.of(0, newLimit));

    // Merge preserving uniqueness (a brand-new card can be both due and NEW).
    LinkedHashSet<Card> merged = new LinkedHashSet<>();
    merged.addAll(due);
    merged.addAll(fresh);
    List<Card> selected = new ArrayList<>(merged);
    Collections.shuffle(selected);

    StudySession session = studySessionRepository.save(new StudySession(deck.getId()));
    return new StudySessionStart(session, selected);
  }

  /**
   * Submits an answer for a card within a session. Runs the SM-2 transaction (business-logic-model
   * §4): SrsEngine calculation -> Card SRS fields & stats -> repetitions (AGAIN?0:+1) ->
   * nextReviewDate -> ReviewRecord -> session stats. Atomic; errors surface (no silent failure).
   *
   * @param timeSpentMs time spent in milliseconds; stored as seconds ({@code timeSpentMs / 1000.0})
   */
  @Transactional
  public Card submitAnswer(UUID sessionId, UUID cardId, ReviewRating rating, long timeSpentMs) {
    StudySession session =
        studySessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Study session not found: " + sessionId));
    Card card =
        cardRepository
            .findById(cardId)
            .orElseThrow(() -> new NotFoundException("Card not found: " + cardId));

    SrsResult result = srsEngine.calculateNextReview(card, rating);

    int previousInterval = card.getInterval();
    double previousEaseFactor = card.getEaseFactor();
    Instant now = Instant.now();

    card.setInterval(result.interval());
    card.setEaseFactor(result.easeFactor());
    card.setLearningState(result.state());
    card.setRepetitions(rating == ReviewRating.AGAIN ? 0 : card.getRepetitions() + 1);
    card.setNextReviewDate(srsEngine.calculateNextReviewDate(result.interval(), now));
    card.setLastReviewedAt(now);
    card.setUpdatedAt(now);
    card.setTotalReviews(card.getTotalReviews() + 1);
    if (rating != ReviewRating.AGAIN) {
      card.setCorrectReviews(card.getCorrectReviews() + 1);
    }
    cardRepository.save(card);

    double timeSpentSeconds = timeSpentMs / 1000.0;
    ReviewRecord record =
        new ReviewRecord(
            card,
            rating,
            previousInterval,
            result.interval(),
            previousEaseFactor,
            result.easeFactor(),
            timeSpentSeconds);
    reviewRecordRepository.save(record);

    session.setCardsReviewed(session.getCardsReviewed() + 1);
    if (rating != ReviewRating.AGAIN) {
      session.setCorrectAnswers(session.getCorrectAnswers() + 1);
    }
    session.setTotalTimeSpent(session.getTotalTimeSpent() + timeSpentSeconds);
    studySessionRepository.save(session);

    return card;
  }

  /**
   * Ends (completes) a study session and returns its summary (business-logic-model §4). Idempotent
   * (BR-SES-3): re-ending an already-ended session keeps the existing {@code endedAt} and returns
   * the same statistics. No re-aggregation — the summary is built from the counters {@code
   * submitAnswer} accumulated (BR-SES-4, single source of truth). Mid-session exit reuses this same
   * path (BR-SES-5).
   *
   * @return the session summary (cardsReviewed, correctAnswers, accuracy, totalTimeSpent)
   */
  @Transactional
  public SessionSummary end(UUID sessionId) {
    StudySession session =
        studySessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Study session not found: " + sessionId));

    if (session.isActive()) {
      session.setEndedAt(Instant.now());
      studySessionRepository.save(session);
    }
    return SessionSummary.from(session);
  }
}
