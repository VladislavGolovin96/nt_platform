package com.loadtest.notification.repository;

import com.loadtest.notification.domain.NotificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, UUID> {

    List<NotificationHistory> findByUserIdOrderBySentAtDesc(UUID userId);

    List<NotificationHistory> findAllByOrderBySentAtDesc();
}
