package com.loadtest.notification.dto;

import com.loadtest.notification.domain.Webhook;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WebhookResponse(
        UUID id,
        UUID userId,
        String url,
        List<String> events,
        boolean active,
        Instant createdAt
) {
    public static WebhookResponse from(Webhook w) {
        return new WebhookResponse(
                w.getId(), w.getUserId(), w.getUrl(),
                w.getEvents(), w.isActive(), w.getCreatedAt());
    }
}
