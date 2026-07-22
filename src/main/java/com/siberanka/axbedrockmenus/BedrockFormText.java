package com.siberanka.axbedrockmenus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class BedrockFormText {
    private static final char SECTION = '\u00A7';
    private static final String RESET = "\u00A7r";
    private static final String TITLE_STYLE = "\u00A70\u00A7l";
    private static final String CONTENT_STYLE = "\u00A7f";
    private static final String PRIMARY_STYLE = "\u00A70\u00A7l";
    private static final String SECONDARY_STYLE = "\u00A71";
    private static final Set<String> MINI_MESSAGE_TAGS = Set.of(
            "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
            "gold", "gray", "grey", "dark_gray", "dark_grey", "blue", "green", "aqua",
            "red", "light_purple", "yellow", "white", "bold", "b", "italic", "i",
            "underlined", "u", "strikethrough", "st", "obfuscated", "obf", "reset"
    );

    private BedrockFormText() {
    }

    static String title(String input) {
        String clean = sanitize(input).replace('\n', ' ').strip();
        return clean.isEmpty() ? "" : TITLE_STYLE + clean + RESET;
    }

    static String content(String input) {
        return styledLines(input, CONTENT_STYLE, CONTENT_STYLE);
    }

    static String button(String input) {
        return styledLines(input, PRIMARY_STYLE, SECONDARY_STYLE);
    }

    private static String styledLines(String input, String firstStyle, String remainingStyle) {
        List<String> lines = cleanLines(input);
        if (lines.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder(input == null ? 16 : input.length() + 16);
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                result.append('\n');
            }
            result.append(index == 0 ? firstStyle : remainingStyle)
                    .append(lines.get(index))
                    .append(RESET);
        }
        return result.toString();
    }

    private static List<String> cleanLines(String input) {
        String sanitized = sanitize(input);
        if (sanitized.isEmpty()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (String line : sanitized.split("\\n", -1)) {
            String cleaned = line.strip();
            if (!cleaned.isEmpty()) {
                lines.add(cleaned);
            }
        }
        return List.copyOf(lines);
    }

    private static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String normalized = input.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder result = new StringBuilder(normalized.length());
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            if (current == SECTION) {
                if (index + 1 < normalized.length()) {
                    index++;
                }
                continue;
            }
            if (current == '&' && index + 1 < normalized.length() && isFormatCode(normalized.charAt(index + 1))) {
                index++;
                continue;
            }
            if (current == '<') {
                int closing = normalized.indexOf('>', index + 1);
                if (closing > index && isFormattingTag(normalized.substring(index + 1, closing))) {
                    index = closing;
                    continue;
                }
            }
            if (current == '\n' || !Character.isISOControl(current)) {
                result.append(current);
            }
        }
        return result.toString();
    }

    private static boolean isFormatCode(char value) {
        char normalized = Character.toLowerCase(value);
        return (normalized >= '0' && normalized <= '9')
                || (normalized >= 'a' && normalized <= 'u')
                || normalized == 'x';
    }

    private static boolean isFormattingTag(String rawTag) {
        String tag = rawTag.strip().toLowerCase(Locale.ROOT);
        if (tag.startsWith("/")) {
            tag = tag.substring(1);
        }
        int separator = tag.indexOf(':');
        String name = separator >= 0 ? tag.substring(0, separator) : tag;
        return MINI_MESSAGE_TAGS.contains(name)
                || name.equals("color")
                || name.equals("colour")
                || name.equals("gradient")
                || name.equals("rainbow")
                || name.matches("#[0-9a-f]{6}");
    }
}
