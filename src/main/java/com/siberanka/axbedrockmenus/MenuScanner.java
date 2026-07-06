package com.siberanka.axbedrockmenus;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MenuScanner {
    private static final Set<String> FILLER_MATERIALS = Set.of(
            "AIR",
            "BLACK_STAINED_GLASS_PANE",
            "GRAY_STAINED_GLASS_PANE",
            "LIGHT_GRAY_STAINED_GLASS_PANE",
            "WHITE_STAINED_GLASS_PANE",
            "GLASS_PANE",
            "STAINED_GLASS_PANE"
    );

    private final JavaPlugin owner;
    private final PluginConfig config;
    private final LanguageService language;
    private final Map<String, CacheEntry> cache = new HashMap<>();

    public MenuScanner(JavaPlugin owner, PluginConfig config, LanguageService language) {
        this.owner = owner;
        this.config = config;
        this.language = language;
    }

    public Optional<MenuData> loadGenericMenu(Integration integration, Player player) {
        List<MenuButton> buttons = cachedButtons(integration);
        List<MenuButton> visible = new ArrayList<>();
        for (MenuButton button : integration.quickActions()) {
            if (button.permission() == null || button.permission().isBlank() || player.hasPermission(button.permission())) {
                visible.add(new MenuButton(withSummary(button.text(), button.description()), expandBase(integration, button.commands()), button.permission(), button.description()));
            }
            if (visible.size() >= config.maxButtonsPerForm()) {
                break;
            }
        }
        for (MenuButton button : buttons) {
            if (button.permission() == null || button.permission().isBlank() || player.hasPermission(button.permission())) {
                visible.add(button);
            }
            if (visible.size() >= config.maxButtonsPerForm()) {
                break;
            }
        }
        if (visible.isEmpty()) {
            for (MenuButton fallback : integration.fallbackButtons()) {
                visible.add(new MenuButton(language.translateToken(fallback.text()), fallback.commands(), fallback.permission()));
                if (visible.size() >= config.maxButtonsPerForm()) {
                    break;
                }
            }
        }
        if (visible.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new MenuData(language.translateToken(integration.title()), genericContent(integration), List.copyOf(visible)));
    }

    public List<String> expandBase(Integration integration, List<String> commands) {
        List<String> result = new ArrayList<>(commands.size());
        String base = primaryAlias(integration);
        for (String command : commands) {
            result.add(command.replace("%base%", base));
        }
        return List.copyOf(result);
    }

    private String primaryAlias(Integration integration) {
        Plugin target = Bukkit.getPluginManager().getPlugin(integration.pluginName());
        if (target != null && target.isEnabled()) {
            File configFile = new File(target.getDataFolder(), "config.yml");
            if (configFile.isFile()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                for (String path : integration.aliasPaths()) {
                    List<String> aliases = config.getStringList(path);
                    if (!aliases.isEmpty() && aliases.get(0) != null && !aliases.get(0).isBlank()) {
                        return aliases.get(0);
                    }
                }
            }
        }
        return integration.aliases().isEmpty() ? integration.pluginName().toLowerCase(Locale.ROOT) : integration.aliases().get(0);
    }

    private List<MenuButton> cachedButtons(Integration integration) {
        long now = System.currentTimeMillis();
        CacheEntry entry = cache.get(integration.id());
        if (entry != null && now - entry.loadedAt() <= config.cacheTtlMillis()) {
            return entry.buttons();
        }
        List<MenuButton> scanned = scanButtons(integration);
        cache.put(integration.id(), new CacheEntry(now, scanned));
        return scanned;
    }

    private List<MenuButton> scanButtons(Integration integration) {
        Plugin target = Bukkit.getPluginManager().getPlugin(integration.pluginName());
        if (target == null || !target.isEnabled()) {
            return List.of();
        }
        List<File> files = candidateFiles(target.getDataFolder(), integration.menuFiles());
        List<MenuButton> buttons = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (File file : files) {
            if (!safeFile(target.getDataFolder(), file)) {
                continue;
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            Counter counter = new Counter();
            scanSection(integration, yaml, file.getName(), 0, counter, buttons, seen);
            if (buttons.size() >= config.maxButtonsPerForm()) {
                break;
            }
        }
        return List.copyOf(buttons);
    }

    private List<File> candidateFiles(File dataFolder, List<String> patterns) {
        List<File> files = new ArrayList<>();
        for (String pattern : patterns) {
            if (pattern.contains("*")) {
                files.addAll(glob(dataFolder, pattern));
            } else {
                File file = new File(dataFolder, pattern.replace("/", File.separator));
                if (file.isFile()) {
                    files.add(file);
                }
            }
        }
        files.sort(Comparator.comparing(File::getAbsolutePath));
        if (files.size() > 50) {
            return files.subList(0, 50);
        }
        return files;
    }

    private List<File> glob(File dataFolder, String pattern) {
        String normalized = pattern.replace("\\", "/");
        int star = normalized.indexOf('*');
        String prefix = normalized.substring(0, star);
        String suffix = normalized.substring(star + 1);
        File root = prefix.endsWith("/") ? new File(dataFolder, prefix.substring(0, prefix.length() - 1)) : new File(dataFolder, prefix).getParentFile();
        if (root == null || !root.isDirectory()) {
            return List.of();
        }
        List<File> files = new ArrayList<>();
        File[] children = root.listFiles();
        if (children == null) {
            return List.of();
        }
        for (File child : children) {
            String relative = root.toPath().relativize(child.toPath()).toString().replace("\\", "/");
            if (child.isFile() && relative.endsWith(suffix)) {
                files.add(child);
            }
        }
        return files;
    }

    public boolean safeMenuFile(Integration integration, File file) {
        Plugin target = Bukkit.getPluginManager().getPlugin(integration.pluginName());
        return target != null && safeFile(target.getDataFolder(), file);
    }

    public File pluginDataFile(Integration integration, String relativePath) {
        Plugin target = Bukkit.getPluginManager().getPlugin(integration.pluginName());
        if (target == null || !target.isEnabled()) {
            return null;
        }
        return new File(target.getDataFolder(), relativePath.replace("/", File.separator));
    }

    public int maxButtons() {
        return config.maxButtonsPerForm();
    }

    public Optional<String> displayName(ConfigurationSection section, String... paths) {
        return firstString(section, paths).map(language::stripColor).map(value -> limit(value, 64));
    }

    public String withSummary(String label, List<String> summary) {
        String cleaned = limit(language.stripColor(label), 64);
        if (summary == null || summary.isEmpty()) {
            return cleaned;
        }
        StringBuilder builder = new StringBuilder(cleaned);
        int lines = 0;
        for (String line : summary) {
            if (line == null || line.isBlank()) {
                continue;
            }
            builder.append("\n").append(limit(language.stripColor(line), 72));
            lines++;
            if (lines >= 3) {
                break;
            }
        }
        return builder.toString();
    }

    private boolean safeFile(File dataFolder, File file) {
        try {
            String base = dataFolder.getCanonicalPath();
            String target = file.getCanonicalPath();
            if (!target.startsWith(base + File.separator)) {
                return false;
            }
            if (!file.isFile() || !file.getName().endsWith(".yml")) {
                return false;
            }
            return file.length() <= config.maxMenuFileBytes();
        } catch (Exception exception) {
            owner.getLogger().warning("Skipped unsafe menu file " + file + ": " + exception.getMessage());
            return false;
        }
    }

    private void scanSection(Integration integration, ConfigurationSection section, String path, int depth, Counter counter, List<MenuButton> buttons, Set<String> seen) {
        if (depth > config.maxYamlDepth() || counter.increment() > config.maxYamlNodes() || buttons.size() >= config.maxButtonsPerForm()) {
            return;
        }
        createButton(integration, section, path, depth).ifPresent(button -> {
            String key = button.text().toLowerCase(Locale.ROOT) + "|" + button.commands();
            if (seen.add(key)) {
                buttons.add(button);
            }
        });
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof ConfigurationSection child) {
                scanSection(integration, child, path + "." + key, depth + 1, counter, buttons, seen);
            }
        }
    }

    private Optional<MenuButton> createButton(Integration integration, ConfigurationSection section, String path, int depth) {
        List<String> commands = readCommands(integration, section);
        if (commands.isEmpty() && (depth == 0 || !looksLikeConceptItem(section))) {
            return Optional.empty();
        }
        if (isFiller(section)) {
            return Optional.empty();
        }
        String text = firstString(section, "name", "display-name", "title", "text", "item.name", "icon.name", "display.name", "claimable.name", "unclaimable.name", "no-permission.name")
                .orElseGet(() -> readablePath(path));
        List<String> summary = description(section);
        text = withSummary(text, summary);
        String permission = firstString(section, "permission", "view-permission", "requirements.permission").orElse(null);
        return Optional.of(new MenuButton(text, commands, permission, summary));
    }

    private boolean looksLikeConceptItem(ConfigurationSection section) {
        return section.contains("rank")
                || section.contains("price")
                || section.contains("currency")
                || section.contains("claimable")
                || section.contains("unclaimable")
                || section.contains("no-permission")
                || section.contains("claim-items")
                || section.contains("claim-commands")
                || section.contains("buy-actions")
                || section.contains("rewards")
                || section.contains("reward")
                || section.contains("warp")
                || section.contains("category")
                || section.contains("mine")
                || section.contains("envoy")
                || section.contains("crate")
                || section.contains("sellwand")
                || section.contains("zone");
    }

    private boolean isFiller(ConfigurationSection section) {
        Optional<String> material = firstString(section, "material", "item", "icon", "display-item", "type");
        if (material.isEmpty()) {
            return false;
        }
        String value = material.get().toUpperCase(Locale.ROOT).replace("MINECRAFT:", "");
        if (!FILLER_MATERIALS.contains(value)) {
            return false;
        }
        Optional<String> label = firstString(section, "name", "display-name", "title", "text", "item.name", "icon.name", "display.name", "claimable.name", "unclaimable.name", "no-permission.name");
        return label.isEmpty() || language.stripColor(label.get()).isBlank();
    }

    private List<String> readCommands(Integration integration, ConfigurationSection section) {
        List<String> commands = new ArrayList<>(ArtillexActionParser.commandsFor(integration, section, null));
        if (commands.size() > 4) {
            return List.copyOf(commands.subList(0, 4));
        }
        return List.copyOf(commands);
    }

    private void readCommandValue(ConfigurationSection section, List<String> commands, String... keys) {
        for (String key : keys) {
            Object value = section.get(key);
            if (value instanceof String string && !string.isBlank()) {
                commands.add(string);
            } else if (value instanceof List<?> list) {
                for (Object entry : list) {
                    if (entry != null && !String.valueOf(entry).isBlank()) {
                        commands.add(String.valueOf(entry));
                    }
                }
            } else if (value instanceof ConfigurationSection child) {
                for (String childKey : child.getKeys(false)) {
                    Object childValue = child.get(childKey);
                    if (childValue instanceof String string && !string.isBlank()) {
                        commands.add(string);
                    } else if (childValue instanceof List<?> list) {
                        for (Object entry : list) {
                            if (entry != null && !String.valueOf(entry).isBlank()) {
                                commands.add(String.valueOf(entry));
                            }
                        }
                    }
                }
            }
        }
    }

    private Optional<String> firstString(ConfigurationSection section, String... paths) {
        for (String path : paths) {
            String value = section.getString(path);
            if (value != null && !value.isBlank()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    private List<String> description(ConfigurationSection section) {
        Object lore = section.get("lore");
        if (lore == null) {
            lore = section.get("item.lore");
        }
        if (lore == null) {
            lore = section.get("claimable.lore");
        }
        if (!(lore instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object entry : list) {
            if (entry == null) {
                continue;
            }
            String line = language.stripColor(String.valueOf(entry)).trim();
            if (!line.isBlank()) {
                result.add(line);
            }
            if (result.size() >= 5) {
                break;
            }
        }
        return List.copyOf(result);
    }

    private String genericContent(Integration integration) {
        return switch (integration.id()) {
            case "axgraves" -> "Available grave actions from the AxGraves menu configuration.";
            case "axtrade" -> "Trade controls and available menu actions from AxTrade.";
            case "axvaults" -> "Vault menu controls from AxVaults. Real vault storage remains owned by AxVaults.";
            case "axshulkers" -> "Shulker controls from AxShulkers.";
            case "axafkzone" -> "AFK zone reward and navigation actions.";
            case "axinventoryrestore" -> "Inventory restore menu actions. Restore execution remains owned by AxInventoryRestore.";
            case "axplayerwarps" -> "Player warp controls, categories, search, and navigation actions.";
            case "axsellwands" -> "Sellwand purchase and management actions.";
            case "axrewards" -> "Claimable rewards and reward menu actions.";
            case "axminions" -> "Minion menu actions and controls.";
            case "axenvoys" -> "Envoy reward and navigation actions.";
            case "axsmithing" -> "Smithing menu actions and upgrade controls.";
            case "axmines" -> "Mine list and mine control actions.";
            case "axkills" -> "Kill message and statistics menu actions.";
            case "axcalendar" -> "Calendar days and reward claim actions.";
            default -> "Available actions from the plugin menu configuration.";
        };
    }

    private String readablePath(String path) {
        ArrayDeque<String> parts = new ArrayDeque<>(List.of(path.split("\\.")));
        while (!parts.isEmpty()) {
            String last = parts.removeLast();
            if (!last.isBlank() && !last.endsWith(".yml")) {
                return last.replace('-', ' ').replace('_', ' ');
            }
        }
        return "Menu";
    }

    private static String limit(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + ".";
    }

    private record CacheEntry(long loadedAt, List<MenuButton> buttons) {
    }

    private static final class Counter {
        private int value;

        int increment() {
            return ++value;
        }
    }
}
