package com.loadtest.notification.service;

import com.loadtest.notification.domain.NotificationChannel;
import com.loadtest.notification.domain.NotificationHistory;
import com.loadtest.notification.domain.NotificationStatus;
import com.loadtest.notification.repository.NotificationHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${notification.email.from}")
    private String fromAddress;

    private final JavaMailSender mailSender;
    private final NotificationHistoryRepository historyRepository;

    public EmailService(JavaMailSender mailSender,
                        NotificationHistoryRepository historyRepository) {
        this.mailSender = mailSender;
        this.historyRepository = historyRepository;
    }

    public void send(UUID userId, String toEmail, String eventType, String subject, String body) {
        NotificationStatus status = NotificationStatus.FAILED;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            status = NotificationStatus.SENT;
            log.info("Email sent to {} for event {}", toEmail, eventType);
        } catch (Exception e) {
            log.error("Failed to send email to {} for event {}: {}", toEmail, eventType, e.getMessage());
        } finally {
            saveHistory(userId, eventType, status, Map.of("to", toEmail, "subject", subject));
        }
    }

    private void saveHistory(UUID userId, String event, NotificationStatus status,
                              Map<String, Object> payload) {
        NotificationHistory history = new NotificationHistory();
        history.setUserId(userId);
        history.setEvent(event);
        history.setChannel(NotificationChannel.EMAIL);
        history.setStatus(status);
        history.setPayload(payload);
        history.setSentAt(Instant.now());
        historyRepository.save(history);
    }
}
