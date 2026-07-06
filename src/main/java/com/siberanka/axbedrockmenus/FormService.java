package com.siberanka.axbedrockmenus;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class FormService {
    private final JavaPlugin plugin;
    private final PluginConfig config;
    private final LanguageService language;
    private final IntegrationRegistry integrations;
    private final FloodgateBridge floodgate;
    private final SafeCommandService commands;
    private final MenuScanner scanner;
    private final RateLimiter openLimiter;
    private final RateLimiter clickLimiter;
    private final AtomicLong sequence = new AtomicLong();
    private final Map<UUID, FormSession> active = new ConcurrentHashMap<>();

    public FormService(JavaPlugin plugin, PluginConfig config, LanguageService language, IntegrationRegistry integrations, FloodgateBridge floodgate, SafeCommandService commands, MenuScanner scanner) {
        this.plugin = plugin;
        this.config = config;
        this.language = language;
        this.integrations = integrations;
        this.floodgate = floodgate;
        this.commands = commands;
        this.scanner = scanner;
        this.openLimiter = new RateLimiter(config.menuOpenLimit());
        this.clickLimiter = new RateLimiter(config.buttonClickLimit());
    }

    public void open(Player player, Integration integration) {
        if (!plugin.isEnabled() || !player.isOnline()) {
            return;
        }
        if (!integrations.isPluginAvailable(integration)) {
            player.sendMessage(language.message("menu.plugin-disabled"));
            return;
        }
        if (!openLimiter.tryAcquire(player.getUniqueId())) {
            player.sendMessage(language.message("menu.rate-limited"));
            return;
        }
        Optional<MenuData> data = scanner.loadMenu(integration, player);
        if (data.isEmpty()) {
            player.sendMessage(language.message("menu.no-buttons"));
            return;
        }
        MenuData menu = data.get();
        List<String> labels = new ArrayList<>(menu.buttons().size());
        for (MenuButton button : menu.buttons()) {
            labels.add(language.translateToken(button.text()));
        }
        long id = sequence.incrementAndGet();
        AtomicBoolean consumed = new AtomicBoolean();
        active.put(player.getUniqueId(), new FormSession(id, integration.id()));
        boolean sent = floodgate.sendSimpleForm(
                player.getUniqueId(),
                menu.title(),
                "",
                labels,
                clicked -> handleClick(player.getUniqueId(), id, menu, clicked, consumed),
                () -> active.remove(player.getUniqueId(), new FormSession(id, integration.id()))
        );
        if (!sent) {
            active.remove(player.getUniqueId(), new FormSession(id, integration.id()));
            player.sendMessage(language.message("menu.plugin-disabled"));
        }
    }

    public void invalidateAll() {
        active.clear();
        sequence.incrementAndGet();
    }

    public int activeSessions() {
        return active.size();
    }

    private void handleClick(UUID uuid, long sessionId, MenuData menu, int clicked, AtomicBoolean consumed) {
        if (!consumed.compareAndSet(false, true)) {
            return;
        }
        FormSession expected = active.remove(uuid);
        if (expected == null || expected.id() != sessionId) {
            return;
        }
        if (clicked < 0 || clicked >= menu.buttons().size()) {
            return;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline() || !plugin.isEnabled()) {
            return;
        }
        if (!clickLimiter.tryAcquire(uuid)) {
            player.sendMessage(language.message("menu.rate-limited"));
            return;
        }
        commands.execute(player, menu.buttons().get(clicked));
    }

    private record FormSession(long id, String integrationId) {
    }
}
