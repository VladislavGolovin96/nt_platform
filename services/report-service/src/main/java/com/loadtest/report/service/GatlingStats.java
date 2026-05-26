package com.loadtest.report.service;

import java.util.List;

public record GatlingStats(
        int totalRequests,
        int okCount,
        int koCount,
        long minMs,
        long maxMs,
        long meanMs,
        long p50Ms,
        long p75Ms,
        long p95Ms,
        long p99Ms,
        double rps,
        List<RequestStat> perRequest
) {
    public static GatlingStats empty() {
        return new GatlingStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0, List.of());
    }

    public boolean hasData() {
        return totalRequests > 0;
    }

    public double errorRate() {
        return totalRequests > 0 ? (double) koCount / totalRequests : 0.0;
    }
}
