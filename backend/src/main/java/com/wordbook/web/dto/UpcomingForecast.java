package com.wordbook.web.dto;

/**
 * Upcoming review forecast by due window (FR-6.4, BR-ST5), all in the server-local timezone. {@code
 * dueToday} and {@code dueThisWeek} are cumulative (≤ boundary); {@code dueTomorrow} is the
 * half-open tomorrow window — overlap is allowed (display-only forecast). Response-only DTO.
 *
 * @param dueToday cards due before today's local midnight (cumulative)
 * @param dueTomorrow cards due within tomorrow's window [today midnight, tomorrow midnight)
 * @param dueThisWeek cards due before today+7 days local midnight (cumulative)
 */
public record UpcomingForecast(int dueToday, int dueTomorrow, int dueThisWeek) {}
