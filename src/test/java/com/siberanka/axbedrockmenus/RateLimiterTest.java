package com.siberanka.axbedrockmenus;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {
    @Test
    void blocksActionsOverLimitInsideWindow() {
        RateLimiter limiter = new RateLimiter(new RateLimitSpec(10_000, 2));
        UUID uuid = UUID.randomUUID();

        assertTrue(limiter.tryAcquire(uuid));
        assertTrue(limiter.tryAcquire(uuid));
        assertFalse(limiter.tryAcquire(uuid));
    }

    @Test
    void tracksPlayersIndependently() {
        RateLimiter limiter = new RateLimiter(new RateLimitSpec(10_000, 1));

        assertTrue(limiter.tryAcquire(UUID.randomUUID()));
        assertTrue(limiter.tryAcquire(UUID.randomUUID()));
    }
}
