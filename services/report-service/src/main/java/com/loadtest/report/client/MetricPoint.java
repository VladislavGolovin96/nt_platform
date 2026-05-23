package com.loadtest.report.client;

import java.time.Instant;

public record MetricPoint(Instant timestamp, double value) {}
