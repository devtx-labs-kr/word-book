package com.wordbook.web.dto;

import com.wordbook.service.StudySessionStart;
import java.util.List;
import java.util.UUID;

/**
 * Response when a study session starts: the session id, the ordered (shuffled) cards, and {@code
 * total} = card count (component-methods contract {sessionId, cards, total}).
 */
public record StudyStartResponse(UUID sessionId, List<CardResponse> cards, int total) {

  public static StudyStartResponse from(StudySessionStart start) {
    List<CardResponse> cards = start.cards().stream().map(CardResponse::from).toList();
    return new StudyStartResponse(start.session().getId(), cards, cards.size());
  }
}
