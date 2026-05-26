package com.loadtest.report.service;

public record RequestStat(
        String name,
        int count,
        int okCount,
        int koCount,
        long minMs,
        long meanMs,
        long maxMs,
        long p95Ms
) {}
