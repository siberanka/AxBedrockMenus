package com.siberanka.axbedrockmenus;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class FoliaExecutor {
    private final JavaPlugin plugin;

    public FoliaExecutor(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(Player player, Runnable runnable) {
        if (player == null || !player.isOnline() || !plugin.isEnabled()) {
            return;
        }
        if (tryEntityScheduler(player, runnable)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && plugin.isEnabled()) {
                runnable.run();
            }
        });
    }

    private boolean tryEntityScheduler(Player player, Runnable runnable) {
        try {
            Method getScheduler = player.getClass().getMethod("getScheduler");
            Object scheduler = getScheduler.invoke(player);
            Method run = scheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, Consumer.class, Runnable.class);
            Consumer<Object> task = ignored -> {
                if (player.isOnline() && plugin.isEnabled()) {
                    runnable.run();
                }
            };
            run.invoke(scheduler, plugin, task, null);
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }
}
