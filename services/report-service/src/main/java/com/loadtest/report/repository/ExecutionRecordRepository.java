package com.loadtest.report.repository;

import com.loadtest.report.domain.ExecutionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecord, UUID> {
}
