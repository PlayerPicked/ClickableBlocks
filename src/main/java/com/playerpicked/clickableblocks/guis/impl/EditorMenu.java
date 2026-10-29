package com.playerpicked.clickableblocks.guis.impl;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.data.MenuController;
import com.playerpicked.clickableblocks.guis.BlockMenu;
import com.playerpicked.clickableblocks.guis.Menu;
import com.playerpicked.clickableblocks.identifiers.ClickTrigger;
import com.playerpicked.clickableblocks.identifiers.ClickableType;
import com.playerpicked.clickableblocks.identifiers.keys.ConfigKeys;
import com.playerpicked.clickableblocks.identifiers.keys.DialogKeys;
import com.playerpicked.clickableblocks.identifiers.keys.ItemKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MenuKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MessageKeys;
import com.playerpicked.clickableblocks.model.BlockPosition;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import com.playerpicked.clickableblocks.utils.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class EditorMenu extends BlockMenu {

    public EditorMenu(@NotNull MenuController menuController, @NotNull ClickableBlock block) {
        super(menuController, block);
    }

    @Override
    protected @NotNull MenuKeys getKey() {
        return MenuKeys.EDITOR;
    }

    @Override
    protected @NotNull Menu reopen() {
        return new EditorMenu(menuController, block);
    }

    @Override
    public void setMenuItems() {
        inventory.clear();
        String[] info = Placeholders.of(block);

        place(ItemKeys.EDITOR_INFO, info);
        place(ItemKeys.EDITOR_STATUS, info);
        place(ItemKeys.EDITOR_ACTIONS, info);
        place(ItemKeys.EDITOR_SETTINGS, info);
        place(ItemKeys.EDITOR_APPEARANCE, info);
        place(ItemKeys.EDITOR_HOLOGRAM, info);
        place(ItemKeys.EDITOR_PARTICLE, info);
        place(ItemKeys.EDITOR_TYPE, info);
        place(ItemKeys.EDITOR_MOVE, info);
        place(ItemKeys.EDITOR_TELEPORT, info);
        place(ItemKeys.EDITOR_TEST, info);
        place(ItemKeys.EDITOR_CLONE, info);
        place(ItemKeys.EDITOR_DELETE, info);
        place(ItemKeys.EDITOR_BACK, info);
        place(ItemKeys.EDITOR_CLOSE, info);

        decorate();
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        Player player = player();

        if (ItemKeys.EDITOR_STATUS.matches(slot)) {
            block.setEnabled(!block.isEnabled());
            edited("enabled", MessageKeys.state(block.isEnabled()));
            return;
        }

        if (ItemKeys.EDITOR_ACTIONS.matches(slot)) {
            sound("action");
            openLater(new TriggersMenu(menuController, block));
            return;
        }

        if (ItemKeys.EDITOR_SETTINGS.matches(slot)) {
            sound("action");
            openLater(new SettingsMenu(menuController, block));
            return;
        }

        if (ItemKeys.EDITOR_APPEARANCE.matches(slot)) {
            sound("action");
            openLater(new AppearanceMenu(menuController, block));
            return;
        }

        if (ItemKeys.EDITOR_HOLOGRAM.matches(slot)) {
            sound("action");
            openLater(new HologramMenu(menuController, block));
            return;
        }

        if (ItemKeys.EDITOR_PARTICLE.matches(slot)) {
            List<String> particles = ConfigKeys.EDITOR_PARTICLES.getList(List.of("NONE"));
            if (particles.isEmpty()) return;

            int index = Math.max(0, particles.indexOf(block.getParticle().toUpperCase()));
            int next = Math.floorMod(index + (event.isRightClick() ? -1 : 1), particles.size());
            block.setParticle(particles.get(next).toUpperCase());
            edited("particle", block.getParticle());
            return;
        }

        if (ItemKeys.EDITOR_TYPE.matches(slot)) {
            ClickableType type = event.isRightClick() ? block.getType().previous() : block.getType().next();
            if (plugin.getEditorService().changeType(player, block, type)) {
                sound("action");
                updateMenuItems();
            } else error();
            return;
        }

        if (ItemKeys.EDITOR_MOVE.matches(slot)) {
            BlockPosition target = plugin.getEditorService().resolveTarget(player, block.getType());
            if (target == null) {
                MessageKeys.BLOCK_NO_TARGET.send(player);
                error();
                return;
            }

            if (plugin.getEditorService().move(player, block, target)) {
                sound("action");
                updateMenuItems();
            } else error();
            return;
        }

        if (ItemKeys.EDITOR_TELEPORT.matches(slot)) {
            sound("action");
            closeLater();
            plugin.getEditorService().teleport(player, block);
            return;
        }

        if (ItemKeys.EDITOR_TEST.matches(slot)) {
            ClickTrigger trigger = ClickTrigger.of(event.isLeftClick(), event.isShiftClick());

            if (block.resolveActions(trigger).isEmpty()) {
                MessageKeys.BLOCK_NO_ACTIONS.send(player, "{trigger}", MessageKeys.getTriggerName(trigger));
                error();
                return;
            }

            sound("action");
            player.closeInventory();
            ClickableBlocks.getScheduler().runTask(player, () -> {
                if (!player.isOnline() || isStale()) return;
                plugin.getClickService().forceRun(player, block, trigger);
                MessageKeys.BLOCK_TESTED.send(player, "{id}", block.getId(), "{trigger}", MessageKeys.getTriggerName(trigger));
            });
            return;
        }

        if (ItemKeys.EDITOR_CLONE.matches(slot)) {
            String suggestion = plugin.getBlockManager().nextFreeId(block.getId());

            plugin.getDialogInputManager().request(player, DialogKeys.CLONE, suggestion, value -> {
                if (isStale()) return;

                plugin.getEditorService().duplicate(player, block, value.isEmpty() ? suggestion : value)
                        .ifPresentOrElse(copy -> new EditorMenu(MenuController.getMenuUtils(player), copy).open(), () -> reopen().open());
            }, () -> {
                if (!isStale()) reopen().open();
            }, "{id}", block.getId());
            return;
        }

        if (ItemKeys.EDITOR_DELETE.matches(slot)) {
            sound("action");
            openLater(new ConfirmMenu(menuController,
                    MessageKeys.getSettingName("delete").replace("{id}", block.getId()),
                    () -> {
                        plugin.getEditorService().delete(block);
                        MessageKeys.BLOCK_DELETED.send(player, "{id}", block.getId());
                        new ListMenu(MenuController.getMenuUtils(player)).open();
                    },
                    () -> new EditorMenu(MenuController.getMenuUtils(player), block).open()));
            return;
        }

        if (ItemKeys.EDITOR_BACK.matches(slot)) {
            sound("action");
            openLater(new ListMenu(menuController));
            return;
        }

        if (ItemKeys.EDITOR_CLOSE.matches(slot)) closeLater();
    }
}