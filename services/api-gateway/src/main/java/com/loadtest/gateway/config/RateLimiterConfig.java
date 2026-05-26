package com.loadtest.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public RedisRateLimiter defaultRateLimiter() {
        // replenishRate: tokens added per second = 100/60 ≈ 2
        // burstCapacity: max tokens in bucket = 100 (handles burst of 100 req)
        return new RedisRateLimiter(2, 100, 1);
    }

    /**
     * Strict rate limiter for POST /api/executions: 10 requests/min per user.
     */
    @Bean
    public RedisRateLimiter executionsRateLimiter() {
        // replenishRate: ~1 token per 6 seconds (10/min)
        // burstCapacity: max 10 in the bucket
        return new RedisRateLimiter(1, 10, 1);
    }
}
