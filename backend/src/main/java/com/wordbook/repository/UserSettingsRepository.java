package com.wordbook.repository;

import com.wordbook.domain.UserSettings;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link UserSettings} (single-row settings, DR-4). */
public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID> {}
