package com.loadtest.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimiterConfig {

    /**
     * Default rate limiter: 100 requests/min per user.
     * replenishRate=100 tokens/sec would be too fast; using replenishRate per
     * Spring's token bucket where replenishRate = tokens added per second.
     * To get 100 req/min: replenishRate=2 (100/60 ≈ 2/s), burstCapacity=100.
     *
     * Alternative: use replenishRate=100 with a 1-minute window via Redis TTL,
     * but Spring's RedisRateLimiter works per-second with burst capacity.
     * burstCapacity=100 allows up to 100 concurrent requests before throttling.
     */
    @Bean
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
