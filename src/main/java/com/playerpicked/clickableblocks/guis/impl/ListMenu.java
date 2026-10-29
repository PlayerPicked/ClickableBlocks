package com.playerpicked.clickableblocks.guis.impl;

import com.playerpicked.clickableblocks.data.MenuController;
import com.playerpicked.clickableblocks.guis.PaginatedMenu;
import com.playerpicked.clickableblocks.identifiers.ClickableType;
import com.playerpicked.clickableblocks.identifiers.keys.ItemKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MenuKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MessageKeys;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import com.playerpicked.clickableblocks.utils.Placeholders;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ListMenu extends PaginatedMenu {
    private List<ClickableBlock> rendered = List.of();
    private List<Integer> contentSlots = List.of();

    public ListMenu(@NotNull MenuController menuController) {
        super(menuController);
    }

    @Override
    protected @NotNull MenuKeys getKey() {
        return MenuKeys.LIST;
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

        contentSlots = getKey().getContentSlots();
        rendered = plugin.getBlockManager().sorted();
        clampPage(rendered.size(), contentSlots.size());

        int start = page * contentSlots.size();
        for (int index = 0; index < contentSlots.size() && start + index < rendered.size(); index++) {
            ClickableBlock block = rendered.get(start + index);

            ItemStack item = ItemKeys.LIST_ENTRY.getItem(Placeholders.of(block));
            if (item == null) continue;

            Material icon = icon(block);
            if (icon != null) item = item.withType(icon);

            inventory.setItem(contentSlots.get(index), item);
        }

        String[] common = {
                "{count}", String.valueOf(rendered.size()),
                "{page}", String.valueOf(page + 1),
                "{pages}", String.valueOf(maxPage(rendered.size(), contentSlots.size()) + 1)
        };

        if (rendered.isEmpty()) place(ItemKeys.LIST_EMPTY, common);
        if (page > 0) place(ItemKeys.LIST_PREVIOUS, common);
        if (page < maxPage(rendered.size(), contentSlots.size())) place(ItemKeys.LIST_NEXT, common);

        place(ItemKeys.LIST_WAND, common);
        place(ItemKeys.LIST_CLOSE, common);

        decorate();
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        Player player = player();

        if (ItemKeys.LIST_CLOSE.matches(slot)) {
            closeLater();
            return;
        }

        if (ItemKeys.LIST_WAND.matches(slot)) {
            player.getInventory().addItem(plugin.getEditorService().createWand()).values()
                    .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
            MessageKeys.WAND_RECEIVED.send(player);
            sound("action");
            return;
        }

        if (ItemKeys.LIST_PREVIOUS.matches(slot) && page > 0) {
            page--;
            sound("page");
            updateMenuItems();
            return;
        }

        if (ItemKeys.LIST_NEXT.matches(slot) && page < maxPage(rendered.size(), contentSlots.size())) {
            page++;
            sound("page");
            updateMenuItems();
            return;
        }

        int position = contentSlots.indexOf(slot);
        if (position < 0) return;

        int index = page * contentSlots.size() + position;
        if (index >= rendered.size()) return;

        ClickableBlock block = rendered.get(index);
        if (plugin.getBlockManager().get(block.getId()).orElse(null) != block) {
            sound("error");
            updateMenuItems();
            return;
        }

        if (event.isShiftClick() && event.isRightClick()) {
            block.setEnabled(!block.isEnabled());
            plugin.getEditorService().changed(block);
            (block.isEnabled() ? MessageKeys.BLOCK_ENABLED : MessageKeys.BLOCK_DISABLED).send(player, "{id}", block.getId());
            sound("action");
            updateMenuItems();
            return;
        }

        if (event.isRightClick()) {
            sound("action");
            closeLater();
            plugin.getEditorService().teleport(player, block);
            return;
        }

        sound("action");
        openLater(new EditorMenu(menuController, block));
    }

    @Nullable
    private Material icon(@NotNull ClickableBlock block) {
        Material material;

        if (block.getType() == ClickableType.BLOCK) {
            Block physical = block.getPosition().isLoaded() ? block.getPosition().getBlock() : null;
            material = physical == null ? null : physical.getType();
        } else material = block.getDisplay().getMaterial();

        return material != null && material.isItem() && !material.isAir() ? material : null;
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