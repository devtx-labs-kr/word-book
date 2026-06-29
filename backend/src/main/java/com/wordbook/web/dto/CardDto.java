package com.wordbook.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wordbook.domain.LearningState;
import java.time.Instant;
import java.util.UUID;

/**
 * Serialization view of a {@code Card} for export/import (U5). Ported 1:1 from the macOS {@code
 * CardDTO} — note there is NO {@code ReviewRecord} history (FP-4).
 *
 * <p>{@code learningState} is typed as the {@link LearningState} enum so its {@code @JsonValue}
 * (lowercase wire value) / {@code @JsonCreator} (unrecognized + null -&gt; NEW fallback, FB-1)
 * apply automatically and the value lines up with {@code Card.setLearningState(LearningState)}.
 *
 * <p>The nullable display/SRS fields ({@code example}, {@code pronunciation}, {@code notes}, {@code
 * lastReviewedAt}) are omitted from the export when null ({@code @JsonInclude(NON_NULL)}, CR-3),
 * matching Swift's behaviour of dropping {@code nil} optionals; on import a missing key or explicit
 * {@code null} are both accepted.
 *
 * <p>On import {@code id}/{@code createdAt}/{@code updatedAt} are ignored (regenerated, FP-1); the
 * SRS + statistics fields are preserved verbatim with NO SM-2 recalculation (FP-2).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CardDto(
    UUID id,
    String front,
    String back,
    @JsonInclude(JsonInclude.Include.NON_NULL) String example,
    @JsonInclude(JsonInclude.Include.NON_NULL) String pronunciation,
    @JsonInclude(JsonInclude.Include.NON_NULL) String notes,
    LearningState learningState,
    double easeFactor,
    int interval,
    int repetitions,
    Instant nextReviewDate,
    @JsonInclude(JsonInclude.Include.NON_NULL) Instant lastReviewedAt,
    int totalReviews,
    int correctReviews,
    Instant createdAt,
    Instant updatedAt) {}
