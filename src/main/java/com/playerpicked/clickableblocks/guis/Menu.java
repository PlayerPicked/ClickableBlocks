package com.playerpicked.clickableblocks.guis;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.data.MenuController;
import com.playerpicked.clickableblocks.guis.service.MenuService;
import com.playerpicked.clickableblocks.identifiers.keys.ItemKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MenuKeys;
import com.playerpicked.clickableblocks.item.ItemFactory;
import com.playerpicked.clickableblocks.processor.MenuProcessor;
import com.playerpicked.clickableblocks.processor.MessageProcessor;
import com.playerpicked.clickableblocks.utils.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@SuppressWarnings("deprecation")
public abstract class Menu implements InventoryHolder {
    protected final ClickableBlocks plugin = ClickableBlocks.getInstance();

    public MenuController menuController;
    protected Inventory inventory;

    private MenuProcessor updater;

    public Menu(@NotNull MenuController menuController) {
        this.menuController = menuController;
    }

    public abstract void handleMenu(final @NotNull InventoryClickEvent event);

    public abstract void setMenuItems();

    public abstract String getMenuName();

    public abstract int getSlots();

    public abstract int getMenuTick();

    @NotNull
    protected abstract MenuKeys getKey();

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        Player owner = menuController.owner();
        UUID uuid = owner.getUniqueId();

        if (!owner.isOnline()) return;
        if (!MenuService.canOpen(uuid)) return;

        inventory = Bukkit.createInventory(this, getSlots(), MessageProcessor.process(getMenuName()));

        this.setMenuItems();

        owner.openInventory(inventory);
        sound("open");

        int tick = Math.max(0, getMenuTick());
        if (tick > 0) {
            if (updater == null) updater = new MenuProcessor(this);
            updater.start(tick);
        }
    }

    public void close() {
        if (updater != null) {
            updater.stop();
            updater = null;
        }

        MenuController.remove(menuController.owner());
        inventory = null;
    }

    public void updateMenuItems() {
        if (inventory == null) return;

        setMenuItems();
        menuController.owner().updateInventory();
    }

    @NotNull
    protected Player player() {
        return menuController.owner();
    }

    protected void openLater(@NotNull Menu menu) {
        Player owner = player();
        ClickableBlocks.getScheduler().runTask(owner, () -> {
            if (owner.isOnline()) menu.open();
        });
    }

    protected void closeLater() {
        Player owner = player();
        ClickableBlocks.getScheduler().runTask(owner, owner::closeInventory);
    }

    protected void place(@NotNull ItemKeys key, @NotNull String... replacements) {
        ItemStack item = key.getItem(replacements);
        if (item == null) return;

        for (int slot : key.getSlots()) {
            if (slot >= 0 && slot < inventory.getSize()) inventory.setItem(slot, item.clone());
        }
    }

    protected void decorate() {
        ItemFactory.setItemsForMenu(getKey().getDecorationPath(), inventory);
    }

    protected void sound(@NotNull String name) {
        SoundUtil.play(player(), getKey().getSound(name));
    }
}