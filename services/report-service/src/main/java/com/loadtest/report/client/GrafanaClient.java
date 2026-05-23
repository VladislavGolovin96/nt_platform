package com.loadtest.report.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

@Component
public class GrafanaClient {

    private static final Logger log = LoggerFactory.getLogger(GrafanaClient.class);

    @Value("${grafana.url}")
    private String grafanaUrl;

    @Value("${grafana.token}")
    private String token;

    @Value("${grafana.dashboard-uid}")
    private String dashboardUid;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public byte[] renderPanel(int panelId, Instant from, Instant to) {
        String url = grafanaUrl + "/render/d-solo/" + dashboardUid
                + "?orgId=1"
                + "&from=" + from.toEpochMilli()
                + "&to=" + to.toEpochMilli()
                + "&width=1000&height=400"
                + "&panelId=" + panelId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                return response.body().readAllBytes();
            }
            log.warn("Grafana render returned HTTP {} for panelId={}", response.statusCode(), panelId);
        } catch (IOException | InterruptedException e) {
            log.warn("Grafana render failed for panelId={}: {}", panelId, e.getMessage());
        }
        return new byte[0];
    }
}
