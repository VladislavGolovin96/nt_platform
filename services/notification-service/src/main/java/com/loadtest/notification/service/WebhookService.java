package com.loadtest.notification.service;

import com.loadtest.notification.domain.NotificationChannel;
import com.loadtest.notification.domain.NotificationHistory;
import com.loadtest.notification.domain.NotificationStatus;
import com.loadtest.notification.domain.Webhook;
import com.loadtest.notification.dto.WebhookPayload;
import com.loadtest.notification.repository.NotificationHistoryRepository;
import com.loadtest.notification.repository.WebhookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    @Value("${notification.webhook.max-retries:3}")
    private int maxRetries;

    @Value("${notification.webhook.timeout-ms:5000}")
    private int timeoutMs;

    private final WebhookRepository webhookRepository;
    private final NotificationHistoryRepository historyRepository;
    private final RestTemplate restTemplate;

    public WebhookService(WebhookRepository webhookRepository,
                          NotificationHistoryRepository historyRepository,
                          RestTemplate restTemplate) {
        this.webhookRepository = webhookRepository;
        this.historyRepository = historyRepository;
        this.restTemplate = restTemplate;
    }

    public void dispatchForUser(UUID userId, String eventType, WebhookPayload payload) {
        List<Webhook> hooks = webhookRepository.findActiveByUserIdAndEvent(userId, "[\"" + eventType + "\"]");
        for (Webhook hook : hooks) {
            deliver(hook, eventType, payload, userId);
        }
    }

    public void dispatchForEvent(String eventType, WebhookPayload payload) {
        List<Webhook> hooks = webhookRepository.findActiveByEvent("[\"" + eventType + "\"]");
        for (Webhook hook : hooks) {
            deliver(hook, eventType, payload, hook.getUserId());
        }
    }

    private void deliver(Webhook hook, String eventType, WebhookPayload payload, UUID userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<WebhookPayload> request = new HttpEntity<>(payload, headers);

        NotificationStatus status = NotificationStatus.FAILED;
        Exception lastError = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                restTemplate.postForEntity(hook.getUrl(), request, String.class);
                status = NotificationStatus.SENT;
                log.info("Webhook delivered to {} for event {} (attempt {})", hook.getUrl(), eventType, attempt);
                break;
            } catch (Exception e) {
                lastError = e;
                log.warn("Webhook attempt {}/{} failed for {}: {}", attempt, maxRetries, hook.getUrl(), e.getMessage());
                if (attempt < maxRetries) {
                    try { Thread.sleep(1000L * attempt); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (status == NotificationStatus.FAILED && lastError != null) {
            log.error("Webhook delivery failed after {} attempts for {}: {}",
                    maxRetries, hook.getUrl(), lastError.getMessage());
        }

        saveHistory(userId, eventType, NotificationChannel.WEBHOOK, status, toMap(payload));
    }

    private void saveHistory(UUID userId, String event, NotificationChannel channel,
                              NotificationStatus status, Map<String, Object> payload) {
        NotificationHistory history = new NotificationHistory();
        history.setUserId(userId);
        history.setEvent(event);
        history.setChannel(channel);
        history.setStatus(status);
        history.setPayload(payload);
        history.setSentAt(Instant.now());
        historyRepository.save(history);
    }

    private Map<String, Object> toMap(WebhookPayload p) {
        Map<String, Object> map = new HashMap<>();
        map.put("event", p.event());
        map.put("executionId", p.executionId());
        if (p.status() != null) map.put("status", p.status());
        if (p.duration() != null) map.put("duration", p.duration());
        if (p.error() != null) map.put("error", p.error());
        if (p.reportUrl() != null) map.put("reportUrl", p.reportUrl());
        map.put("timestamp", p.timestamp());
        return map;
    }
}
