package com.wordbook.service;

import com.wordbook.domain.LearningState;

/**
 * Result of an SM-2 calculation: the next interval (days), the new ease factor, and the new
 * learning state. Mirrors the Swift tuple {@code (interval, easeFactor, state)}.
 *
 * <p>Note: {@code repetitions} is intentionally NOT part of this result — the caller
 * (StudySessionService) owns the repetitions update (AGAIN -> 0, else +1), matching the original
 * Swift design where the engine returns only the tuple above.
 */
public record SrsResult(int interval, double easeFactor, LearningState state) {}
