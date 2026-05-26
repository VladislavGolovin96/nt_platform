package com.loadtest.report.service;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

@Component
public class PdfGenerator {

    // ── colours ──────────────────────────────────────────────────────────────
    private static final DeviceRgb BLUE_DARK   = new DeviceRgb(30,  80,  160);
    private static final DeviceRgb BLUE_LIGHT  = new DeviceRgb(220, 230, 245);
    private static final DeviceRgb GREEN       = new DeviceRgb(0,   140, 0);
    private static final DeviceRgb RED         = new DeviceRgb(200, 0,   0);
    private static final DeviceRgb ORANGE      = new DeviceRgb(200, 100, 0);
    private static final DeviceRgb GREY_LIGHT  = new DeviceRgb(245, 245, 245);
    private static final DeviceRgb GREY_BORDER = new DeviceRgb(200, 200, 200);
    private static final DeviceRgb WHITE       = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb BLACK       = new DeviceRgb(0,   0,   0);

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneId.of("UTC"));

    @Value("${report.threshold.p95-ms}")
    private long thresholdP95Ms;

    @Value("${report.threshold.error-rate}")
    private double thresholdErrorRate;

    // ── public API ────────────────────────────────────────────────────────────

    public byte[] generate(ExecutionReport report) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
             Document doc = new Document(pdfDoc, PageSize.A4)) {

            doc.setMargins(40, 40, 40, 40);

            addHeader(doc, report);
            addSummaryCards(doc, report);
            addLatencyTable(doc, report);
            addPerRequestTable(doc, report);
            addDistribution(doc, report);
            addGrafanaPanels(doc, report);
            addConclusions(doc, report);

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
        return baos.toByteArray();
    }

    // ── sections ──────────────────────────────────────────────────────────────

    /** Blue banner + metadata table */
    private void addHeader(Document doc, ExecutionReport r) {
        doc.add(new Paragraph("Load Test Report")
                .setFontSize(26).setBold()
                .setFontColor(BLUE_DARK)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4));

        doc.add(new Paragraph(r.simulationClass())
                .setFontSize(11)
                .setFontColor(new DeviceRgb(100, 100, 100))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));

        Table meta = new Table(UnitValue.createPercentArray(new float[]{38, 62}))
                .useAllAvailableWidth()
                .setMarginBottom(24);

        addMetaRow(meta, "Execution ID", r.executionId().toString());
        addMetaRow(meta, "Test Type",    r.testType());
        addMetaRow(meta, "Target",       r.targetHost() + "  (" + r.targetMode() + ")");
        addMetaRow(meta, "Started",      r.startedAt()  != null ? DATE_FMT.format(r.startedAt())  : "—");
        addMetaRow(meta, "Finished",     r.finishedAt() != null ? DATE_FMT.format(r.finishedAt()) : "—");
        addMetaRow(meta, "Duration",     formatDuration(r.durationMs()));

        doc.add(meta);
    }

    /** Three top-level KPI boxes: total requests / error rate / throughput */
    private void addSummaryCards(Document doc, ExecutionReport r) {
        GatlingStats s = r.gatlingStats();
        if (!s.hasData()) return;

        sectionTitle(doc, "Summary");

        Table cards = new Table(UnitValue.createPercentArray(new float[]{33, 34, 33}))
                .useAllAvailableWidth()
                .setMarginBottom(24);

        double errPct = s.errorRate() * 100;
        DeviceRgb errColor = s.errorRate() > thresholdErrorRate ? RED : GREEN;

        cards.addCell(kpiCard("Total Requests",
                s.totalRequests() + "  (✓" + s.okCount() + "  ✗" + s.koCount() + ")",
                s.koCount() > 0 ? RED : GREEN));
        cards.addCell(kpiCard("Error Rate",
                String.format("%.2f%%", errPct), errColor));
        cards.addCell(kpiCard("Throughput",
                String.format("%.2f req/s", s.rps()), BLUE_DARK));

        doc.add(cards);
    }

    /** Detailed latency table: min / mean / p50 / p75 / p95 / p99 / max */
    private void addLatencyTable(Document doc, ExecutionReport r) {
        GatlingStats s = r.gatlingStats();
        if (!s.hasData()) {
            sectionTitle(doc, "Performance Metrics");
            doc.add(new Paragraph("No Gatling data available — simulation.log not found.")
                    .setFontColor(new DeviceRgb(150, 150, 150))
                    .setMarginBottom(20));
            return;
        }

        sectionTitle(doc, "Response Time (ms)");

        Table t = new Table(UnitValue.createPercentArray(new float[]{20, 12, 12, 12, 12, 14, 12, 12}))
                .useAllAvailableWidth().setMarginBottom(24);

        String[] headers = {"Metric", "Min", "Mean", "P50", "P75", "P95 *", "P99", "Max"};
        for (String h : headers) {
            t.addHeaderCell(headerCell(h));
        }

        // Global row
        DeviceRgb p95Color = s.p95Ms() > thresholdP95Ms ? RED : BLACK;
        t.addCell(labelCell("Global"));
        t.addCell(numCell(s.minMs()));
        t.addCell(numCell(s.meanMs()));
        t.addCell(numCell(s.p50Ms()));
        t.addCell(numCell(s.p75Ms()));
        t.addCell(numCell(s.p95Ms(), p95Color));
        t.addCell(numCell(s.p99Ms()));
        t.addCell(numCell(s.maxMs()));

        doc.add(t);
        doc.add(new Paragraph("* P95 threshold: " + thresholdP95Ms + " ms")
                .setFontSize(8).setFontColor(new DeviceRgb(120, 120, 120))
                .setMarginBottom(20));
    }

    /** Per-request breakdown */
    private void addPerRequestTable(Document doc, ExecutionReport r) {
        GatlingStats s = r.gatlingStats();
        if (!s.hasData() || s.perRequest().isEmpty()) return;

        sectionTitle(doc, "Per-Request Breakdown");

        Table t = new Table(UnitValue.createPercentArray(new float[]{30, 10, 8, 12, 12, 12, 12, 12}))
                .useAllAvailableWidth().setMarginBottom(24);

        for (String h : List.of("Request", "Total", "KO", "Min", "Mean", "P95", "Max", "Status")) {
            t.addHeaderCell(headerCell(h));
        }

        boolean odd = true;
        for (RequestStat rs : s.perRequest()) {
            DeviceRgb bg = odd ? WHITE : GREY_LIGHT;
            boolean reqOk = rs.koCount() == 0 && rs.p95Ms() <= thresholdP95Ms;
            String status = reqOk ? "PASS" : "FAIL";
            DeviceRgb statusColor = reqOk ? GREEN : RED;

            t.addCell(dataCell(rs.name(), bg));
            t.addCell(dataCell(String.valueOf(rs.count()), bg));
            t.addCell(dataCell(rs.koCount() > 0 ? "✗ " + rs.koCount() : "—", bg,
                    rs.koCount() > 0 ? RED : null));
            t.addCell(dataCell(rs.minMs() + " ms", bg));
            t.addCell(dataCell(rs.meanMs() + " ms", bg));
            t.addCell(dataCell(rs.p95Ms() + " ms", bg,
                    rs.p95Ms() > thresholdP95Ms ? ORANGE : null));
            t.addCell(dataCell(rs.maxMs() + " ms", bg));
            t.addCell(dataCell(status, bg, statusColor));

            odd = !odd;
        }

        doc.add(t);
    }


    private void addDistribution(Document doc, ExecutionReport r) {
        GatlingStats s = r.gatlingStats();
        if (!s.hasData() || s.perRequest().isEmpty()) return;

        sectionTitle(doc, "Response Time Distribution");


        int total = s.totalRequests();
        long p50 = s.p50Ms(), p75 = s.p75Ms(), p95 = s.p95Ms();

        int under800  = (int) (total * pctBelow(p50, p75, p95, 800));
        int under1200 = (int) (total * pctBelow(p50, p75, p95, 1200)) - under800;
        int over1200  = total - under800 - under1200;
        under1200 = Math.max(0, under1200);
        over1200  = Math.max(0, over1200);

        Table t = new Table(UnitValue.createPercentArray(new float[]{40, 20, 20}))
                .useAllAvailableWidth().setMarginBottom(24);

        t.addHeaderCell(headerCell("Bucket"));
        t.addHeaderCell(headerCell("Count"));
        t.addHeaderCell(headerCell("Share"));

        addDistRow(t, "< 800 ms",          under800,  total);
        addDistRow(t, "800 ms – 1200 ms",  under1200, total);
        addDistRow(t, "≥ 1200 ms",         over1200,  total);
        addDistRow(t, "Failed",            s.koCount(), total);

        doc.add(t);
    }

    /** Grafana chart images */
    private void addGrafanaPanels(Document doc, ExecutionReport r) {
        List<byte[]> panels = r.grafanaPanels();
        if (panels == null || panels.stream().allMatch(p -> p == null || p.length == 0)) return;

        sectionTitle(doc, "Performance Charts");
        for (byte[] panel : panels) {
            if (panel == null || panel.length == 0) continue;
            try {
                doc.add(new Image(ImageDataFactory.create(panel))
                        .setWidth(UnitValue.createPercentValue(100))
                        .setMarginBottom(14));
            } catch (Exception ignored) {}
        }
    }

    /** Pass/Fail verdict + threshold details */
    private void addConclusions(Document doc, ExecutionReport r) {
        sectionTitle(doc, "Conclusions");

        GatlingStats s = r.gatlingStats();
        boolean p95Ok  = !s.hasData() || s.p95Ms() <= thresholdP95Ms;
        boolean errOk  = !s.hasData() || s.errorRate() <= thresholdErrorRate;
        boolean passed = p95Ok && errOk;

        doc.add(new Paragraph("Overall Result: " + (passed ? "PASS" : "FAIL"))
                .setFontSize(16).setBold()
                .setFontColor(passed ? GREEN : RED)
                .setMarginBottom(10));

        if (s.hasData()) {
            doc.add(new Paragraph(String.format(
                    "P95 latency: %d ms  (threshold: %d ms)  %s",
                    s.p95Ms(), thresholdP95Ms, p95Ok ? "✓" : "✗"))
                    .setFontColor(p95Ok ? GREEN : RED));
            doc.add(new Paragraph(String.format(
                    "Error rate: %.2f%%  (threshold: %.2f%%)  %s",
                    s.errorRate() * 100, thresholdErrorRate * 100, errOk ? "✓" : "✗"))
                    .setFontColor(errOk ? GREEN : RED));
            doc.add(new Paragraph(String.format(
                    "Total requests: %d  (OK: %d, KO: %d)",
                    s.totalRequests(), s.okCount(), s.koCount()))
                    .setFontColor(new DeviceRgb(60, 60, 60))
                    .setMarginTop(6));
        } else {
            doc.add(new Paragraph("Metrics not available — simulation.log was not found on this instance.")
                    .setFontColor(new DeviceRgb(150, 150, 150)));
        }

        if (passed && s.hasData()) {
            doc.add(new Paragraph("All thresholds met. Test passed successfully.")
                    .setFontColor(GREEN).setMarginTop(8));
        }
    }

    // ── cell factories ────────────────────────────────────────────────────────

    private void sectionTitle(Document doc, String text) {
        doc.add(new Paragraph(text)
                .setFontSize(14).setBold()
                .setFontColor(BLUE_DARK)
                .setMarginBottom(8)
                .setBorderBottom(new SolidBorder(BLUE_DARK, 1))
                .setPaddingBottom(4));
    }

    private Cell kpiCard(String label, String value, DeviceRgb valueColor) {
        Table inner = new Table(1).useAllAvailableWidth();
        inner.addCell(new Cell().add(new Paragraph(label)
                        .setFontSize(9).setFontColor(new DeviceRgb(100, 100, 100)))
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));
        inner.addCell(new Cell().add(new Paragraph(value)
                        .setFontSize(13).setBold().setFontColor(valueColor))
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));

        return new Cell().add(inner)
                .setPadding(12)
                .setBackgroundColor(GREY_LIGHT)
                .setBorder(new SolidBorder(GREY_BORDER, 1))
                .setTextAlignment(TextAlignment.CENTER);
    }

    private Cell headerCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontSize(9))
                .setBackgroundColor(BLUE_LIGHT)
                .setPadding(5)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private Cell labelCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold().setFontSize(9))
                .setPadding(5);
    }

    private Cell numCell(long value) {
        return numCell(value, null);
    }

    private Cell numCell(long value, DeviceRgb color) {
        Paragraph p = new Paragraph(String.valueOf(value)).setFontSize(9);
        if (color != null) p.setFontColor(color).setBold();
        return new Cell().add(p).setPadding(5).setTextAlignment(TextAlignment.RIGHT);
    }

    private Cell dataCell(String text, DeviceRgb bg) {
        return dataCell(text, bg, null);
    }

    private Cell dataCell(String text, DeviceRgb bg, DeviceRgb textColor) {
        Paragraph p = new Paragraph(text).setFontSize(8);
        if (textColor != null) p.setFontColor(textColor).setBold();
        return new Cell().add(p)
                .setBackgroundColor(bg)
                .setPadding(4)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    private void addMetaRow(Table t, String key, String value) {
        t.addCell(new Cell().add(new Paragraph(key).setBold().setFontSize(10))
                .setBackgroundColor(GREY_LIGHT).setPadding(6));
        t.addCell(new Cell().add(new Paragraph(value != null ? value : "—").setFontSize(10))
                .setPadding(6));
    }

    private void addDistRow(Table t, String label, int count, int total) {
        double pct = total > 0 ? count * 100.0 / total : 0;
        t.addCell(new Cell().add(new Paragraph(label).setFontSize(9)).setPadding(4));
        t.addCell(new Cell().add(new Paragraph(String.valueOf(count)).setFontSize(9))
                .setPadding(4).setTextAlignment(TextAlignment.RIGHT));
        t.addCell(new Cell().add(new Paragraph(String.format("%.0f%%", pct)).setFontSize(9))
                .setPadding(4).setTextAlignment(TextAlignment.RIGHT));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String formatDuration(long ms) {
        long s = ms / 1000;
        return String.format("%dm %ds", s / 60, s % 60);
    }

    /**
     * Very rough estimate: what fraction of requests fall below the threshold,
     * based on the p50/p75/p95 breakpoints.
     */
    private double pctBelow(long p50, long p75, long p95, long threshold) {
        if (threshold <= p50)  return 0.50 * threshold / p50;
        if (threshold <= p75)  return 0.50 + 0.25 * (threshold - p50) / (double)(p75 - p50 + 1);
        if (threshold <= p95)  return 0.75 + 0.20 * (threshold - p75) / (double)(p95 - p75 + 1);
        return Math.min(1.0,   0.95 + 0.05 * (threshold - p95) / (double)(p95 + 1));
    }
}
