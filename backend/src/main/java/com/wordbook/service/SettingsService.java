package com.wordbook.service;

import com.wordbook.domain.UserSettings;
import com.wordbook.repository.UserSettingsRepository;
import com.wordbook.web.dto.SettingsResponse;
import com.wordbook.web.dto.UpdateSettingsRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Settings read/write over the single {@link UserSettings} row (U6, FR-9). Shares the same single
 * row as {@code StudySessionService.start} (which reads {@code newCardsPerDay}/{@code
 * maxReviewsPerDay} via {@code findAll().stream().findFirst()}), so an edit here takes effect on
 * the next study session (BR-S7).
 *
 * <p>Get-or-create keeps the table at exactly 0→1 rows (INV-1/INV-3, RD-U6-2): the first read
 * lazily creates a default row, and updates mutate that row in place — never a new row (BR-S5).
 * Writes are {@code @Transactional}; a save failure rolls back and propagates (no silent failure,
 * BR-S11).
 */
@Service
public class SettingsService {

  private final UserSettingsRepository userSettingsRepository;

  public SettingsService(UserSettingsRepository userSettingsRepository) {
    this.userSettingsRepository = userSettingsRepository;
  }

  /** Returns the settings, creating the default row on first access (get-or-create, RD-U6-2). */
  @Transactional
  public SettingsResponse get() {
    return SettingsResponse.from(getOrCreate());
  }

  /**
   * Applies a full-body update (BR-S4) to the single row in place and returns the saved state. All
   * seven fields are written, including the UI-hidden pass-through fields {@code autoPlayAudio} /
   * {@code studyReminder*} (BR-S10). Bean Validation on {@link UpdateSettingsRequest} has already
   * rejected missing/out-of-range fields with a 400 before this runs (SD-U6-1/SD-U6-2).
   */
  @Transactional
  public SettingsResponse update(UpdateSettingsRequest request) {
    UserSettings settings = getOrCreate();
    settings.setNewCardsPerDay(request.getNewCardsPerDay());
    settings.setMaxReviewsPerDay(request.getMaxReviewsPerDay());
    settings.setShowPronunciation(request.getShowPronunciation());
    settings.setDarkMode(request.getDarkMode());
    settings.setAutoPlayAudio(request.getAutoPlayAudio());
    settings.setStudyReminderEnabled(request.getStudyReminderEnabled());
    settings.setStudyReminderTime(request.getStudyReminderTime());
    return SettingsResponse.from(userSettingsRepository.save(settings));
  }

  /**
   * Returns the single settings row, creating and persisting a default {@link UserSettings} when
   * none exists. Mirrors the {@code findAll().stream().findFirst()} pattern used by {@code
   * StudySessionService} so both paths see the same row (RD-U6-2).
   */
  private UserSettings getOrCreate() {
    return userSettingsRepository.findAll().stream()
        .findFirst()
        .orElseGet(() -> userSettingsRepository.save(new UserSettings()));
  }
}
