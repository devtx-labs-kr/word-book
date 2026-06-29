package com.wordbook.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Global user settings — a single row (DR-4). Ported from the Swift {@code UserSettings}.
 *
 * <p>In U1 only the defaults path is exercised; full settings wiring lands in U6. {@code
 * autoPlayAudio} / {@code studyReminder*} are stored as values only.
 */
@Entity
@Table(name = "user_settings")
public class UserSettings {

  @Id private UUID id = UUID.randomUUID();

  private int newCardsPerDay = 20;
  private int maxReviewsPerDay = 200;
  private boolean showPronunciation = true;
  private boolean darkMode = false;
  private boolean autoPlayAudio = false;
  private boolean studyReminderEnabled = false;
  private Instant studyReminderTime;

  public UserSettings() {}

  public UUID getId() {
    return id;
  }

  public int getNewCardsPerDay() {
    return newCardsPerDay;
  }

  public void setNewCardsPerDay(int newCardsPerDay) {
    this.newCardsPerDay = newCardsPerDay;
  }

  public int getMaxReviewsPerDay() {
    return maxReviewsPerDay;
  }

  public void setMaxReviewsPerDay(int maxReviewsPerDay) {
    this.maxReviewsPerDay = maxReviewsPerDay;
  }

  public boolean isShowPronunciation() {
    return showPronunciation;
  }

  public void setShowPronunciation(boolean showPronunciation) {
    this.showPronunciation = showPronunciation;
  }

  public boolean isDarkMode() {
    return darkMode;
  }

  public void setDarkMode(boolean darkMode) {
    this.darkMode = darkMode;
  }

  public boolean isAutoPlayAudio() {
    return autoPlayAudio;
  }

  public void setAutoPlayAudio(boolean autoPlayAudio) {
    this.autoPlayAudio = autoPlayAudio;
  }

  public boolean isStudyReminderEnabled() {
    return studyReminderEnabled;
  }

  public void setStudyReminderEnabled(boolean studyReminderEnabled) {
    this.studyReminderEnabled = studyReminderEnabled;
  }

  public Instant getStudyReminderTime() {
    return studyReminderTime;
  }

  public void setStudyReminderTime(Instant studyReminderTime) {
    this.studyReminderTime = studyReminderTime;
  }
}
