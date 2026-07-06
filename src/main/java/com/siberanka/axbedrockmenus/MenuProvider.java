package com.siberanka.axbedrockmenus;

import org.bukkit.entity.Player;

import java.util.Optional;

public interface MenuProvider {
    Optional<MenuData> load(Integration integration, Player player, MenuScanner scanner);
}
