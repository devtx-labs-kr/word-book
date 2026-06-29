package com.wordbook.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

/** Verifies server-local calendar-day addition for the next review date (ADR-8). */
class SrsEngineDateTest {

  private final SrsEngine engine = new SrsEngine();

  @Test
  void addsCalendarDaysInServerLocalZone() {
    ZoneId zone = ZoneId.systemDefault();
    Instant from = ZonedDateTime.of(2026, 1, 10, 9, 30, 0, 0, zone).toInstant();

    Instant next = engine.calculateNextReviewDate(6, from);

    ZonedDateTime expected = ZonedDateTime.of(2026, 1, 16, 9, 30, 0, 0, zone);
    assertThat(next).isEqualTo(expected.toInstant());
  }

  @Test
  void zeroIntervalKeepsSameMoment() {
    Instant from = Instant.parse("2026-03-01T00:00:00Z");
    assertThat(engine.calculateNextReviewDate(0, from)).isEqualTo(from);
  }
}
