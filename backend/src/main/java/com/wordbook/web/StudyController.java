package com.wordbook.web;

import com.wordbook.domain.Card;
import com.wordbook.service.StudySessionService;
import com.wordbook.service.StudySessionStart;
import com.wordbook.web.dto.AnswerRequest;
import com.wordbook.web.dto.CardResponse;
import com.wordbook.web.dto.SessionSummary;
import com.wordbook.web.dto.StudyStartResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for the study flow: start a session and submit answers. */
@RestController
public class StudyController {

  private final StudySessionService studySessionService;

  public StudyController(StudySessionService studySessionService) {
    this.studySessionService = studySessionService;
  }

  @PostMapping("/api/decks/{deckId}/study")
  public ResponseEntity<StudyStartResponse> start(@PathVariable UUID deckId) {
    StudySessionStart start = studySessionService.start(deckId);
    return ResponseEntity.ok(StudyStartResponse.from(start));
  }

  @PostMapping("/api/study/{sessionId}/answer")
  public ResponseEntity<CardResponse> answer(
      @PathVariable UUID sessionId, @Valid @RequestBody AnswerRequest request) {
    Card card =
        studySessionService.submitAnswer(
            sessionId, request.getCardId(), request.getRating(), request.getTimeSpentMs());
    return ResponseEntity.ok(CardResponse.from(card));
  }

  /**
   * Completes (ends) a study session and returns its summary (business-logic-model §4). The service
   * method is {@code end}; the endpoint is {@code complete} (complete ↔ end). Idempotent —
   * re-ending an already-ended session returns the same summary. Mid-session exit uses this same
   * endpoint (BR-SES-5).
   */
  @PostMapping("/api/study/{sessionId}/complete")
  public ResponseEntity<SessionSummary> complete(@PathVariable UUID sessionId) {
    return ResponseEntity.ok(studySessionService.end(sessionId));
  }
}
