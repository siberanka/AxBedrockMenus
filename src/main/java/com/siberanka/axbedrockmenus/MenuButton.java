package com.siberanka.axbedrockmenus;

import java.util.List;

public record MenuButton(String text, List<String> commands, String permission, List<String> description) {
    public MenuButton(String text, List<String> commands, String permission) {
        this(text, commands, permission, List.of());
    }
}
