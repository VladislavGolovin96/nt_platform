package com.loadtest.execution.runner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GatlingRunnerTest {

    private GatlingRunner runner;

    @BeforeEach
    void setUp() {
        runner = new GatlingRunner(mock(com.loadtest.execution.repository.ExecutionLogRepository.class),
                mock(org.springframework.kafka.core.KafkaTemplate.class));
        ReflectionTestUtils.setField(runner, "gatlingHome", "/opt/gatling");
        ReflectionTestUtils.setField(runner, "resultsDir", "/tmp/gatling-results");
    }

    @Test
    void buildCommand_containsJavaExecutable() {
        List<String> cmd = runner.buildCommand("/tmp/classes", "com.example.MySimulation", "exec-123");
        assertEquals("java", cmd.get(0));
    }

    @Test
    void buildCommand_classpathIncludesArtifactAndGatlingLib() {
        List<String> cmd = runner.buildCommand("/tmp/classes", "com.example.MySimulation", "exec-123");
        int cpIndex = cmd.indexOf("-cp");
        assertTrue(cpIndex >= 0, "-cp flag must be present");
        String classpath = cmd.get(cpIndex + 1);
        assertTrue(classpath.contains("/tmp/classes"), "Classpath must include artifact classes dir");
        assertTrue(classpath.contains("/opt/gatling/lib/*"), "Classpath must include Gatling lib dir");
    }

    @Test
    void buildCommand_hasGatlingMainClass() {
        List<String> cmd = runner.buildCommand("/tmp/classes", "com.example.MySimulation", "exec-123");
        assertTrue(cmd.contains("io.gatling.app.Gatling"), "Command must include Gatling main class");
    }

    @Test
    void buildCommand_hasSimulationClass() {
        List<String> cmd = runner.buildCommand("/tmp/classes", "com.example.MySimulation", "exec-123");
        int sIndex = cmd.indexOf("-s");
        assertTrue(sIndex >= 0, "-s flag must be present");
        assertEquals("com.example.MySimulation", cmd.get(sIndex + 1));
    }

    @Test
    void buildCommand_resultsPathContainsExecutionId() {
        List<String> cmd = runner.buildCommand("/tmp/classes", "com.example.MySimulation", "exec-123");
        int rfIndex = cmd.indexOf("-rf");
        assertTrue(rfIndex >= 0, "-rf flag must be present");
        String resultPath = cmd.get(rfIndex + 1);
        assertTrue(resultPath.contains("exec-123"), "Results path must contain executionId");
        assertTrue(resultPath.startsWith("/tmp/gatling-results"), "Results path must start with configured dir");
    }
}
