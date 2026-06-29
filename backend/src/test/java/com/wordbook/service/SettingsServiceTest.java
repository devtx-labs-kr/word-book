package com.wordbook.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.wordbook.domain.UserSettings;
import com.wordbook.repository.UserSettingsRepository;
import com.wordbook.web.dto.SettingsResponse;
import com.wordbook.web.dto.UpdateSettingsRequest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Service-level tests for the single-row settings get-or-create + in-place update (U6, BR-S5/S10).
 */
@SpringBootTest
class SettingsServiceTest {

  @Autowired private SettingsService settingsService;
  @Autowired private UserSettingsRepository userSettingsRepository;

  @BeforeEach
  void resetSettings() {
    // The settings table is a single shared row; start each test from a clean slate.
    userSettingsRepository.deleteAll();
  }

  @Test
  void getCreatesDefaultRowWhenNoneExists() {
    assertThat(userSettingsRepository.findAll()).isEmpty();

    SettingsResponse response = settingsService.get();

    // Defaults from the UserSettings entity (20/200/showPronunciation=on/light).
    assertThat(response.newCardsPerDay()).isEqualTo(20);
    assertThat(response.maxReviewsPerDay()).isEqualTo(200);
    assertThat(response.showPronunciation()).isTrue();
    assertThat(response.darkMode()).isFalse();
    // The default row was created and persisted (lifecycle INV-3).
    assertThat(userSettingsRepository.findAll()).hasSize(1);
  }

  @Test
  void updateMutatesSingleRowInPlaceWithoutCreatingDuplicates() {
    // Seed the single row via get-or-create and capture its id.
    settingsService.get();
    UUID rowId = userSettingsRepository.findAll().get(0).getId();

    UpdateSettingsRequest request = request(10, 150, false, true);
    SettingsResponse response = settingsService.update(request);

    assertThat(response.newCardsPerDay()).isEqualTo(10);
    assertThat(response.maxReviewsPerDay()).isEqualTo(150);
    assertThat(response.showPronunciation()).isFalse();
    assertThat(response.darkMode()).isTrue();

    // Still exactly one row, and it is the same row (in-place, BR-S5).
    assertThat(userSettingsRepository.findAll()).hasSize(1);
    assertThat(userSettingsRepository.findAll().get(0).getId()).isEqualTo(rowId);
  }

  @Test
  void updateCreatesRowWhenNoneExistsThenPersists() {
    assertThat(userSettingsRepository.findAll()).isEmpty();

    settingsService.update(request(5, 50, true, false));

    assertThat(userSettingsRepository.findAll()).hasSize(1);
    assertThat(settingsService.get().newCardsPerDay()).isEqualTo(5);
  }

  @Test
  void updatePreservesPassThroughFields() {
    Instant reminder = Instant.parse("2026-01-01T09:00:00Z");
    UpdateSettingsRequest request = request(20, 200, true, false);
    request.setAutoPlayAudio(true);
    request.setStudyReminderEnabled(true);
    request.setStudyReminderTime(reminder);

    SettingsResponse response = settingsService.update(request);

    // Pass-through fields (BR-S10) are stored and returned, not dropped.
    assertThat(response.autoPlayAudio()).isTrue();
    assertThat(response.studyReminderEnabled()).isTrue();
    assertThat(response.studyReminderTime()).isEqualTo(reminder);

    UserSettings persisted = userSettingsRepository.findAll().get(0);
    assertThat(persisted.isAutoPlayAudio()).isTrue();
    assertThat(persisted.isStudyReminderEnabled()).isTrue();
    assertThat(persisted.getStudyReminderTime()).isEqualTo(reminder);
  }

  private UpdateSettingsRequest request(
      int newCards, int maxReviews, boolean showPronunciation, boolean darkMode) {
    UpdateSettingsRequest request = new UpdateSettingsRequest();
    request.setNewCardsPerDay(newCards);
    request.setMaxReviewsPerDay(maxReviews);
    request.setShowPronunciation(showPronunciation);
    request.setDarkMode(darkMode);
    request.setAutoPlayAudio(false);
    request.setStudyReminderEnabled(false);
    request.setStudyReminderTime(null);
    return request;
  }
}
