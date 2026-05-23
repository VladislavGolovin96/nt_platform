package com.loadtest.project.service;

import com.loadtest.project.domain.BuildTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class BuildService {

    private static final Logger log = LoggerFactory.getLogger(BuildService.class);

    public BuildResult build(Path projectDir, BuildTool buildTool) {
        List<String> command = buildCommand(buildTool);
        log.info("Building project at {} with {}: {}", projectDir, buildTool, command);

        ProcessBuilder pb = new ProcessBuilder(command)
                .directory(projectDir.toFile())
                .redirectErrorStream(true);

        StringBuilder output = new StringBuilder();
        int exitCode;

        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }
            exitCode = process.waitFor();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Build process failed: {}", e.getMessage());
            return new BuildResult(false, e.getMessage(), null);
        }

        boolean success = exitCode == 0;
        Path classesDir = success ? resolveClassesDir(projectDir, buildTool) : null;

        if (success) {
            log.info("Build succeeded. Classes dir: {}", classesDir);
        } else {
            log.warn("Build failed (exit={}). Output:\n{}", exitCode, output);
        }

        return new BuildResult(success, output.toString(), classesDir);
    }

    private List<String> buildCommand(BuildTool buildTool) {
        return switch (buildTool) {
            case MAVEN -> List.of("mvn", "test-compile", "-f", "pom.xml", "-q");
            case GRADLE -> List.of("./gradlew", "testClasses", "-q");
        };
    }

    private Path resolveClassesDir(Path projectDir, BuildTool buildTool) {
        Path classesDir = switch (buildTool) {
            case MAVEN -> projectDir.resolve("target/test-classes");
            case GRADLE -> projectDir.resolve("build/classes/java/test");
        };
        return Files.exists(classesDir) ? classesDir : null;
    }
}
