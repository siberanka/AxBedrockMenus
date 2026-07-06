package com.siberanka.axbedrockmenus;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Map;

public final class AxBedrockMenusPlugin extends JavaPlugin {
    private PluginConfig pluginConfig;
    private LanguageService languageService;
    private IntegrationRegistry integrationRegistry;
    private FloodgateBridge floodgateBridge;
    private FoliaExecutor foliaExecutor;
    private SafeCommandService safeCommandService;
    private MenuScanner menuScanner;
    private ProviderRegistry providerRegistry;
    private FormService formService;

    @Override
    public void onEnable() {
        loadServices(false);
        getServer().getPluginManager().registerEvents(new CommandInterceptListener(this), this);
        var command = getCommand("axbedrockmenus");
        if (command != null) {
            command.setExecutor(this);
        }
        getLogger().info("Enabled AxBedrockMenus 1.0.0 with " + integrationRegistry.enabledCount() + " integrations.");
    }

    @Override
    public void onDisable() {
        if (formService != null) {
            formService.invalidateAll();
        }
    }

    public void reloadPlugin() {
        if (formService != null) {
            formService.invalidateAll();
        }
        loadServices(true);
    }

    private void loadServices(boolean reload) {
        ConfigService configService = new ConfigService(this);
        configService.prepareFiles();
        this.pluginConfig = configService.loadPluginConfig();
        this.languageService = new LanguageService(this, configService, pluginConfig.language());
        this.integrationRegistry = new IntegrationRegistry(this, configService.loadIntegrations());
        this.floodgateBridge = new FloodgateBridge(this);
        this.foliaExecutor = new FoliaExecutor(this);
        this.safeCommandService = new SafeCommandService(this, pluginConfig, languageService, foliaExecutor);
        this.menuScanner = new MenuScanner(this, pluginConfig, languageService);
        this.providerRegistry = new ProviderRegistry(languageService);
        this.formService = new FormService(this, pluginConfig, languageService, integrationRegistry, floodgateBridge, safeCommandService, menuScanner, providerRegistry);
        if (reload) {
            getLogger().info("Configuration reloaded.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("axbedrockmenus.admin")) {
            sender.sendMessage(languageService.message("commands.no-permission"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(languageService.message("commands.usage"));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) {
            reloadPlugin();
            sender.sendMessage(languageService.message("commands.reloaded"));
            return true;
        }
        if (sub.equals("status")) {
            sender.sendMessage(languageService.message("commands.status.header"));
            sender.sendMessage(languageService.message("commands.status.floodgate", Map.of("value", String.valueOf(floodgateBridge.isAvailable()))));
            sender.sendMessage(languageService.message("commands.status.integrations", Map.of("value", String.valueOf(integrationRegistry.availableCount()))));
            sender.sendMessage(languageService.message("commands.status.forms", Map.of("value", String.valueOf(formService.activeSessions()))));
            return true;
        }
        sender.sendMessage(languageService.message("commands.usage"));
        return true;
    }

    public PluginConfig pluginConfig() {
        return pluginConfig;
    }

    public LanguageService languageService() {
        return languageService;
    }

    public IntegrationRegistry integrationRegistry() {
        return integrationRegistry;
    }

    public FloodgateBridge floodgateBridge() {
        return floodgateBridge;
    }

    public FormService formService() {
        return formService;
    }

    public boolean canUse(Player player) {
        return !pluginConfig.requirePermission() || player.hasPermission("axbedrockmenus.use");
    }
}
