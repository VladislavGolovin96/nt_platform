package com.loadtest.execution.repository;

import com.loadtest.execution.domain.Execution;
import com.loadtest.execution.domain.ExecutionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExecutionRepository extends JpaRepository<Execution, UUID> {

    List<Execution> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Execution> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("UPDATE Execution e SET e.status = :status WHERE e.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") ExecutionStatus status);

    @Modifying
    @Query("""
            UPDATE Execution e SET
              e.status = :status,
              e.startedAt = :startedAt
            WHERE e.id = :id
            """)
    void updateStarted(@Param("id") UUID id,
                       @Param("status") ExecutionStatus status,
                       @Param("startedAt") Instant startedAt);

    @Modifying
    @Query("""
            UPDATE Execution e SET
              e.status = :status,
              e.finishedAt = :finishedAt,
              e.durationMs = :durationMs,
              e.resultPath = :resultPath
            WHERE e.id = :id
            """)
    void updateFinished(@Param("id") UUID id,
                        @Param("status") ExecutionStatus status,
                        @Param("finishedAt") Instant finishedAt,
                        @Param("durationMs") Long durationMs,
                        @Param("resultPath") String resultPath);
}
