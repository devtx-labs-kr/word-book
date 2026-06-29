package com.wordbook.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.wordbook.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * REST slice (MockMvc) for {@code /api/settings}: GET returns defaults (creating the row), PUT
 * persists a valid full body, and the partial-body / out-of-range defenses (SD-U6-1/SD-U6-2)
 * surface as 400 via the existing GlobalExceptionHandler.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SettingsApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserSettingsRepository userSettingsRepository;

  @BeforeEach
  void resetSettings() {
    userSettingsRepository.deleteAll();
  }

  @Test
  void getReturnsDefaultsAndCreatesTheRow() throws Exception {
    mockMvc
        .perform(get("/api/settings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.newCardsPerDay").value(20))
        .andExpect(jsonPath("$.maxReviewsPerDay").value(200))
        .andExpect(jsonPath("$.showPronunciation").value(true))
        .andExpect(jsonPath("$.darkMode").value(false));
  }

  @Test
  void putWithValidFullBodyReturns200AndPersists() throws Exception {
    String body =
        "{\"newCardsPerDay\":10,\"maxReviewsPerDay\":150,\"showPronunciation\":false,"
            + "\"darkMode\":true,\"autoPlayAudio\":false,\"studyReminderEnabled\":false,"
            + "\"studyReminderTime\":null}";

    mockMvc
        .perform(put("/api/settings").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.newCardsPerDay").value(10))
        .andExpect(jsonPath("$.darkMode").value(true));

    // Re-read confirms persistence to the single row.
    mockMvc
        .perform(get("/api/settings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.newCardsPerDay").value(10))
        .andExpect(jsonPath("$.darkMode").value(true));
  }

  @Test
  void putWithOutOfRangeValueReturns400() throws Exception {
    String body =
        "{\"newCardsPerDay\":-5,\"maxReviewsPerDay\":150,\"showPronunciation\":true,"
            + "\"darkMode\":false,\"autoPlayAudio\":false,\"studyReminderEnabled\":false,"
            + "\"studyReminderTime\":null}";

    mockMvc
        .perform(put("/api/settings").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void putWithMissingRequiredFieldReturns400() throws Exception {
    // newCardsPerDay omitted entirely — wrapper + @NotNull surfaces the missing key as 400
    // (SD-U6-2: a primitive would silently coerce to 0 and overwrite the row).
    String body =
        "{\"maxReviewsPerDay\":150,\"showPronunciation\":true,\"darkMode\":false,"
            + "\"autoPlayAudio\":false,\"studyReminderEnabled\":false,\"studyReminderTime\":null}";

    mockMvc
        .perform(put("/api/settings").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }
}
