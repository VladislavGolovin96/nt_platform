package com.loadtest.notification.config;

import org.apache.kafka.common.errors.SerializationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConfig {

    /**
     * Exponential back-off for Kafka listener errors.
     * Prevents tight retry loops (CPU spikes) when a message fails to process.
     *
     * Back-off schedule: 1s → 2s → 4s → 8s → ... → max 30s
     * Stops retrying after 5 minutes total elapsed time.
     * SerializationException is marked as non-retryable (seek past the record).
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        ExponentialBackOff backOff = new ExponentialBackOff(1_000L, 2.0);
        backOff.setMaxInterval(30_000L);
        backOff.setMaxElapsedTime(300_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(backOff);
        handler.addNotRetryableExceptions(SerializationException.class);
        return handler;
    }
}
