package com.loadtest.project.service;

import com.loadtest.common.exception.ResourceNotFoundException;
import com.loadtest.project.domain.Project;
import com.loadtest.project.domain.ProjectStatus;
import com.loadtest.project.dto.CreateProjectRequest;
import com.loadtest.project.dto.ProjectResponse;
import com.loadtest.project.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final BuildPipelineService buildPipelineService;

    public ProjectService(ProjectRepository projectRepository,
                          BuildPipelineService buildPipelineService) {
        this.projectRepository = projectRepository;
        this.buildPipelineService = buildPipelineService;
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, UUID userId) {
        Project project = new Project();
        project.setUserId(userId);
        project.setName(request.name());
        project.setGitUrl(request.gitUrl());
        project.setBranch(request.branch());
        project.setBuildTool(request.buildTool());
        project.setStatus(ProjectStatus.PENDING);

        project = projectRepository.save(project);
        log.info("Created project {} for user {}", project.getId(), userId);

        // Launch async pipeline — returns immediately
        buildPipelineService.run(project);

        return ProjectResponse.from(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects(UUID userId) {
        return projectRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId, UUID userId) {
        return projectRepository.findByIdAndUserId(projectId, userId)
                .map(ProjectResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));
    }

    @Transactional(readOnly = true)
    public List<String> getSimulations(UUID projectId, UUID userId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));
        List<String> sims = project.getSimulations();
        return sims != null ? sims : List.of();
    }

    @Transactional
    public ProjectResponse syncProject(UUID projectId, UUID userId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));

        project.setStatus(ProjectStatus.PENDING);
        project = projectRepository.save(project);
        log.info("Syncing project {}", projectId);

        // Re-run pipeline (fresh clone = equivalent to pull for stateless service)
        buildPipelineService.run(project);

        return ProjectResponse.from(project);
    }

    @Transactional
    public void deleteProject(UUID projectId, UUID userId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));
        projectRepository.delete(project);
        log.info("Deleted project {}", projectId);
    }
}
