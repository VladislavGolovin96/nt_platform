package com.loadtest.report.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parses Gatling simulation.log to extract performance metrics.
 *
 * Gatling 3.x simulation.log format (tab-separated):
 *   RUN    <simClass>  <simId>  <start>  <description>  <version>
 *   USER   <scenario>  <userId>  START|END  <startTs>  <endTs>
 *   REQUEST  <scenario>  <name>  <startTs>  <endTs>  OK|KO  [message]
 */
@Component
public class GatlingLogParser {

    private static final Logger log = LoggerFactory.getLogger(GatlingLogParser.class);

    /**
     * @param resultBasePath path like /tmp/gatling-results/{executionId}
     * @return parsed stats, or empty stats if file not found / unreadable
     */
    public GatlingStats parse(String resultBasePath) {
        Path baseDir = Path.of(resultBasePath);
        if (!Files.exists(baseDir)) {
            log.warn("Gatling results directory not found: {}", resultBasePath);
            return GatlingStats.empty();
        }

        Optional<Path> simLog = findSimulationLog(baseDir);
        if (simLog.isEmpty()) {
            log.warn("simulation.log not found under {}", resultBasePath);
            return GatlingStats.empty();
        }

        return parseLog(simLog.get());
    }

    private Optional<Path> findSimulationLog(Path baseDir) {
        try (var stream = Files.list(baseDir)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(d -> d.resolve("simulation.log"))
                    .filter(Files::exists)
                    .findFirst();
        } catch (IOException e) {
            log.warn("Could not list gatling results directory: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private GatlingStats parseLog(Path logFile) {
        List<Long> allTimes = new ArrayList<>();
        Map<String, RequestAccumulator> byRequest = new LinkedHashMap<>();
        long runStart = 0;
        long runEnd = 0;

        try (BufferedReader reader = Files.newBufferedReader(logFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length < 2) continue;

                switch (parts[0]) {
                    case "RUN" -> {
                        if (parts.length > 3) {
                            try { runStart = Long.parseLong(parts[3].trim()); } catch (NumberFormatException ignored) {}
                        }
                    }
                    case "REQUEST" -> {
                        if (parts.length >= 6) {
                            try {
                                String name  = parts[2].trim().isBlank() ? "request" : parts[2].trim();
                                long start   = Long.parseLong(parts[3].trim());
                                long end     = Long.parseLong(parts[4].trim());
                                boolean ok   = "OK".equalsIgnoreCase(parts[5].trim());
                                long duration = end - start;

                                if (duration >= 0) {
                                    allTimes.add(duration);
                                    byRequest.computeIfAbsent(name, k -> new RequestAccumulator()).add(duration, ok);
                                    if (end > runEnd) runEnd = end;
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    default -> {}
                }
            }
        } catch (IOException e) {
            log.error("Failed to read simulation.log {}: {}", logFile, e.getMessage());
            return GatlingStats.empty();
        }

        if (allTimes.isEmpty()) {
            return GatlingStats.empty();
        }

        Collections.sort(allTimes);
        long totalDuration = (runEnd > runStart) ? (runEnd - runStart) : 1;
        double rps = (allTimes.size() * 1000.0) / totalDuration;

        int okCount = byRequest.values().stream().mapToInt(a -> a.ok).sum();
        int koCount = byRequest.values().stream().mapToInt(a -> a.ko).sum();

        List<RequestStat> perRequest = byRequest.entrySet().stream()
                .map(e -> e.getValue().toStat(e.getKey()))
                .toList();

        return new GatlingStats(
                allTimes.size(),
                okCount,
                koCount,
                allTimes.get(0),
                allTimes.get(allTimes.size() - 1),
                (long) allTimes.stream().mapToLong(Long::longValue).average().orElse(0),
                percentile(allTimes, 50),
                percentile(allTimes, 75),
                percentile(allTimes, 95),
                percentile(allTimes, 99),
                rps,
                perRequest
        );
    }

    private long percentile(List<Long> sorted, int p) {
        if (sorted.isEmpty()) return 0;
        int idx = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(idx, sorted.size() - 1)));
    }

    // ── inner accumulator ────────────────────────────────────────────────────

    private static class RequestAccumulator {
        final List<Long> times = new ArrayList<>();
        int ok, ko;

        void add(long ms, boolean success) {
            times.add(ms);
            if (success) ok++; else ko++;
        }

        RequestStat toStat(String name) {
            List<Long> sorted = new ArrayList<>(times);
            Collections.sort(sorted);
            long mean = (long) sorted.stream().mapToLong(Long::longValue).average().orElse(0);
            long p95  = sorted.isEmpty() ? 0
                    : sorted.get((int) Math.max(0, Math.ceil(0.95 * sorted.size()) - 1));
            return new RequestStat(name, ok + ko, ok, ko,
                    sorted.isEmpty() ? 0 : sorted.get(0),
                    mean,
                    sorted.isEmpty() ? 0 : sorted.get(sorted.size() - 1),
                    p95);
        }
    }
}
