package com.siberanka.axbedrockmenus;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ArtillexActionParser {
    private ArtillexActionParser() {
    }

    public static List<String> commandsFor(Integration integration, ConfigurationSection section, String contextCommand) {
        List<String> actions = rawActions(section);
        List<String> commands = new ArrayList<>();
        for (String action : actions) {
            parseAction(integration, action, contextCommand).stream().limit(4 - commands.size()).forEach(commands::add);
            if (commands.size() >= 4) {
                break;
            }
        }
        if (commands.isEmpty() && contextCommand != null && !contextCommand.isBlank()) {
            commands.add(contextCommand);
        }
        return List.copyOf(commands);
    }

    public static List<String> rawActions(ConfigurationSection section) {
        List<String> actions = new ArrayList<>();
        read(section, actions,
                "actions",
                "commands",
                "command",
                "player-command",
                "player-commands",
                "left-click-actions",
                "right-click-actions",
                "left-click-commands",
                "right-click-commands",
                "click-actions",
                "click-commands");
        return List.copyOf(actions);
    }

    private static List<String> parseAction(Integration integration, String raw, String contextCommand) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String action = raw.trim();
        String lower = action.toLowerCase(Locale.ROOT);
        if (lower.startsWith("[sound]") || lower.startsWith("[close]") || lower.startsWith("[refresh]")) {
            return List.of(contextCommand == null ? firstAlias(integration) : contextCommand);
        }
        if (lower.startsWith("[message]")) {
            return List.of();
        }
        if (lower.startsWith("[menu]")) {
            String target = action.substring("[menu]".length()).trim();
            if (target.equalsIgnoreCase("close")) {
                return List.of();
            }
            return List.of(firstAlias(integration) + (target.isBlank() ? "" : " " + target));
        }
        if (lower.startsWith("[page]")) {
            String target = action.substring("[page]".length()).trim();
            return List.of(firstAlias(integration) + (target.isBlank() ? "" : " " + target));
        }
        if (lower.startsWith("[console]") || lower.startsWith("[player]") || lower.startsWith("console:") || lower.startsWith("player:")) {
            return List.of(action);
        }
        if (action.startsWith("[") && action.contains("]")) {
            return List.of();
        }
        return List.of(action);
    }

    private static String firstAlias(Integration integration) {
        return integration.aliases().isEmpty() ? integration.pluginName().toLowerCase(Locale.ROOT) : integration.aliases().get(0);
    }

    private static void read(ConfigurationSection section, List<String> actions, String... keys) {
        for (String key : keys) {
            Object value = section.get(key);
            if (value instanceof String string && !string.isBlank()) {
                actions.add(string);
            } else if (value instanceof List<?> list) {
                for (Object entry : list) {
                    if (entry != null && !String.valueOf(entry).isBlank()) {
                        actions.add(String.valueOf(entry));
                    }
                }
            }
        }
    }
}
