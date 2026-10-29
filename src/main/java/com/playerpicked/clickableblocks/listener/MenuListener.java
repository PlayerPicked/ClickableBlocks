package com.playerpicked.clickableblocks.listener;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.data.MenuController;
import com.playerpicked.clickableblocks.guis.BlockBoundMenu;
import com.playerpicked.clickableblocks.guis.Menu;
import com.playerpicked.clickableblocks.guis.service.MenuService;
import com.playerpicked.clickableblocks.identifiers.keys.MessageKeys;
import com.playerpicked.clickableblocks.utils.LoggerUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(final @NotNull InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder(false);
        if (!(holder instanceof Menu menu)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (holder instanceof BlockBoundMenu bound && bound.isStale()) {
            MessageKeys.EDITOR_BLOCK_REMOVED.send(player, "{id}", bound.getBlock().getId());
            ClickableBlocks.getScheduler().runTask(player, player::closeInventory);
            return;
        }

        try {
            menu.handleMenu(event);
        } catch (Exception exception) {
            LoggerUtils.error("A menu click failed for {}", player.getName(), exception);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(final @NotNull InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof Menu) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(final @NotNull InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) instanceof Menu menu) menu.close();
    }

    @EventHandler
    public void onPlayerQuit(final @NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        MenuController.remove(player);
        MenuService.clear(player.getUniqueId());
        ClickableBlocks.getInstance().getCooldownManager().forget(player.getUniqueId());
    }
}