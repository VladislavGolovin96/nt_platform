package com.loadtest.project.service;

import com.loadtest.common.kafka.event.ProjectBuiltEvent;
import com.loadtest.project.domain.Project;
import com.loadtest.project.domain.ProjectStatus;
import com.loadtest.project.kafka.producer.ProjectKafkaProducer;
import com.loadtest.project.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Runs the build pipeline asynchronously to avoid blocking the HTTP thread.
 * Kept in a separate bean so @Async proxy works (Spring can't proxy self-calls).
 */
@Service
public class BuildPipelineService {

    private static final Logger log = LoggerFactory.getLogger(BuildPipelineService.class);

    private final ProjectRepository projectRepository;
    private final GitService gitService;
    private final BuildService buildService;
    private final MinioService minioService;
    private final ProjectKafkaProducer kafkaProducer;

    public BuildPipelineService(ProjectRepository projectRepository,
                                GitService gitService,
                                BuildService buildService,
                                MinioService minioService,
                                ProjectKafkaProducer kafkaProducer) {
        this.projectRepository = projectRepository;
        this.gitService = gitService;
        this.buildService = buildService;
        this.minioService = minioService;
        this.kafkaProducer = kafkaProducer;
    }

    /**
     * Clones repo, builds, finds simulations, uploads to MinIO, updates DB, publishes Kafka.
     * Status flow: PENDING → CLONING → BUILDING → READY | FAILED
     */
    @Async("taskExecutor")
    public void run(Project project) {
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("project-" + project.getId());

            // CLONING
            setStatus(project.getId(), ProjectStatus.CLONING);
            gitService.cloneRepository(project.getGitUrl(), project.getBranch(), workDir);

            // BUILDING
            setStatus(project.getId(), ProjectStatus.BUILDING);
            BuildResult result = buildService.build(workDir, project.getBuildTool());
            if (!result.success()) {
                log.warn("Build failed for project {}", project.getId());
                setStatus(project.getId(), ProjectStatus.FAILED);
                return;
            }

            // FIND SIMULATIONS
            List<String> simulations = gitService.findSimulationClasses(workDir);
            log.info("Found {} simulation(s) in project {}", simulations.size(), project.getId());

            // UPLOAD TO MINIO
            String artifactPath = minioService.uploadArtifact(
                    project.getUserId().toString(),
                    project.getId().toString(),
                    result.classesDir());

            // UPDATE DB → READY
            markReady(project.getId(), artifactPath, simulations);

            // PUBLISH KAFKA
            kafkaProducer.publishProjectBuilt(new ProjectBuiltEvent(
                    project.getId().toString(),
                    project.getUserId().toString(),
                    artifactPath,
                    simulations));

        } catch (Exception e) {
            log.error("Build pipeline failed for project {}: {}", project.getId(), e.getMessage(), e);
            setStatus(project.getId(), ProjectStatus.FAILED);
        } finally {
            deleteDirectory(workDir);
        }
    }

    @Transactional
    public void setStatus(java.util.UUID projectId, ProjectStatus status) {
        projectRepository.updateStatus(projectId, status);
    }

    @Transactional
    public void markReady(java.util.UUID projectId, String artifactPath, List<String> simulations) {
        projectRepository.updateReady(
                projectId, ProjectStatus.READY, artifactPath, simulations, Instant.now());
    }

    private void deleteDirectory(Path dir) {
        if (dir == null || !Files.exists(dir)) return;
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });
        } catch (IOException e) {
            log.warn("Could not fully delete temp dir {}: {}", dir, e.getMessage());
        }
    }
}
