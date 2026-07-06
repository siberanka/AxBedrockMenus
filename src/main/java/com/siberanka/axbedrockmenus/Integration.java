package com.siberanka.axbedrockmenus;

import java.util.List;

public record Integration(
        String id,
        String pluginName,
        String title,
        List<String> aliases,
        List<String> aliasPaths,
        List<String> menuFiles,
        List<MenuButton> quickActions,
        List<MenuButton> fallbackButtons
) {
}
