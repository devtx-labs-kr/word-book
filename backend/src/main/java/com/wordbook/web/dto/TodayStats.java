package com.wordbook.web.dto;

/**
 * Today's study aggregates, summed from {@code StudySession} counters within the server-local day
 * window (FR-6.1, BR-ST2). Response-only DTO (never persisted).
 *
 * @param cardsReviewed Σ today sessions' cardsReviewed
 * @param correctAnswers Σ today sessions' correctAnswers
 * @param accuracy {@code correctAnswers / cardsReviewed}, 0 when none reviewed (BR-ST8)
 * @param studyTimeSeconds Σ today sessions' totalTimeSpent (seconds)
 * @param sessionCount number of sessions started today
 */
public record TodayStats(
    int cardsReviewed,
    int correctAnswers,
    double accuracy,
    double studyTimeSeconds,
    int sessionCount) {}
