package com.playerpicked.clickableblocks.guis.impl;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.data.MenuController;
import com.playerpicked.clickableblocks.guis.Menu;
import com.playerpicked.clickableblocks.identifiers.keys.ItemKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MenuKeys;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ConfirmMenu extends Menu {
    private final String question;
    private final Runnable onConfirm;
    private final Runnable onCancel;
    private final AtomicBoolean answered = new AtomicBoolean(false);

    public ConfirmMenu(@NotNull MenuController menuController, @NotNull String question, @NotNull Runnable onConfirm, @NotNull Runnable onCancel) {
        super(menuController);
        this.question = question;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    protected @NotNull MenuKeys getKey() {
        return MenuKeys.CONFIRM;
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

        place(ItemKeys.CONFIRM_INFO, "{question}", question);
        place(ItemKeys.CONFIRM_ACCEPT, "{question}", question);
        place(ItemKeys.CONFIRM_CANCEL, "{question}", question);

        decorate();
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        boolean accept = ItemKeys.CONFIRM_ACCEPT.matches(slot);
        boolean cancel = ItemKeys.CONFIRM_CANCEL.matches(slot);

        if (!accept && !cancel) return;
        if (!answered.compareAndSet(false, true)) return;

        sound(accept ? "confirm" : "action");

        Player player = player();
        ClickableBlocks.getScheduler().runTask(player, () -> {
            if (!player.isOnline()) return;
            (accept ? onConfirm : onCancel).run();
        });
    }

    @NotNull
    @Override
    public String getMenuName() {
        return getKey().getTitle();
    }

    @Override
    public int getSlots() {
        return getKey().getSize();
    }

    @Override
    public int getMenuTick() {
        return getKey().getTick();
    }
}