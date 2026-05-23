package com.loadtest.project.dto;

import com.loadtest.project.domain.BuildTool;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProjectRequest(
        @NotBlank String name,
        @NotBlank String gitUrl,
        String branch,
        @NotNull BuildTool buildTool
) {
    public String branch() {
        return (branch == null || branch.isBlank()) ? "main" : branch;
    }
}
