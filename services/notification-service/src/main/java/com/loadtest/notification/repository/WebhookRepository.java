package com.loadtest.notification.repository;

import com.loadtest.notification.domain.Webhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookRepository extends JpaRepository<Webhook, UUID> {

    List<Webhook> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Webhook> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Find all active webhooks for a user that subscribe to the given event type.
     * Uses native JSONB containment operator.
     */
    @Query(value = "SELECT * FROM webhooks WHERE user_id = :userId AND active = true AND events @> CAST(:event AS jsonb)",
            nativeQuery = true)
    List<Webhook> findActiveByUserIdAndEvent(@Param("userId") UUID userId, @Param("event") String event);

    /**
     * Find all active webhooks (across all users) that subscribe to the given event.
     */
    @Query(value = "SELECT * FROM webhooks WHERE active = true AND events @> CAST(:event AS jsonb)",
            nativeQuery = true)
    List<Webhook> findActiveByEvent(@Param("event") String event);
}
