package com.siberanka.axbedrockmenus;

import java.util.List;

public record Integration(
        String id,
        String pluginName,
        String title,
        List<String> aliases,
        List<String> menuFiles,
        List<MenuButton> fallbackButtons
) {
}
