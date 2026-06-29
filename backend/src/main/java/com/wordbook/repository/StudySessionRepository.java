package com.wordbook.repository;

import com.wordbook.domain.StudySession;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link StudySession}, including read-only statistics queries (U4, FR-6). */
public interface StudySessionRepository extends JpaRepository<StudySession, UUID> {

  /**
   * Sessions started within the half-open range {@code [start, end)} — used for today's aggregates.
   * Half-open (GreaterThanEqual/LessThan) avoids the double-counting that inclusive {@code Between}
   * would cause at the midnight boundary (BR-ST1, business-logic-model §3).
   */
  List<StudySession> findByStartedAtGreaterThanEqualAndStartedAtLessThan(
      Instant start, Instant end);

  /** The 10 most recent sessions, newest first — DB-side sort + limit (BR-ST4, PD-D-U4-2). */
  List<StudySession> findTop10ByOrderByStartedAtDesc();
}
