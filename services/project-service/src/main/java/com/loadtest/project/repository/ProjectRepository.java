package com.loadtest.project.repository;

import com.loadtest.project.domain.Project;
import com.loadtest.project.domain.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Project> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE Project p SET p.status = :status WHERE p.id = :id")
    void updateStatus(UUID id, ProjectStatus status);

    @Modifying
    @Transactional
    @Query("""
            UPDATE Project p
            SET p.status = :status,
                p.artifactPath = :artifactPath,
                p.simulations = :simulations,
                p.lastSyncedAt = :syncedAt
            WHERE p.id = :id
            """)
    void updateReady(UUID id, ProjectStatus status, String artifactPath,
                     List<String> simulations, Instant syncedAt);
}
