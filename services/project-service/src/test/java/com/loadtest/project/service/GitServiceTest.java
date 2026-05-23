package com.loadtest.project.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitServiceTest {

    private final GitService gitService = new GitService();

    @Test
    void findSimulationClasses_detectsExtendsSimulation(@TempDir Path tempDir) throws Exception {
        Path srcDir = tempDir.resolve("src/test/scala/example");
        Files.createDirectories(srcDir);

        Path javaFile = srcDir.resolve("BasicSimulation.java");
        Files.writeString(javaFile, """
                package example;
                import io.gatling.core.scenario.Simulation;
                public class BasicSimulation extends Simulation {
                }
                """);

        List<String> result = gitService.findSimulationClasses(tempDir);

        assertEquals(1, result.size());
        assertEquals("example.BasicSimulation", result.get(0));
    }

    @Test
    void findSimulationClasses_ignoresNonSimulationClasses(@TempDir Path tempDir) throws Exception {
        Path srcDir = tempDir.resolve("src/main/java/example");
        Files.createDirectories(srcDir);

        Files.writeString(srcDir.resolve("Helper.java"), """
                package example;
                public class Helper {}
                """);

        List<String> result = gitService.findSimulationClasses(tempDir);

        assertTrue(result.isEmpty());
    }

    @Test
    void findSimulationClasses_returnsEmptyWhenNoSrcDir(@TempDir Path tempDir) {
        List<String> result = gitService.findSimulationClasses(tempDir);
        assertTrue(result.isEmpty());
    }
}
