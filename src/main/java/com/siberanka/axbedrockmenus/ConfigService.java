package com.siberanka.axbedrockmenus;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ConfigService {
    private static final DateTimeFormatter BACKUP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final JavaPlugin plugin;

    public ConfigService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void prepareFiles() {
        saveIfMissing("config.yml");
        saveIfMissing("integrations.yml");
        saveIfMissing("lang/tr.yml");
        saveIfMissing("lang/en.yml");
        saveIfMissing("lang/az.yml");
        saveIfMissing("lang/es.yml");
        repairKnownFile("config.yml");
        repairKnownFile("lang/tr.yml");
        repairKnownFile("lang/en.yml");
        repairKnownFile("lang/az.yml");
        repairKnownFile("lang/es.yml");
    }

    public PluginConfig loadPluginConfig() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file("config.yml"));
        return new PluginConfig(
                config.getString("settings.language", "tr"),
                config.getBoolean("settings.debug", false),
                config.getBoolean("settings.require-permission", true),
                config.getBoolean("settings.intercept-bedrock-only", true),
                Math.max(1000L, config.getLong("settings.cache-ttl-millis", 5000L)),
                clamp(config.getInt("settings.max-buttons-per-form", 80), 1, 100),
                Math.max(65536L, config.getLong("settings.max-menu-file-bytes", 524288L)),
                clamp(config.getInt("settings.max-yaml-depth", 8), 2, 16),
                clamp(config.getInt("settings.max-yaml-nodes", 2000), 100, 10000),
                new RateLimitSpec(Math.max(250L, config.getLong("rate-limits.menu-open.window-millis", 1200L)), clamp(config.getInt("rate-limits.menu-open.max-actions", 2), 1, 20)),
                new RateLimitSpec(Math.max(250L, config.getLong("rate-limits.button-click.window-millis", 1000L)), clamp(config.getInt("rate-limits.button-click.max-actions", 1), 1, 20)),
                clamp(config.getInt("command-security.max-command-length", 180), 20, 512),
                config.getBoolean("command-security.allow-console-actions", false),
                lowerList(config.getStringList("command-security.blocked-roots")),
                lowerList(config.getStringList("command-security.blocked-substrings")),
                config.getString("messages.prefix", "&8[&bAxBedrockMenus&8]&r ")
        );
    }

    public YamlConfiguration loadIntegrations() {
        return YamlConfiguration.loadConfiguration(file("integrations.yml"));
    }

    public YamlConfiguration loadLanguage(String language) {
        File target = file("lang/" + language + ".yml");
        if (!target.isFile()) {
            target = file("lang/en.yml");
        }
        return YamlConfiguration.loadConfiguration(target);
    }

    private void saveIfMissing(String resource) {
        File target = file(resource);
        if (!target.isFile()) {
            plugin.saveResource(resource, false);
        }
    }

    private void repairKnownFile(String resource) {
        File target = file(resource);
        YamlConfiguration defaults = loadResourceYaml(resource);
        YamlConfiguration current = YamlConfiguration.loadConfiguration(target);
        boolean dirty = false;

        for (String key : current.getKeys(true)) {
            if (!defaults.contains(key)) {
                dirty = true;
                break;
            }
        }
        if (!dirty) {
            for (String key : defaults.getKeys(true)) {
                if (!current.contains(key) || !compatible(defaults.get(key), current.get(key))) {
                    dirty = true;
                    break;
                }
            }
        }
        if (!dirty) {
            return;
        }

        backup(target);
        YamlConfiguration repaired = new YamlConfiguration();
        for (String key : defaults.getKeys(true)) {
            Object defaultValue = defaults.get(key);
            if (defaultValue instanceof ConfigurationSection) {
                repaired.createSection(key);
                continue;
            }
            Object currentValue = current.get(key);
            repaired.set(key, compatible(defaultValue, currentValue) ? currentValue : defaultValue);
        }
        try {
            repaired.save(target);
            plugin.getLogger().warning("Repaired " + resource + " after backing up invalid or unknown entries.");
        } catch (Exception exception) {
            plugin.getLogger().severe("Could not repair " + resource + ": " + exception.getMessage());
        }
    }

    private YamlConfiguration loadResourceYaml(String resource) {
        try (var stream = plugin.getResource(resource)) {
            if (stream == null) {
                return new YamlConfiguration();
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (Exception exception) {
            plugin.getLogger().severe("Could not read bundled " + resource + ": " + exception.getMessage());
            return new YamlConfiguration();
        }
    }

    private void backup(File target) {
        try {
            File backup = new File(target.getParentFile(), target.getName() + "." + BACKUP_FORMAT.format(LocalDateTime.now()) + ".bak");
            Files.copy(target.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception exception) {
            plugin.getLogger().severe("Could not create backup for " + target.getName() + ": " + exception.getMessage());
        }
    }

    private boolean compatible(Object expected, Object actual) {
        if (actual == null) {
            return false;
        }
        if (expected instanceof ConfigurationSection) {
            return actual instanceof ConfigurationSection;
        }
        if (expected instanceof List<?>) {
            return actual instanceof List<?>;
        }
        if (expected instanceof Number) {
            return actual instanceof Number;
        }
        return Objects.equals(expected.getClass(), actual.getClass());
    }

    private File file(String path) {
        return new File(plugin.getDataFolder(), path.replace("/", File.separator));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static List<String> lowerList(List<String> input) {
        List<String> result = new ArrayList<>(input.size());
        for (String value : input) {
            result.add(value.toLowerCase());
        }
        return List.copyOf(result);
    }
}
