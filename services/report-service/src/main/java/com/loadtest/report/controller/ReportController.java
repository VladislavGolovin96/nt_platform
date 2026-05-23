package com.loadtest.report.controller;

import com.loadtest.report.domain.Report;
import com.loadtest.report.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public List<Report> listReports(
            @RequestHeader("X-User-Id") String userId) {
        return reportService.listReports(UUID.fromString(userId));
    }

    @GetMapping("/{executionId}")
    public Report getReport(
            @PathVariable UUID executionId,
            @RequestHeader("X-User-Id") String userId) {
        return reportService.getReportByExecutionId(executionId, UUID.fromString(userId));
    }

    @GetMapping("/{executionId}/download")
    public ResponseEntity<Void> downloadReport(
            @PathVariable UUID executionId,
            @RequestHeader("X-User-Id") String userId) {
        String presignedUrl = reportService.getDownloadUrl(executionId, UUID.fromString(userId));
        return ResponseEntity.status(302)
                .location(URI.create(presignedUrl))
                .build();
    }
}
