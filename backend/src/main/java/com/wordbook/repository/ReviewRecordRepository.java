package com.wordbook.repository;

import com.wordbook.domain.ReviewRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link ReviewRecord}. */
public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, UUID> {}
