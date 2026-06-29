package com.wordbook.web.dto;

import com.wordbook.domain.UserSettings;
import java.time.Instant;

/**
 * Read projection of the single {@link UserSettings} row (U6, FR-9). Carries all seven settings
 * fields; {@code studyReminderTime} is nullable (serializes as {@code null} when unset). Maps to
 * the component-methods {@code SettingsView} name ({@code SettingsView ≡ SettingsResponse}). Holds
 * no internal identifiers or server config (SD-U6-4).
 */
public record SettingsResponse(
    int newCardsPerDay,
    int maxReviewsPerDay,
    boolean showPronunciation,
    boolean darkMode,
    boolean autoPlayAudio,
    boolean studyReminderEnabled,
    Instant studyReminderTime) {

  public static SettingsResponse from(UserSettings settings) {
    return new SettingsResponse(
        settings.getNewCardsPerDay(),
        settings.getMaxReviewsPerDay(),
        settings.isShowPronunciation(),
        settings.isDarkMode(),
        settings.isAutoPlayAudio(),
        settings.isStudyReminderEnabled(),
        settings.getStudyReminderTime());
  }
}
