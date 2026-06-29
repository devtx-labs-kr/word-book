package com.wordbook.web.dto;

import java.util.List;

/**
 * Assembled response for {@code GET /api/statistics} (FR-6.1~6.4). One controller call returns all
 * four sections; empty data yields zeros/empty list with HTTP 200, not an error (BR-ST7/ST11).
 * Response-only DTO.
 *
 * @param today today's aggregates
 * @param overall overall card distribution
 * @param upcoming upcoming review forecast
 * @param recentSessions up to 10 most recent sessions, newest first
 */
public record StatisticsResponse(
    TodayStats today,
    OverallStats overall,
    UpcomingForecast upcoming,
    List<SessionView> recentSessions) {}
