package com.siberanka.axbedrockmenus;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class CommandInterceptListener implements Listener {
    private final AxBedrockMenusPlugin plugin;

    public CommandInterceptListener(AxBedrockMenusPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        plugin.integrationRegistry().findByCommand(event.getMessage()).ifPresent(integration -> {
            if (plugin.pluginConfig().interceptBedrockOnly() && !plugin.floodgateBridge().isFloodgatePlayer(event.getPlayer().getUniqueId())) {
                return;
            }
            if (!plugin.canUse(event.getPlayer())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(plugin.languageService().message("commands.no-permission"));
                return;
            }
            event.setCancelled(true);
            plugin.formService().open(event.getPlayer(), integration);
        });
    }
}
