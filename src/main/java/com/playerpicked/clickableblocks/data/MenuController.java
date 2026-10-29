package com.playerpicked.clickableblocks.data;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuController {

    private static final ConcurrentHashMap<UUID, MenuController> menuMap = new ConcurrentHashMap<>();

    private final Player owner;

    public MenuController(@NotNull Player owner) {
        this.owner = owner;
    }

    @NotNull
    public Player owner() {
        return owner;
    }

    public static MenuController getMenuUtils(@NotNull Player player) {
        return menuMap.compute(player.getUniqueId(), (uuid, existing) -> existing != null && existing.owner == player ? existing : new MenuController(player));
    }

    public static void remove(@NotNull Player player) {
        menuMap.remove(player.getUniqueId());
    }
}