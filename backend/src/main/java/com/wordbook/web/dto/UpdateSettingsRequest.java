package com.wordbook.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Full-body settings update (U6, BR-S4 — partial patches are not supported; the client merges the
 * current settings with its change and PUTs the whole object).
 *
 * <p>The numeric/flag fields are <strong>wrapper types ({@code Integer}/{@code Boolean}) with
 * {@code @NotNull}</strong> rather than primitives (SD-U6-2). Primitives would let a missing JSON
 * key deserialize silently to {@code 0}/{@code false} and quietly overwrite the single settings
 * row; wrappers + {@code @NotNull} surface both a missing key and an explicit {@code null} as a
 * {@code MethodArgumentNotValidException} → 400 via the existing {@code GlobalExceptionHandler}.
 * {@code @Min}/{@code @Max} pass {@code null}, so {@code @NotNull} is required alongside them.
 */
public class UpdateSettingsRequest {

  @NotNull(message = "newCardsPerDay is required")
  @Min(value = 0, message = "newCardsPerDay must be at least 0")
  @Max(value = 1000, message = "newCardsPerDay must be at most 1000")
  private Integer newCardsPerDay;

  @NotNull(message = "maxReviewsPerDay is required")
  @Min(value = 0, message = "maxReviewsPerDay must be at least 0")
  @Max(value = 10000, message = "maxReviewsPerDay must be at most 10000")
  private Integer maxReviewsPerDay;

  @NotNull(message = "showPronunciation is required")
  private Boolean showPronunciation;

  @NotNull(message = "darkMode is required")
  private Boolean darkMode;

  @NotNull(message = "autoPlayAudio is required")
  private Boolean autoPlayAudio;

  @NotNull(message = "studyReminderEnabled is required")
  private Boolean studyReminderEnabled;

  /** Pass-through only (BR-S10); nullable, so no {@code @NotNull}. */
  private Instant studyReminderTime;

  public Integer getNewCardsPerDay() {
    return newCardsPerDay;
  }

  public void setNewCardsPerDay(Integer newCardsPerDay) {
    this.newCardsPerDay = newCardsPerDay;
  }

  public Integer getMaxReviewsPerDay() {
    return maxReviewsPerDay;
  }

  public void setMaxReviewsPerDay(Integer maxReviewsPerDay) {
    this.maxReviewsPerDay = maxReviewsPerDay;
  }

  public Boolean getShowPronunciation() {
    return showPronunciation;
  }

  public void setShowPronunciation(Boolean showPronunciation) {
    this.showPronunciation = showPronunciation;
  }

  public Boolean getDarkMode() {
    return darkMode;
  }

  public void setDarkMode(Boolean darkMode) {
    this.darkMode = darkMode;
  }

  public Boolean getAutoPlayAudio() {
    return autoPlayAudio;
  }

  public void setAutoPlayAudio(Boolean autoPlayAudio) {
    this.autoPlayAudio = autoPlayAudio;
  }

  public Boolean getStudyReminderEnabled() {
    return studyReminderEnabled;
  }

  public void setStudyReminderEnabled(Boolean studyReminderEnabled) {
    this.studyReminderEnabled = studyReminderEnabled;
  }

  public Instant getStudyReminderTime() {
    return studyReminderTime;
  }

  public void setStudyReminderTime(Instant studyReminderTime) {
    this.studyReminderTime = studyReminderTime;
  }
}
