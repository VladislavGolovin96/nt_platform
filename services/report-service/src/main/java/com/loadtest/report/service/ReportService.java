package com.loadtest.report.service;

import com.loadtest.common.exception.ResourceNotFoundException;
import com.loadtest.common.kafka.event.ReportGeneratedEvent;
import com.loadtest.common.kafka.event.TestFinishedEvent;
import com.loadtest.report.client.GrafanaClient;
import com.loadtest.report.client.MetricPoint;
import com.loadtest.report.client.PrometheusClient;
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
    private final PrometheusClient prometheusClient;
    private final PdfGenerator pdfGenerator;
    private final MinioService minioService;
    private final ReportKafkaProducer kafkaProducer;

    public ReportService(ReportRepository reportRepository,
                         ExecutionRecordRepository executionRecordRepository,
                         GrafanaClient grafanaClient,
                         PrometheusClient prometheusClient,
                         PdfGenerator pdfGenerator,
                         MinioService minioService,
                         ReportKafkaProducer kafkaProducer) {
        this.reportRepository = reportRepository;
        this.executionRecordRepository = executionRecordRepository;
        this.grafanaClient = grafanaClient;
        this.prometheusClient = prometheusClient;
        this.pdfGenerator = pdfGenerator;
        this.minioService = minioService;
        this.kafkaProducer = kafkaProducer;
    }

    @Async("taskExecutor")
    public void generateReport(TestFinishedEvent event) {
        UUID executionId = UUID.fromString(event.executionId());

        ExecutionRecord execution = executionRecordRepository.findById(executionId)
                .orElse(null);
        if (execution == null) {
            log.error("Execution {} not found in DB — cannot generate report", executionId);
            return;
        }

        Report report = createReport(executionId, execution.getUserId());

        try {
            log.info("Generating report for execution {}", executionId);

            Instant from = execution.getStartedAt() != null ? execution.getStartedAt() : Instant.now().minusSeconds(300);
            Instant to   = execution.getFinishedAt() != null ? execution.getFinishedAt() : Instant.now();

            List<MetricPoint> p50   = queryLatencyPercentile(from, to, "0.5");
            List<MetricPoint> p95   = queryLatencyPercentile(from, to, "0.95");
            List<MetricPoint> p99   = queryLatencyPercentile(from, to, "0.99");
            List<MetricPoint> tput  = prometheusClient.queryRange(
                    "rate(http_server_requests_seconds_count[1m])", from, to, "15s");
            List<MetricPoint> errRate = prometheusClient.queryRange(
                    "rate(http_server_requests_seconds_count{status=~\"5..\"}[1m])"
                            + " / rate(http_server_requests_seconds_count[1m])",
                    from, to, "15s");

            List<byte[]> panels = List.of(
                    grafanaClient.renderPanel(1, from, to),
                    grafanaClient.renderPanel(2, from, to)
            );

            ExecutionReport execReport = ExecutionReport.from(
                    execution, p50, p95, p99, tput, errRate, panels);

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

    private List<MetricPoint> queryLatencyPercentile(Instant from, Instant to, String quantile) {
        String query = String.format(
                "histogram_quantile(%s, rate(http_server_requests_seconds_bucket[1m])) * 1000",
                quantile);
        return prometheusClient.queryRange(query, from, to, "15s");
    }
}
