package com.loadtest.report.service;

import com.loadtest.common.exception.ResourceNotFoundException;
import com.loadtest.common.kafka.event.ReportGeneratedEvent;
import com.loadtest.common.kafka.event.TestFinishedEvent;
import com.loadtest.report.client.GrafanaClient;
import com.loadtest.report.domain.ExecutionRecord;
import com.loadtest.report.domain.Report;
import com.loadtest.report.domain.ReportStatus;
import com.loadtest.report.kafka.producer.ReportKafkaProducer;
import com.loadtest.report.repository.ExecutionRecordRepository;
import com.loadtest.report.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportRepository reportRepository;
    private final ExecutionRecordRepository executionRecordRepository;
    private final GrafanaClient grafanaClient;
    private final GatlingLogParser gatlingLogParser;
    private final PdfGenerator pdfGenerator;
    private final MinioService minioService;
    private final ReportKafkaProducer kafkaProducer;

    public ReportService(ReportRepository reportRepository,
                         ExecutionRecordRepository executionRecordRepository,
                         GrafanaClient grafanaClient,
                         GatlingLogParser gatlingLogParser,
                         PdfGenerator pdfGenerator,
                         MinioService minioService,
                         ReportKafkaProducer kafkaProducer) {
        this.reportRepository = reportRepository;
        this.executionRecordRepository = executionRecordRepository;
        this.grafanaClient = grafanaClient;
        this.gatlingLogParser = gatlingLogParser;
        this.pdfGenerator = pdfGenerator;
        this.minioService = minioService;
        this.kafkaProducer = kafkaProducer;
    }

    @Async("taskExecutor")
    public void generateReport(TestFinishedEvent event) {
        UUID executionId = UUID.fromString(event.executionId());

        ExecutionRecord execution = executionRecordRepository.findById(executionId).orElse(null);
        if (execution == null) {
            log.error("Execution {} not found in DB — cannot generate report", executionId);
            return;
        }

        Report report = createReport(executionId, execution.getUserId());

        try {
            log.info("Generating report for execution {}", executionId);

            // Parse real Gatling metrics from simulation.log
            GatlingStats stats = gatlingLogParser.parse(event.resultPath());
            if (!stats.hasData()) {
                log.warn("No Gatling stats found for execution {} at path {}",
                        executionId, event.resultPath());
            } else {
                log.info("Parsed Gatling stats: {} requests, p95={}ms, rps={:.2f}",
                        stats.totalRequests(), stats.p95Ms(), stats.rps());
            }

            // Try to fetch Grafana panels (best-effort, failures don't abort report)
            Instant from = execution.getStartedAt() != null ? execution.getStartedAt() : Instant.now().minusSeconds(300);
            Instant to   = execution.getFinishedAt() != null ? execution.getFinishedAt() : Instant.now();

            List<byte[]> panels = fetchGrafanaPanels(from, to);

            ExecutionReport execReport = ExecutionReport.from(execution, stats, panels);

            byte[] pdf = pdfGenerator.generate(execReport);
            String pdfPath = minioService.uploadReport(executionId.toString(), pdf);

            markReady(report.getId(), pdfPath, Instant.now());

            String downloadUrl = minioService.generatePresignedUrl(pdfPath);
            kafkaProducer.publishReportGenerated(new ReportGeneratedEvent(
                    executionId.toString(), pdfPath, downloadUrl));

            log.info("Report generated for execution {}: {}", executionId, pdfPath);

        } catch (Exception e) {
            log.error("Report generation failed for execution {}: {}", executionId, e.getMessage(), e);
            markFailed(report.getId());
        }
    }

    @Transactional
    public Report createReport(UUID executionId, UUID userId) {
        Report report = new Report();
        report.setExecutionId(executionId);
        report.setUserId(userId);
        report.setStatus(ReportStatus.GENERATING);
        return reportRepository.save(report);
    }

    @Transactional
    public void markReady(UUID reportId, String pdfPath, Instant generatedAt) {
        reportRepository.updateReady(reportId, ReportStatus.READY, pdfPath, generatedAt);
    }

    @Transactional
    public void markFailed(UUID reportId) {
        reportRepository.updateStatus(reportId, ReportStatus.FAILED);
    }

    @Transactional(readOnly = true)
    public Report getReportByExecutionId(UUID executionId, UUID userId) {
        return reportRepository.findByExecutionIdAndUserId(executionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", executionId.toString()));
    }

    @Transactional(readOnly = true)
    public List<Report> listReports(UUID userId) {
        return reportRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public String getDownloadUrl(UUID executionId, UUID userId) {
        Report report = getReportByExecutionId(executionId, userId);
        if (report.getPdfPath() == null) {
            throw new IllegalStateException("Report for execution " + executionId + " is not ready yet");
        }
        return minioService.generatePresignedUrl(report.getPdfPath());
    }

    private List<byte[]> fetchGrafanaPanels(Instant from, Instant to) {
        try {
            return List.of(
                    grafanaClient.renderPanel(1, from, to),
                    grafanaClient.renderPanel(2, from, to)
            );
        } catch (Exception e) {
            log.warn("Failed to fetch Grafana panels (skipped): {}", e.getMessage());
            return List.of();
        }
    }
}
