package com.siberanka.axbedrockmenus;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class AxRankMenuProvider implements MenuProvider {
    private final LanguageService language;

    public AxRankMenuProvider(LanguageService language) {
        this.language = language;
    }

    @Override
    public Optional<MenuData> load(Integration integration, Player player, MenuScanner scanner) {
        File ranksFile = scanner.pluginDataFile(integration, "ranks.yml");
        if (ranksFile == null || !scanner.safeMenuFile(integration, ranksFile)) {
            return scanner.loadGenericMenu(integration, player);
        }
        YamlConfiguration ranks = YamlConfiguration.loadConfiguration(ranksFile);
        String title = language.stripColor(ranks.getString("title", integration.title()));
        List<MenuButton> buttons = new ArrayList<>();
        List<RankEntry> entries = new ArrayList<>();
        for (String key : ranks.getKeys(false)) {
            ConfigurationSection section = ranks.getConfigurationSection(key);
            if (section == null || !section.contains("rank")) {
                continue;
            }
            entries.add(new RankEntry(
                    section.getInt("slot", 9999),
                    key,
                    section
            ));
        }
        entries.sort(Comparator.comparingInt(RankEntry::slot).thenComparing(RankEntry::key));
        for (RankEntry entry : entries) {
            ConfigurationSection section = entry.section();
            String rank = section.getString("rank", entry.key());
            String price = section.isSet("price") ? String.valueOf(section.get("price")) : "";
            String currency = section.getString("currency", "");
            String server = section.getString("server", "");
            String name = scanner.displayName(section, "item.name").orElse(rank);
            List<String> description = new ArrayList<>();
            description.add("Rank: " + rank);
            if (!price.isBlank()) {
                description.add("Price: " + price + (currency.isBlank() ? "" : " " + currency));
            }
            if (!server.isBlank()) {
                description.add("Server: " + server);
            }
            List<String> commands = List.of();
            buttons.add(new MenuButton(scanner.withSummary(name, description), commands, null, description));
            if (buttons.size() >= scanner.maxButtons()) {
                break;
            }
        }
        if (buttons.isEmpty()) {
            return scanner.loadGenericMenu(integration, player);
        }
        String content = "Ranks are listed directly from AxRankMenu ranks.yml. Purchase actions are not executed by AxBedrockMenus unless a safe owner-plugin API is available.";
        return Optional.of(new MenuData(title, content, List.copyOf(buttons)));
    }

    private record RankEntry(int slot, String key, ConfigurationSection section) {
    }
}
