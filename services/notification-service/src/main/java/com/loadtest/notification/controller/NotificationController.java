package com.loadtest.notification.controller;

import com.loadtest.notification.domain.NotificationHistory;
import com.loadtest.notification.dto.WebhookRequest;
import com.loadtest.notification.dto.WebhookResponse;
import com.loadtest.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/webhooks")
    @ResponseStatus(HttpStatus.CREATED)
    public WebhookResponse registerWebhook(
            @Valid @RequestBody WebhookRequest request,
            @RequestHeader("X-User-Id") String userId) {
        return notificationService.registerWebhook(request, UUID.fromString(userId));
    }

    @GetMapping("/webhooks")
    public List<WebhookResponse> listWebhooks(
            @RequestHeader("X-User-Id") String userId) {
        return notificationService.listWebhooks(UUID.fromString(userId));
    }

    @DeleteMapping("/webhooks/{id}")
    public ResponseEntity<Void> deleteWebhook(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId) {
        notificationService.deleteWebhook(id, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    public List<NotificationHistory> getHistory(
            @RequestHeader("X-User-Id") String userId) {
        return notificationService.getHistory(UUID.fromString(userId));
    }
}
