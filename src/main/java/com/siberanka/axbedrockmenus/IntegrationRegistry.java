package com.siberanka.axbedrockmenus;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class IntegrationRegistry {
    private final JavaPlugin plugin;
    private final List<Integration> integrations;
    private final Map<String, Integration> byAlias;

    public IntegrationRegistry(JavaPlugin plugin, YamlConfiguration configuration) {
        this.plugin = plugin;
        this.integrations = new ArrayList<>();
        this.byAlias = new HashMap<>();
        ConfigurationSection root = configuration.getConfigurationSection("integrations");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null || !section.getBoolean("enabled", true)) {
                continue;
            }
            List<MenuButton> fallback = new ArrayList<>();
            for (Map<?, ?> map : section.getMapList("fallback-buttons")) {
                readButton(map).ifPresent(fallback::add);
            }
            List<MenuButton> quickActions = new ArrayList<>();
            for (Map<?, ?> map : section.getMapList("quick-actions")) {
                readButton(map).ifPresent(quickActions::add);
            }
            Integration integration = new Integration(
                    id.toLowerCase(Locale.ROOT),
                    section.getString("plugin", id),
                    section.getString("title", id),
                    lower(section.getStringList("aliases")),
                    section.getStringList("alias-paths"),
                    section.getStringList("menu-files"),
                    List.copyOf(quickActions),
                    List.copyOf(fallback)
            );
            integrations.add(integration);
            for (String alias : aliasesFor(integration)) {
                byAlias.put(alias, integration);
            }
        }
    }

    private Optional<MenuButton> readButton(Map<?, ?> map) {
        Object text = map.get("text");
        Object command = map.get("command");
        if (text == null || command == null) {
            return Optional.empty();
        }
        List<String> commands;
        if (command instanceof List<?> list) {
            commands = new ArrayList<>();
            for (Object value : list) {
                if (value != null && !String.valueOf(value).isBlank()) {
                    commands.add(String.valueOf(value));
                }
            }
        } else {
            commands = List.of(String.valueOf(command));
        }
        List<String> description = new ArrayList<>();
        Object rawDescription = map.get("description");
        if (rawDescription instanceof List<?> list) {
            for (Object value : list) {
                if (value != null && !String.valueOf(value).isBlank()) {
                    description.add(String.valueOf(value));
                }
            }
        } else if (rawDescription != null && !String.valueOf(rawDescription).isBlank()) {
            description.add(String.valueOf(rawDescription));
        }
        Object permission = map.get("permission");
        return Optional.of(new MenuButton(String.valueOf(text), List.copyOf(commands), permission == null ? null : String.valueOf(permission), List.copyOf(description)));
    }

    public Optional<Integration> findByCommand(String commandLine) {
        String normalized = commandLine.startsWith("/") ? commandLine.substring(1) : commandLine;
        String root = normalized.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (root.contains(":")) {
            root = root.substring(root.indexOf(':') + 1);
        }
        return Optional.ofNullable(byAlias.get(root));
    }

    public boolean isPluginAvailable(Integration integration) {
        var target = Bukkit.getPluginManager().getPlugin(integration.pluginName());
        return target != null && target.isEnabled();
    }

    public int enabledCount() {
        return integrations.size();
    }

    public int availableCount() {
        int count = 0;
        for (Integration integration : integrations) {
            if (isPluginAvailable(integration)) {
                count++;
            }
        }
        return count;
    }

    private static List<String> lower(List<String> values) {
        List<String> lowered = new ArrayList<>(values.size());
        for (String value : values) {
            lowered.add(value.toLowerCase(Locale.ROOT));
        }
        return List.copyOf(lowered);
    }

    private List<String> aliasesFor(Integration integration) {
        List<String> aliases = new ArrayList<>(integration.aliases());
        var target = Bukkit.getPluginManager().getPlugin(integration.pluginName());
        if (target == null || !target.isEnabled()) {
            return aliases;
        }
        var configFile = new java.io.File(target.getDataFolder(), "config.yml");
        if (!configFile.isFile()) {
            return aliases;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        for (String path : integration.aliasPaths()) {
            aliases.addAll(lower(config.getStringList(path)));
        }
        return aliases.stream().distinct().toList();
    }
}
