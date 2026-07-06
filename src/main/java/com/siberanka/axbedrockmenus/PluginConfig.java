package com.siberanka.axbedrockmenus;

import java.util.List;

public record PluginConfig(
        String language,
        boolean debug,
        boolean requirePermission,
        boolean interceptBedrockOnly,
        long cacheTtlMillis,
        int maxButtonsPerForm,
        long maxMenuFileBytes,
        int maxYamlDepth,
        int maxYamlNodes,
        RateLimitSpec menuOpenLimit,
        RateLimitSpec buttonClickLimit,
        int maxCommandLength,
        boolean allowConsoleActions,
        List<String> blockedRoots,
        List<String> blockedSubstrings,
        String prefix
) {
}
