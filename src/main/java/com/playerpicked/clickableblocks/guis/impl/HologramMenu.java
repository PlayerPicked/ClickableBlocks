package com.playerpicked.clickableblocks.guis.impl;

import com.playerpicked.clickableblocks.data.MenuController;
import com.playerpicked.clickableblocks.guis.BlockMenu;
import com.playerpicked.clickableblocks.guis.Menu;
import com.playerpicked.clickableblocks.identifiers.keys.ConfigKeys;
import com.playerpicked.clickableblocks.identifiers.keys.DialogKeys;
import com.playerpicked.clickableblocks.identifiers.keys.ItemKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MenuKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MessageKeys;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import com.playerpicked.clickableblocks.model.HologramSettings;
import com.playerpicked.clickableblocks.utils.FormatUtil;
import org.bukkit.entity.Display;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public final class HologramMenu extends BlockMenu {
    private static final List<String> BILLBOARDS = Arrays.stream(Display.Billboard.values()).map(Enum::name).toList();

    public HologramMenu(@NotNull MenuController menuController, @NotNull ClickableBlock block) {
        super(menuController, block);
    }

    @Override
    protected @NotNull MenuKeys getKey() {
        return MenuKeys.HOLOGRAM;
    }

    @Override
    protected @NotNull Menu reopen() {
        return new HologramMenu(menuController, block);
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

        HologramSettings hologram = block.getHologram();
        List<String> lines = hologram.getLines();

        String[] info = {
                "{id}", block.getId(),
                "{state}", MessageKeys.state(hologram.isEnabled()),
                "{lines}", lines.isEmpty() ? MessageKeys.PLACEHOLDER_NONE.getMessage() : String.join("\n", lines),
                "{count}", String.valueOf(lines.size()),
                "{max}", String.valueOf(HologramSettings.MAX_LINES),
                "{height}", FormatUtil.number(hologram.getHeight()),
                "{background}", MessageKeys.state(hologram.isBackground()),
                "{shadow}", MessageKeys.state(hologram.isShadow()),
                "{billboard}", hologram.getBillboard(),
                "{view_range}", FormatUtil.number(hologram.getViewRange())
        };

        place(ItemKeys.HOLOGRAM_TOGGLE, info);
        place(ItemKeys.HOLOGRAM_LINES, info);
        place(ItemKeys.HOLOGRAM_HEIGHT, info);
        place(ItemKeys.HOLOGRAM_BILLBOARD, info);
        place(ItemKeys.HOLOGRAM_VIEW_RANGE, info);
        place(ItemKeys.HOLOGRAM_BACKGROUND, info);
        place(ItemKeys.HOLOGRAM_SHADOW, info);
        place(ItemKeys.HOLOGRAM_BACK, info);

        decorate();
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        HologramSettings hologram = block.getHologram();

        if (ItemKeys.HOLOGRAM_TOGGLE.matches(slot)) {
            hologram.setEnabled(!hologram.isEnabled());

            if (hologram.isEnabled() && hologram.getLines().isEmpty()) {
                askLines();
                return;
            }

            edited("hologram", MessageKeys.state(hologram.isEnabled()));
            return;
        }

        if (ItemKeys.HOLOGRAM_LINES.matches(slot)) {
            if (event.isRightClick()) {
                hologram.setLines(List.of());
                edited("hologram-lines", MessageKeys.PLACEHOLDER_NONE.getMessage());
                return;
            }

            askLines();
            return;
        }

        if (ItemKeys.HOLOGRAM_HEIGHT.matches(slot)) {
            double step = ConfigKeys.EDITOR_STEP_HOLOGRAM.getDouble(0.05);
            hologram.setHeight(hologram.getHeight() + (event.isRightClick() ? -step : step));
            editedQuietly();
            return;
        }

        if (ItemKeys.HOLOGRAM_BILLBOARD.matches(slot)) {
            hologram.setBillboard(AppearanceMenu.cycle(BILLBOARDS, hologram.getBillboard(), !event.isRightClick()));
            edited("hologram-billboard", hologram.getBillboard());
            return;
        }

        if (ItemKeys.HOLOGRAM_VIEW_RANGE.matches(slot)) {
            float step = (float) ConfigKeys.EDITOR_STEP_VIEW_RANGE.getDouble(0.5);
            hologram.setViewRange(hologram.getViewRange() + (event.isRightClick() ? -step : step));
            editedQuietly();
            return;
        }

        if (ItemKeys.HOLOGRAM_BACKGROUND.matches(slot)) {
            hologram.setBackground(!hologram.isBackground());
            edited("hologram-background", MessageKeys.state(hologram.isBackground()));
            return;
        }

        if (ItemKeys.HOLOGRAM_SHADOW.matches(slot)) {
            hologram.setShadow(!hologram.isShadow());
            edited("hologram-shadow", MessageKeys.state(hologram.isShadow()));
            return;
        }

        if (ItemKeys.HOLOGRAM_BACK.matches(slot)) {
            sound("action");
            openLater(new EditorMenu(menuController, block));
        }
    }

    private void askLines() {
        HologramSettings hologram = block.getHologram();
        String initial = hologram.getLines().isEmpty() ? "&f" + block.getId() : String.join("\n", hologram.getLines());

        ask(DialogKeys.HOLOGRAM_LINES, initial, value -> {
            List<String> lines = Arrays.stream(value.split("\\r?\\n|\\\\n"))
                    .map(String::stripTrailing)
                    .limit(HologramSettings.MAX_LINES)
                    .toList();

            boolean empty = lines.stream().allMatch(String::isBlank);
            hologram.setLines(empty ? List.of() : lines);
            if (!empty) hologram.setEnabled(true);

            edited("hologram-lines", String.valueOf(hologram.getLines().size()));
        }, "{max}", String.valueOf(HologramSettings.MAX_LINES));
    }
}