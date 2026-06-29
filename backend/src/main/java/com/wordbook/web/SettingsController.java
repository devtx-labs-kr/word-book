package com.wordbook.web;

import com.wordbook.service.SettingsService;
import com.wordbook.web.dto.SettingsResponse;
import com.wordbook.web.dto.UpdateSettingsRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for the single settings row (U6, FR-9). {@code GET} returns the current settings
 * (creating defaults on first access); {@code PUT} replaces them with a full body. Validation
 * violations on the {@code @Valid} body surface as 400 via the existing {@code
 * GlobalExceptionHandler} — no new handler.
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

  private final SettingsService settingsService;

  public SettingsController(SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  @GetMapping
  public SettingsResponse get() {
    return settingsService.get();
  }

  @PutMapping
  public SettingsResponse update(@Valid @RequestBody UpdateSettingsRequest request) {
    return settingsService.update(request);
  }
}
