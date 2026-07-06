package com.siberanka.axbedrockmenus;

import java.util.List;

public record MenuButton(String text, List<String> commands, String permission) {
}
