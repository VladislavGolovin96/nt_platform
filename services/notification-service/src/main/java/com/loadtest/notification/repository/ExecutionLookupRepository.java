package com.loadtest.notification.repository;

import com.loadtest.notification.domain.ExecutionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExecutionLookupRepository extends JpaRepository<ExecutionRecord, UUID> {
}
