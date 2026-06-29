package com.wordbook.web;

import com.wordbook.service.StatisticsService;
import com.wordbook.web.dto.CardStat;
import com.wordbook.web.dto.StatisticsResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only statistics endpoints (FR-6). {@code GET /api/statistics} returns the assembled
 * dashboard; {@code GET /api/decks/{deckId}/card-stats} returns per-card stats for a deck. Empty
 * data is a normal 200 (zeros/empty list), an unknown deck is a 404 — both via the shared {@link
 * GlobalExceptionHandler} (no new handler, BR-ST11).
 */
@RestController
@RequestMapping("/api")
public class StatisticsController {

  private final StatisticsService statisticsService;

  public StatisticsController(StatisticsService statisticsService) {
    this.statisticsService = statisticsService;
  }

  @GetMapping("/statistics")
  public StatisticsResponse getStatistics() {
    return statisticsService.getStatistics();
  }

  @GetMapping("/decks/{deckId}/card-stats")
  public List<CardStat> cardStats(@PathVariable UUID deckId) {
    return statisticsService.perCard(deckId);
  }
}
