package com.siberanka.axbedrockmenus;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class SafeCommandService {
    private static final Pattern CONTROL = Pattern.compile("[\\r\\n\\t\\u0000]");
    private static final Pattern UNRESOLVED_PLACEHOLDER = Pattern.compile("(%[^%\\s]{1,64}%|\\{[^}\\s]{1,64}})");

    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final LanguageService language;
    private final FoliaExecutor executor;

    public SafeCommandService(JavaPlugin plugin, PluginConfig config, LanguageService language, FoliaExecutor executor) {
        this.plugin = plugin;
        this.config = config;
        this.language = language;
        this.executor = executor;
    }

    public void execute(Player player, MenuButton button) {
        executor.execute(player, () -> {
            if (!player.isOnline()) {
                return;
            }
            List<CommandAction> actions = new ArrayList<>();
            for (String raw : button.commands()) {
                parse(player, raw).ifPresent(actions::add);
            }
            if (actions.isEmpty()) {
                player.sendMessage(language.message("menu.unsafe-command"));
                return;
            }
            for (CommandAction action : actions) {
                if (action.source() == CommandSource.CONSOLE) {
                    if (!config.allowConsoleActions()) {
                        player.sendMessage(language.message("menu.unsafe-command"));
                        continue;
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), action.command());
                } else {
                    player.performCommand(action.command());
                }
            }
        });
    }

    Optional<CommandAction> parse(Player player, String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String command = raw.trim();
        CommandSource source = CommandSource.PLAYER;
        String lower = command.toLowerCase(Locale.ROOT);
        if (lower.startsWith("[console]")) {
            source = CommandSource.CONSOLE;
            command = command.substring("[console]".length()).trim();
        } else if (lower.startsWith("console:")) {
            source = CommandSource.CONSOLE;
            command = command.substring("console:".length()).trim();
        } else if (lower.startsWith("[player]")) {
            command = command.substring("[player]".length()).trim();
        } else if (lower.startsWith("player:")) {
            command = command.substring("player:".length()).trim();
        } else if (lower.startsWith("[message]") || lower.startsWith("message:") || lower.startsWith("[close]")) {
            return Optional.empty();
        }
        while (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        command = replacePlaceholders(player, command);
        if (!safe(command)) {
            if (config.debug()) {
                plugin.getLogger().warning("Blocked unsafe command from menu: " + raw);
            }
            return Optional.empty();
        }
        return Optional.of(new CommandAction(source, command));
    }

    private String replacePlaceholders(Player player, String command) {
        return command
                .replace("{player}", player.getName())
                .replace("%player%", player.getName())
                .replace("%player_name%", player.getName())
                .replace("<player>", player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("{world}", player.getWorld().getName())
                .replace("%world%", player.getWorld().getName());
    }

    private boolean safe(String command) {
        if (command.isBlank() || command.length() > config.maxCommandLength()) {
            return false;
        }
        if (CONTROL.matcher(command).find() || UNRESOLVED_PLACEHOLDER.matcher(command).find()) {
            return false;
        }
        String lower = command.toLowerCase(Locale.ROOT);
        for (String blocked : config.blockedSubstrings()) {
            if (!blocked.isBlank() && lower.contains(blocked.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        String root = lower.split("\\s+", 2)[0];
        for (String blockedRoot : config.blockedRoots()) {
            if (root.equals(blockedRoot)) {
                return false;
            }
        }
        return true;
    }

    enum CommandSource {
        PLAYER,
        CONSOLE
    }

    record CommandAction(CommandSource source, String command) {
    }
}
