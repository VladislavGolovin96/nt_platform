package com.loadtest.execution.repository;

import com.loadtest.execution.domain.ExecutionLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ExecutionLogRepository extends MongoRepository<ExecutionLog, String> {

    List<ExecutionLog> findByExecutionIdOrderByLineNumberAsc(String executionId);
}
