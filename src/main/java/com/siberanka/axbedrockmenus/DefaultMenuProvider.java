package com.siberanka.axbedrockmenus;

import org.bukkit.entity.Player;

import java.util.Optional;

public final class DefaultMenuProvider implements MenuProvider {
    @Override
    public Optional<MenuData> load(Integration integration, Player player, MenuScanner scanner) {
        return scanner.loadGenericMenu(integration, player);
    }
}
