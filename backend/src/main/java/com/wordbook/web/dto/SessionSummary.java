package com.wordbook.web.dto;

import com.wordbook.domain.StudySession;

/**
 * Response for completing (ending) a study session — domain-entities §3.4. Statistics come straight
 * from the session counters that {@code submitAnswer} accumulated (BR-SES-4, single source of
 * truth); {@code end} does not re-aggregate. Response-only DTO (never persisted).
 *
 * @param cardsReviewed number of answers submitted in the session
 * @param correctAnswers answers rated above AGAIN
 * @param accuracy {@code correctAnswers / cardsReviewed}, 0.0–1.0 (0 when no cards reviewed)
 * @param totalTimeSpent accumulated review time in seconds
 */
public record SessionSummary(
    int cardsReviewed, int correctAnswers, double accuracy, double totalTimeSpent) {

  public static SessionSummary from(StudySession session) {
    return new SessionSummary(
        session.getCardsReviewed(),
        session.getCorrectAnswers(),
        session.getAccuracy(),
        session.getTotalTimeSpent());
  }
}
