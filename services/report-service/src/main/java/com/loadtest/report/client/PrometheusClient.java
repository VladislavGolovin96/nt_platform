package com.loadtest.report.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class PrometheusClient {

    private static final Logger log = LoggerFactory.getLogger(PrometheusClient.class);

    @Value("${prometheus.url}")
    private String prometheusUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<MetricPoint> queryRange(String query, Instant start, Instant end, String step) {
        String url = prometheusUrl + "/api/v1/query_range"
                + "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&start=" + start.getEpochSecond()
                + "&end=" + end.getEpochSecond()
                + "&step=" + step;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Prometheus returned HTTP {} for query: {}", response.statusCode(), query);
                return List.of();
            }
            return parseResponse(response.body());
        } catch (Exception e) {
            log.warn("Prometheus query failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<MetricPoint> parseResponse(String json) {
        List<MetricPoint> points = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode results = root.path("data").path("result");
            if (results.isArray() && !results.isEmpty()) {
                JsonNode values = results.get(0).path("values");
                for (JsonNode value : values) {
                    long epochSeconds = value.get(0).asLong();
                    double val = Double.parseDouble(value.get(1).asText("0"));
                    points.add(new MetricPoint(Instant.ofEpochSecond(epochSeconds), val));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse Prometheus response: {}", e.getMessage());
        }
        return points;
    }
}
