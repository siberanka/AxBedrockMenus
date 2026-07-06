package com.siberanka.axbedrockmenus;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

@SuppressWarnings("deprecation")
public final class LanguageService {
    private final PluginConfig config;
    private final YamlConfiguration language;
    private final YamlConfiguration fallback;

    public LanguageService(JavaPlugin plugin, ConfigService configService, String languageCode) {
        this.config = configService.loadPluginConfig();
        this.language = configService.loadLanguage(languageCode);
        this.fallback = configService.loadLanguage("en");
    }

    public String message(String key) {
        return color(config.prefix() + raw(key));
    }

    public String message(String key, Map<String, String> replacements) {
        String value = config.prefix() + raw(key);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            value = value.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return color(value);
    }

    public String plain(String key) {
        return stripColor(color(raw(key)));
    }

    public String translateToken(String value) {
        if (value == null) {
            return "";
        }
        if (value.startsWith("%") && value.endsWith("%") && value.length() > 2) {
            return plain(value.substring(1, value.length() - 1));
        }
        return stripColor(color(value));
    }

    public String stripColor(String value) {
        return ChatColor.stripColor(color(value));
    }

    public String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value == null ? "" : value);
    }

    private String raw(String key) {
        return language.getString(key, fallback.getString(key, key));
    }
}
