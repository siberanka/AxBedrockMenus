package com.siberanka.axbedrockmenus;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RateLimiter {
    private final RateLimitSpec spec;
    private final Map<UUID, Deque<Long>> actions = new HashMap<>();

    public RateLimiter(RateLimitSpec spec) {
        this.spec = spec;
    }

    public synchronized boolean tryAcquire(UUID uuid) {
        long now = System.currentTimeMillis();
        Deque<Long> queue = actions.computeIfAbsent(uuid, ignored -> new ArrayDeque<>());
        while (!queue.isEmpty() && now - queue.peekFirst() > spec.windowMillis()) {
            queue.removeFirst();
        }
        if (queue.size() >= spec.maxActions()) {
            return false;
        }
        queue.addLast(now);
        if (actions.size() > 2048) {
            actions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        }
        return true;
    }
}
