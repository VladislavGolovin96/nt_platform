package com.loadtest.report.service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.loadtest.report.client.MetricPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.OptionalDouble;

@Component
public class PdfGenerator {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss UTC").withZone(ZoneId.of("UTC"));

    @Value("${report.threshold.p95-ms}")
    private long thresholdP95Ms;

    @Value("${report.threshold.error-rate}")
    private double thresholdErrorRate;

    public byte[] generate(ExecutionReport report) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
             Document document = new Document(pdfDoc, PageSize.A4)) {

            document.setMargins(40, 40, 40, 40);
            addTitlePage(document, report);
            addMetricsSection(document, report);
            addGrafanaPanels(document, report);
            addConclusions(document, report);
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
        return baos.toByteArray();
    }

    private void addTitlePage(Document doc, ExecutionReport report) {
        doc.add(new Paragraph("Load Test Report")
                .setFontSize(28)
                .setBold()
                .setFontColor(new DeviceRgb(30, 80, 160))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        Table meta = new Table(UnitValue.createPercentArray(new float[]{35, 65}))
                .useAllAvailableWidth()
                .setMarginBottom(30);

        addMetaRow(meta, "Execution ID", report.executionId().toString());
        addMetaRow(meta, "Simulation", report.simulationClass());
        addMetaRow(meta, "Test Type", report.testType());
        addMetaRow(meta, "Target Host", report.targetHost() + " (" + report.targetMode() + ")");
        addMetaRow(meta, "Started At",
                report.startedAt() != null ? DATE_FMT.format(report.startedAt()) : "N/A");
        addMetaRow(meta, "Finished At",
                report.finishedAt() != null ? DATE_FMT.format(report.finishedAt()) : "N/A");
        addMetaRow(meta, "Duration", formatDuration(report.durationMs()));

        doc.add(meta);
    }

    private void addMetricsSection(Document doc, ExecutionReport report) {
        doc.add(new Paragraph("Performance Metrics")
                .setFontSize(18)
                .setBold()
                .setMarginBottom(10));

        Table table = new Table(UnitValue.createPercentArray(new float[]{40, 20, 20, 20}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Header
        for (String header : List.of("Metric", "Min", "Avg", "Max")) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(header).setBold())
                    .setBackgroundColor(new DeviceRgb(220, 230, 245)));
        }

        addMetricRow(table, "P50 Latency (ms)", report.p50Latency());
        addMetricRow(table, "P95 Latency (ms)", report.p95Latency());
        addMetricRow(table, "P99 Latency (ms)", report.p99Latency());
        addMetricRow(table, "Throughput (req/s)", report.throughput());
        addMetricRow(table, "Error Rate", report.errorRate());

        doc.add(table);
    }

    private void addGrafanaPanels(Document doc, ExecutionReport report) {
        List<byte[]> panels = report.grafanaPanels();
        if (panels == null || panels.stream().allMatch(p -> p == null || p.length == 0)) return;

        doc.add(new Paragraph("Performance Charts")
                .setFontSize(18)
                .setBold()
                .setMarginBottom(10));

        for (byte[] panel : panels) {
            if (panel == null || panel.length == 0) continue;
            try {
                Image img = new Image(ImageDataFactory.create(panel))
                        .setWidth(UnitValue.createPercentValue(100))
                        .setMarginBottom(15);
                doc.add(img);
            } catch (Exception e) {
                // skip unrenderable panel
            }
        }
    }

    private void addConclusions(Document doc, ExecutionReport report) {
        doc.add(new Paragraph("Conclusions")
                .setFontSize(18)
                .setBold()
                .setMarginBottom(10));

        double p95Avg = average(report.p95Latency());
        double errAvg = average(report.errorRate());

        boolean p95Ok = p95Avg <= 0 || p95Avg <= thresholdP95Ms;
        boolean errOk = errAvg <= 0 || errAvg <= thresholdErrorRate;
        boolean passed = p95Ok && errOk;

        String verdict = passed ? "PASS" : "FAIL";
        DeviceRgb verdictColor = passed
                ? new DeviceRgb(0, 150, 0)
                : new DeviceRgb(200, 0, 0);

        doc.add(new Paragraph("Overall Result: " + verdict)
                .setFontSize(16)
                .setBold()
                .setFontColor(verdictColor)
                .setMarginBottom(8));

        if (!p95Ok) {
            doc.add(new Paragraph(String.format(
                    "P95 latency %.0f ms exceeds threshold %d ms", p95Avg, thresholdP95Ms))
                    .setFontColor(ColorConstants.RED));
        }
        if (!errOk) {
            doc.add(new Paragraph(String.format(
                    "Error rate %.2f%% exceeds threshold %.2f%%", errAvg * 100, thresholdErrorRate * 100))
                    .setFontColor(ColorConstants.RED));
        }
        if (passed) {
            doc.add(new Paragraph("All thresholds met. Test passed successfully.")
                    .setFontColor(new DeviceRgb(0, 120, 0)));
        }
    }

    private void addMetaRow(Table table, String key, String value) {
        table.addCell(new Cell().add(new Paragraph(key).setBold()));
        table.addCell(new Cell().add(new Paragraph(value != null ? value : "N/A")));
    }

    private void addMetricRow(Table table, String name, List<MetricPoint> points) {
        table.addCell(new Cell().add(new Paragraph(name)));
        if (points == null || points.isEmpty()) {
            table.addCell(new Cell().add(new Paragraph("N/A")));
            table.addCell(new Cell().add(new Paragraph("N/A")));
            table.addCell(new Cell().add(new Paragraph("N/A")));
        } else {
            OptionalDouble min = points.stream().mapToDouble(MetricPoint::value).min();
            OptionalDouble avg = points.stream().mapToDouble(MetricPoint::value).average();
            OptionalDouble max = points.stream().mapToDouble(MetricPoint::value).max();
            table.addCell(new Cell().add(new Paragraph(fmt(min))));
            table.addCell(new Cell().add(new Paragraph(fmt(avg))));
            table.addCell(new Cell().add(new Paragraph(fmt(max))));
        }
    }

    private String fmt(OptionalDouble v) {
        return v.isPresent() ? String.format("%.2f", v.getAsDouble()) : "N/A";
    }

    private double average(List<MetricPoint> points) {
        if (points == null || points.isEmpty()) return 0;
        return points.stream().mapToDouble(MetricPoint::value).average().orElse(0);
    }

    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        return String.format("%dm %ds", seconds / 60, seconds % 60);
    }
}
