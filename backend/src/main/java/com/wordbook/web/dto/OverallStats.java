package com.wordbook.web.dto;

/**
 * Overall card distribution by learning state (FR-6.2, BR-ST3). {@code totalCards} is the full
 * count (includes RELEARNING); the three state chips preserve the original 4-chip layout
 * (Total/New/Learning/Mastered), so their sum may differ from total when RELEARNING cards exist —
 * intentional (original Swift behavior). Response-only DTO.
 *
 * @param totalCards total number of cards
 * @param newCards cards in NEW state
 * @param learningCards cards in LEARNING state
 * @param masteredCards cards in MASTERED state
 */
public record OverallStats(int totalCards, int newCards, int learningCards, int masteredCards) {}
