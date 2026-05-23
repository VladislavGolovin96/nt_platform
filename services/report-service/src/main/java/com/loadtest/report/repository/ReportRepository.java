package com.loadtest.report.repository;

import com.loadtest.report.domain.Report;
import com.loadtest.report.domain.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    Optional<Report> findByExecutionIdAndUserId(UUID executionId, UUID userId);

    Optional<Report> findByExecutionId(UUID executionId);

    List<Report> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("""
            UPDATE Report r SET
              r.status = :status,
              r.pdfPath = :pdfPath,
              r.generatedAt = :generatedAt
            WHERE r.id = :id
            """)
    void updateReady(@Param("id") UUID id,
                     @Param("status") ReportStatus status,
                     @Param("pdfPath") String pdfPath,
                     @Param("generatedAt") Instant generatedAt);

    @Modifying
    @Query("UPDATE Report r SET r.status = :status WHERE r.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") ReportStatus status);
}
