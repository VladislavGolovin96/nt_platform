package com.loadtest.project.dto;

import com.loadtest.project.domain.BuildTool;
import com.loadtest.project.domain.Project;
import com.loadtest.project.domain.ProjectStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID userId,
        String name,
        String gitUrl,
        String branch,
        BuildTool buildTool,
        ProjectStatus status,
        String artifactPath,
        List<String> simulations,
        Instant lastSyncedAt,
        Instant createdAt
) {
    public static ProjectResponse from(Project p) {
        return new ProjectResponse(
                p.getId(), p.getUserId(), p.getName(), p.getGitUrl(),
                p.getBranch(), p.getBuildTool(), p.getStatus(),
                p.getArtifactPath(), p.getSimulations(),
                p.getLastSyncedAt(), p.getCreatedAt()
        );
    }
}
