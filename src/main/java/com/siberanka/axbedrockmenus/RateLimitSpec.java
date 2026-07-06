package com.siberanka.axbedrockmenus;

public record RateLimitSpec(long windowMillis, int maxActions) {
}
