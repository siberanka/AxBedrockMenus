package com.siberanka.axbedrockmenus;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
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
    private final ProviderRegistry providers;
    private final RateLimiter openLimiter;
    private final RateLimiter clickLimiter;
    private final AtomicLong sequence = new AtomicLong();
    private final Map<UUID, FormSession> active = new ConcurrentHashMap<>();

    public FormService(JavaPlugin plugin, PluginConfig config, LanguageService language, IntegrationRegistry integrations, FloodgateBridge floodgate, SafeCommandService commands, MenuScanner scanner, ProviderRegistry providers) {
        this.plugin = plugin;
        this.config = config;
        this.language = language;
        this.integrations = integrations;
        this.floodgate = floodgate;
        this.commands = commands;
        this.scanner = scanner;
        this.providers = providers;
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
        Optional<MenuData> data = providers.providerFor(integration).load(integration, player, scanner);
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
                menu.content(),
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
        MenuButton button = menu.buttons().get(clicked);
        if (button.commands().isEmpty()) {
            player.sendMessage(language.message("menu.info-only"));
            return;
        }
        if (handleInternal(player, button)) {
            return;
        }
        commands.execute(player, button);
    }

    private boolean handleInternal(Player player, MenuButton button) {
        String first = button.commands().get(0);
        if (first.startsWith("abm:select-player:")) {
            openPlayerSelector(player, first.substring("abm:select-player:".length()).trim());
            return true;
        }
        return false;
    }

    private void openPlayerSelector(Player player, String template) {
        if (template.isBlank() || !player.isOnline() || !plugin.isEnabled()) {
            return;
        }
        List<Player> targets = new ArrayList<>(Bukkit.getOnlinePlayers().stream()
                .filter(target -> target.isOnline() && !target.getUniqueId().equals(player.getUniqueId()))
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(Math.max(1, config.maxButtonsPerForm()))
                .toList());
        if (targets.isEmpty()) {
            player.sendMessage(language.message("menu.no-targets"));
            return;
        }
        List<String> labels = targets.stream().map(Player::getName).toList();
        long id = sequence.incrementAndGet();
        AtomicBoolean consumed = new AtomicBoolean();
        active.put(player.getUniqueId(), new FormSession(id, "player-selector"));
        boolean sent = floodgate.sendSimpleForm(
                player.getUniqueId(),
                language.plain("menu.select-player-title"),
                "",
                labels,
                clicked -> {
                    if (!consumed.compareAndSet(false, true)) {
                        return;
                    }
                    FormSession expected = active.remove(player.getUniqueId());
                    if (expected == null || expected.id() != id || clicked < 0 || clicked >= targets.size()) {
                        return;
                    }
                    Player source = Bukkit.getPlayer(player.getUniqueId());
                    Player target = targets.get(clicked);
                    if (source == null || !source.isOnline() || !target.isOnline() || !plugin.isEnabled()) {
                        return;
                    }
                    if (!clickLimiter.tryAcquire(source.getUniqueId())) {
                        source.sendMessage(language.message("menu.rate-limited"));
                        return;
                    }
                    String command = template.replace("%target%", target.getName());
                    commands.execute(source, new MenuButton(target.getName(), List.of(command), null));
                },
                () -> active.remove(player.getUniqueId(), new FormSession(id, "player-selector"))
        );
        if (!sent) {
            active.remove(player.getUniqueId(), new FormSession(id, "player-selector"));
            player.sendMessage(language.message("menu.plugin-disabled"));
        }
    }

    private record FormSession(long id, String integrationId) {
    }
}
