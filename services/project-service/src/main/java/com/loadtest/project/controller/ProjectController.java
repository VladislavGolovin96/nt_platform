package com.loadtest.project.controller;

import com.loadtest.project.dto.CreateProjectRequest;
import com.loadtest.project.dto.ProjectResponse;
import com.loadtest.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ProjectResponse createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @RequestHeader("X-User-Id") String userId) {
        return projectService.createProject(request, UUID.fromString(userId));
    }

    @GetMapping
    public List<ProjectResponse> listProjects(
            @RequestHeader("X-User-Id") String userId) {
        return projectService.listProjects(UUID.fromString(userId));
    }

    @GetMapping("/{id}")
    public ProjectResponse getProject(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        return projectService.getProject(id, UUID.fromString(userId));
    }

    @PostMapping("/{id}/sync")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ProjectResponse syncProject(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        return projectService.syncProject(id, UUID.fromString(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        projectService.deleteProject(id, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/simulations")
    public List<String> getSimulations(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        return projectService.getSimulations(id, UUID.fromString(userId));
    }
}
