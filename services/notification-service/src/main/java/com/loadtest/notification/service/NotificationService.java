package com.loadtest.notification.service;

import com.loadtest.common.exception.ResourceNotFoundException;
import com.loadtest.notification.domain.Webhook;
import com.loadtest.notification.domain.NotificationHistory;
import com.loadtest.notification.domain.UserRecord;
import com.loadtest.notification.dto.WebhookPayload;
import com.loadtest.notification.dto.WebhookRequest;
import com.loadtest.notification.dto.WebhookResponse;
import com.loadtest.notification.repository.ExecutionLookupRepository;
import com.loadtest.notification.repository.NotificationHistoryRepository;
import com.loadtest.notification.repository.UserLookupRepository;
import com.loadtest.notification.repository.WebhookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final WebhookRepository webhookRepository;
    private final NotificationHistoryRepository historyRepository;
    private final ExecutionLookupRepository executionLookupRepository;
    private final UserLookupRepository userLookupRepository;
    private final WebhookService webhookService;
    private final EmailService emailService;

    public NotificationService(WebhookRepository webhookRepository,
                                NotificationHistoryRepository historyRepository,
                                ExecutionLookupRepository executionLookupRepository,
                                UserLookupRepository userLookupRepository,
                                WebhookService webhookService,
                                EmailService emailService) {
        this.webhookRepository = webhookRepository;
        this.historyRepository = historyRepository;
        this.executionLookupRepository = executionLookupRepository;
        this.userLookupRepository = userLookupRepository;
        this.webhookService = webhookService;
        this.emailService = emailService;
    }

    public void onTestStarted(String executionId) {
        log.info("test.started received for execution {}", executionId);
        // per CLAUDE.md: log info only for test.started
    }

    public void onTestFinished(String executionId, String status, long duration) {
        log.info("test.finished for execution {}, status={}, duration={}ms", executionId, status, duration);
        UUID userId = resolveUserId(executionId);
        WebhookPayload payload = WebhookPayload.ofFinished(executionId, status, duration, Instant.now().toString());

        if (userId != null) {
            webhookService.dispatchForUser(userId, "test.finished", payload);
            sendEmail(userId, "test.finished",
                    "Load Test Finished: " + status,
                    String.format("Execution %s completed with status %s in %d ms.", executionId, status, duration));
        } else {
            webhookService.dispatchForEvent("test.finished", payload);
        }
    }

    public void onTestFailed(String executionId, String error, int exitCode) {
        log.warn("test.failed for execution {}, error={}", executionId, error);
        UUID userId = resolveUserId(executionId);
        WebhookPayload payload = WebhookPayload.ofFailed(executionId, error, exitCode, Instant.now().toString());

        if (userId != null) {
            webhookService.dispatchForUser(userId, "test.failed", payload);
            sendEmail(userId, "test.failed",
                    "[HIGH PRIORITY] Load Test Failed",
                    String.format("Execution %s failed with exit code %d.%nError: %s", executionId, exitCode, error));
        } else {
            webhookService.dispatchForEvent("test.failed", payload);
        }
    }

    public void onReportGenerated(String executionId, String reportPath, String downloadUrl) {
        log.info("report.generated for execution {}", executionId);
        UUID userId = resolveUserId(executionId);
        WebhookPayload payload = WebhookPayload.ofReportGenerated(executionId, downloadUrl, Instant.now().toString());

        if (userId != null) {
            webhookService.dispatchForUser(userId, "report.generated", payload);
            sendEmail(userId, "report.generated",
                    "Load Test Report Ready",
                    String.format("Your load test report for execution %s is ready.%nDownload: %s",
                            executionId, downloadUrl));
        } else {
            webhookService.dispatchForEvent("report.generated", payload);
        }
    }

    @Transactional
    public WebhookResponse registerWebhook(WebhookRequest request, UUID userId) {
        Webhook webhook = new Webhook();
        webhook.setUserId(userId);
        webhook.setUrl(request.url());
        webhook.setEvents(request.events());
        webhook.setActive(true);
        return WebhookResponse.from(webhookRepository.save(webhook));
    }

    @Transactional(readOnly = true)
    public List<WebhookResponse> listWebhooks(UUID userId) {
        return webhookRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(WebhookResponse::from).toList();
    }

    @Transactional
    public void deleteWebhook(UUID webhookId, UUID userId) {
        Webhook webhook = webhookRepository.findByIdAndUserId(webhookId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook", webhookId.toString()));
        webhookRepository.delete(webhook);
    }

    @Transactional(readOnly = true)
    public List<NotificationHistory> getHistory(UUID userId) {
        return historyRepository.findByUserIdOrderBySentAtDesc(userId);
    }

    private UUID resolveUserId(String executionId) {
        try {
            return executionLookupRepository.findById(UUID.fromString(executionId))
                    .map(e -> e.getUserId())
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Could not resolve userId for execution {}: {}", executionId, e.getMessage());
            return null;
        }
    }

    private void sendEmail(UUID userId, String event, String subject, String body) {
        Optional<UserRecord> user = userLookupRepository.findById(userId);
        if (user.isPresent() && user.get().getEmail() != null) {
            emailService.send(userId, user.get().getEmail(), event, subject, body);
        } else {
            log.warn("No email found for userId {}, skipping email for event {}", userId, event);
        }
    }
}
