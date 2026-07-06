package com.siberanka.axbedrockmenus;

import java.util.List;

public record MenuData(String title, String content, List<MenuButton> buttons) {
    public MenuData(String title, List<MenuButton> buttons) {
        this(title, "", buttons);
    }
}
