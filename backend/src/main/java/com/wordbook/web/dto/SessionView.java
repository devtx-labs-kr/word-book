package com.wordbook.web.dto;

import com.wordbook.domain.StudySession;
import java.time.Instant;
import java.util.UUID;

/**
 * A single recent-session row (FR-6.3, BR-ST4). {@code deckName} is resolved from the session's
 * loose {@code deckId}; a deleted deck shows as "Unknown Deck" (DR-3, BR-ST9). {@code accuracy}
 * comes straight from {@link StudySession#getAccuracy()} (0-safe, BR-ST8). Response-only DTO.
 *
 * @param sessionId the session id
 * @param deckName the deck name, or "Unknown Deck" when the deck no longer exists
 * @param cardsReviewed cards reviewed in the session
 * @param accuracy session accuracy, 0.0–1.0
 * @param startedAt when the session started
 */
public record SessionView(
    UUID sessionId, String deckName, int cardsReviewed, double accuracy, Instant startedAt) {

  public static SessionView from(StudySession session, String deckName) {
    return new SessionView(
        session.getId(),
        deckName,
        session.getCardsReviewed(),
        session.getAccuracy(),
        session.getStartedAt());
  }
}
