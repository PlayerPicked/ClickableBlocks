package com.playerpicked.clickableblocks.guis.impl;

import com.playerpicked.clickableblocks.data.MenuController;
import com.playerpicked.clickableblocks.guis.Menu;
import com.playerpicked.clickableblocks.identifiers.ClickableType;
import com.playerpicked.clickableblocks.identifiers.keys.DialogKeys;
import com.playerpicked.clickableblocks.identifiers.keys.ItemKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MenuKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MessageKeys;
import com.playerpicked.clickableblocks.model.BlockPosition;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

public final class CreateMenu extends Menu {
    private final BlockPosition clicked;
    private final BlockPosition face;

    public CreateMenu(@NotNull MenuController menuController, @NotNull BlockPosition clicked, @NotNull BlockPosition face) {
        super(menuController);
        this.clicked = clicked;
        this.face = face;
    }

    @Override
    protected @NotNull MenuKeys getKey() {
        return MenuKeys.CREATE;
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

        String[] info = {"{location}", clicked.format()};

        place(ItemKeys.CREATE_INFO, info);
        if (!plugin.getBlockManager().isProtected(clicked)) place(ItemKeys.CREATE_BLOCK, info);
        place(ItemKeys.CREATE_ITEM_DISPLAY, info);
        place(ItemKeys.CREATE_BLOCK_DISPLAY, info);
        place(ItemKeys.CREATE_CLOSE, info);

        decorate();
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        if (ItemKeys.CREATE_CLOSE.matches(slot)) {
            closeLater();
            return;
        }

        ClickableType type = null;
        if (ItemKeys.CREATE_BLOCK.matches(slot) && !plugin.getBlockManager().isProtected(clicked)) type = ClickableType.BLOCK;
        else if (ItemKeys.CREATE_ITEM_DISPLAY.matches(slot)) type = ClickableType.ITEM_DISPLAY;
        else if (ItemKeys.CREATE_BLOCK_DISPLAY.matches(slot)) type = ClickableType.BLOCK_DISPLAY;

        if (type == null) return;

        sound("action");

        Player player = player();
        ClickableType chosen = type;
        BlockPosition position = chosen == ClickableType.BLOCK ? clicked : face;
        String suggestion = plugin.getBlockManager().nextFreeId(chosen == ClickableType.BLOCK ? "block" : "display");

        plugin.getDialogInputManager().request(player, DialogKeys.CREATE, suggestion, value -> {
            plugin.getEditorService().create(player, value.isEmpty() ? suggestion : value, chosen, position)
                    .ifPresentOrElse(
                            block -> new EditorMenu(MenuController.getMenuUtils(player), block).open(),
                            () -> new CreateMenu(MenuController.getMenuUtils(player), clicked, face).open());
        }, () -> {}, "{type}", MessageKeys.getTypeName(chosen), "{location}", position.format());
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