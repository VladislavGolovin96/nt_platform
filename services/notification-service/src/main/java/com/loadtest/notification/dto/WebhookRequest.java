package com.loadtest.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record WebhookRequest(
        @NotBlank String url,
        @NotEmpty List<String> events
) {}
