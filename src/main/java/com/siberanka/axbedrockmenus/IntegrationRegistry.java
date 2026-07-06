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
                Object text = map.get("text");
                Object command = map.get("command");
                if (text != null && command != null) {
                    fallback.add(new MenuButton(String.valueOf(text), List.of(String.valueOf(command)), null));
                }
            }
            Integration integration = new Integration(
                    id.toLowerCase(Locale.ROOT),
                    section.getString("plugin", id),
                    section.getString("title", id),
                    lower(section.getStringList("aliases")),
                    section.getStringList("menu-files"),
                    List.copyOf(fallback)
            );
            integrations.add(integration);
            for (String alias : integration.aliases()) {
                byAlias.put(alias, integration);
            }
        }
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
}
