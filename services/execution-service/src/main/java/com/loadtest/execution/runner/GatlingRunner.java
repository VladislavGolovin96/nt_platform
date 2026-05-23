package com.loadtest.execution.runner;

import com.loadtest.execution.domain.ExecutionLog;
import com.loadtest.execution.repository.ExecutionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Component
public class GatlingRunner {

    private static final Logger log = LoggerFactory.getLogger(GatlingRunner.class);

    @Value("${gatling.home}")
    private String gatlingHome;

    @Value("${gatling.results-dir}")
    private String resultsDir;

    private final ExecutionLogRepository executionLogRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public GatlingRunner(ExecutionLogRepository executionLogRepository,
                         KafkaTemplate<String, Object> kafkaTemplate) {
        this.executionLogRepository = executionLogRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public List<String> buildCommand(String artifactClassesDir, String simulationClass, String executionId) {
        String classpath = artifactClassesDir + ":" + gatlingHome + "/lib/*";
        String resultPath = resultsDir + "/" + executionId;

        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-cp");
        command.add(classpath);
        command.add("io.gatling.app.Gatling");
        command.add("-s");
        command.add(simulationClass);
        command.add("-rf");
        command.add(resultPath);
        return command;
    }

    public int run(String artifactClassesDir, String simulationClass, String executionId,
                   Consumer<String> lineCallback) throws IOException, InterruptedException {
        List<String> command = buildCommand(artifactClassesDir, simulationClass, executionId);
        log.info("Launching Gatling for execution {}: {}", executionId, String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        streamOutput(process, executionId, lineCallback);

        int exitCode = process.waitFor();
        log.info("Gatling process exited with code {} for execution {}", exitCode, executionId);
        return exitCode;
    }

    private void streamOutput(Process process, String executionId, Consumer<String> lineCallback) {
        AtomicInteger lineNumber = new AtomicInteger(0);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int lineNum = lineNumber.incrementAndGet();
                String level = detectLevel(line);

                ExecutionLog logEntry = new ExecutionLog(executionId, lineNum, Instant.now(), level, line);
                executionLogRepository.save(logEntry);

                String topic = "test.log." + executionId;
                kafkaTemplate.send(topic, String.valueOf(lineNum), logEntry);

                if (lineCallback != null) {
                    lineCallback.accept(line);
                }
            }
        } catch (IOException e) {
            log.warn("Stream reading interrupted for execution {}: {}", executionId, e.getMessage());
        }
    }

    private String detectLevel(String line) {
        String upper = line.toUpperCase();
        if (upper.contains("ERROR")) return "ERROR";
        if (upper.contains("WARN")) return "WARN";
        return "INFO";
    }

    public String getResultPath(String executionId) {
        return resultsDir + "/" + executionId;
    }
}
