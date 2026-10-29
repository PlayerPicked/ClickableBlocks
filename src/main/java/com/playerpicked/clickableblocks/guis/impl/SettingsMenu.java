package com.playerpicked.clickableblocks.guis.impl;

import com.playerpicked.clickableblocks.data.MenuController;
import com.playerpicked.clickableblocks.guis.BlockMenu;
import com.playerpicked.clickableblocks.guis.Menu;
import com.playerpicked.clickableblocks.identifiers.ClickableType;
import com.playerpicked.clickableblocks.identifiers.keys.DialogKeys;
import com.playerpicked.clickableblocks.identifiers.keys.ItemKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MenuKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MessageKeys;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import com.playerpicked.clickableblocks.utils.FormatUtil;
import com.playerpicked.clickableblocks.utils.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public final class SettingsMenu extends BlockMenu {
    private static final Pattern PERMISSION_PATTERN = Pattern.compile("[A-Za-z0-9._*-]{1,128}");

    public SettingsMenu(@NotNull MenuController menuController, @NotNull ClickableBlock block) {
        super(menuController, block);
    }

    @Override
    protected @NotNull MenuKeys getKey() {
        return MenuKeys.SETTINGS;
    }

    @Override
    protected @NotNull Menu reopen() {
        return new SettingsMenu(menuController, block);
    }

    @Override
    public void setMenuItems() {
        inventory.clear();
        String[] info = Placeholders.of(block);

        place(ItemKeys.SETTINGS_PERMISSION, info);
        place(ItemKeys.SETTINGS_COOLDOWN, info);
        place(ItemKeys.SETTINGS_COST, info);
        place(ItemKeys.SETTINGS_MAX_USES, info);
        place(ItemKeys.SETTINGS_RESET_USES, info);
        if (block.getType() == ClickableType.BLOCK) place(ItemKeys.SETTINGS_INTERACTION, "{state}", MessageKeys.state(block.isCancelInteraction()));
        place(ItemKeys.SETTINGS_BACK, info);

        decorate();
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        Player player = player();
        boolean clear = event.isRightClick();

        if (ItemKeys.SETTINGS_PERMISSION.matches(slot)) {
            if (clear) {
                block.setPermission("");
                edited("permission", MessageKeys.PLACEHOLDER_NONE.getMessage());
                return;
            }

            ask(DialogKeys.PERMISSION, block.getPermission(), value -> {
                if (!value.isEmpty() && !PERMISSION_PATTERN.matcher(value).matches()) {
                    MessageKeys.EDITOR_INVALID_PERMISSION.send(player, "{input}", value);
                    return;
                }

                block.setPermission(value);
                edited("permission", value.isEmpty() ? MessageKeys.PLACEHOLDER_NONE.getMessage() : value);
            });
            return;
        }

        if (ItemKeys.SETTINGS_COOLDOWN.matches(slot)) {
            if (clear) {
                block.setCooldown(0);
                edited("cooldown", FormatUtil.duration(0));
                return;
            }

            ask(DialogKeys.COOLDOWN, block.getCooldown() <= 0 ? "" : (block.getCooldown() / 1000d) + "s", value -> {
                long millis = value.isEmpty() ? 0 : FormatUtil.parseDuration(value);
                if (millis < 0) {
                    MessageKeys.EDITOR_INVALID_NUMBER.send(player, "{input}", value);
                    return;
                }

                block.setCooldown(millis);
                edited("cooldown", FormatUtil.duration(millis));
            });
            return;
        }

        if (ItemKeys.SETTINGS_COST.matches(slot)) {
            if (clear) {
                block.setCost(0);
                edited("cost", MessageKeys.PLACEHOLDER_FREE.getMessage());
                return;
            }

            ask(DialogKeys.COST, block.getCost() <= 0 ? "" : FormatUtil.number(block.getCost()), value -> {
                Double cost = value.isEmpty() ? Double.valueOf(0) : FormatUtil.parseDouble(value);
                if (cost == null || cost < 0 || cost > 1_000_000_000_000d) {
                    MessageKeys.EDITOR_INVALID_NUMBER.send(player, "{input}", value);
                    return;
                }

                block.setCost(cost);
                edited("cost", cost <= 0 ? MessageKeys.PLACEHOLDER_FREE.getMessage() : FormatUtil.number(cost));
            });
            return;
        }

        if (ItemKeys.SETTINGS_MAX_USES.matches(slot)) {
            if (clear) {
                block.setMaxUses(0);
                edited("max-uses", MessageKeys.PLACEHOLDER_UNLIMITED.getMessage());
                return;
            }

            ask(DialogKeys.MAX_USES, block.getMaxUses() <= 0 ? "" : String.valueOf(block.getMaxUses()), value -> {
                Integer uses = value.isEmpty() ? Integer.valueOf(0) : FormatUtil.parseInt(value);
                if (uses == null || uses < 0) {
                    MessageKeys.EDITOR_INVALID_NUMBER.send(player, "{input}", value);
                    return;
                }

                block.setMaxUses(uses);
                edited("max-uses", uses <= 0 ? MessageKeys.PLACEHOLDER_UNLIMITED.getMessage() : String.valueOf(uses));
            });
            return;
        }

        if (ItemKeys.SETTINGS_RESET_USES.matches(slot)) {
            if (!event.isShiftClick()) {
                error();
                return;
            }

            plugin.getUsageManager().reset(block.getId());
            plugin.getCooldownManager().clear(block.getId());
            MessageKeys.BLOCK_USES_RESET.send(player, "{id}", block.getId());
            sound("action");
            updateMenuItems();
            return;
        }

        if (ItemKeys.SETTINGS_INTERACTION.matches(slot) && block.getType() == ClickableType.BLOCK) {
            block.setCancelInteraction(!block.isCancelInteraction());
            edited("cancel-interaction", MessageKeys.state(block.isCancelInteraction()));
            return;
        }

        if (ItemKeys.SETTINGS_BACK.matches(slot)) {
            sound("action");
            openLater(new EditorMenu(menuController, block));
        }
    }
}