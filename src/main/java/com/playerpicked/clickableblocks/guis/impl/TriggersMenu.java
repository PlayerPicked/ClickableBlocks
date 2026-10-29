package com.playerpicked.clickableblocks.guis.impl;

import com.playerpicked.clickableblocks.data.MenuController;
import com.playerpicked.clickableblocks.guis.BlockMenu;
import com.playerpicked.clickableblocks.guis.Menu;
import com.playerpicked.clickableblocks.identifiers.ClickTrigger;
import com.playerpicked.clickableblocks.identifiers.keys.ItemKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MenuKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MessageKeys;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public final class TriggersMenu extends BlockMenu {
    private static final int PREVIEW_LINES = 5;

    private static final Map<ClickTrigger, ItemKeys> ITEMS = Map.of(
            ClickTrigger.LEFT, ItemKeys.TRIGGERS_LEFT,
            ClickTrigger.RIGHT, ItemKeys.TRIGGERS_RIGHT,
            ClickTrigger.SHIFT_LEFT, ItemKeys.TRIGGERS_SHIFT_LEFT,
            ClickTrigger.SHIFT_RIGHT, ItemKeys.TRIGGERS_SHIFT_RIGHT,
            ClickTrigger.ANY, ItemKeys.TRIGGERS_ANY);

    public TriggersMenu(@NotNull MenuController menuController, @NotNull ClickableBlock block) {
        super(menuController, block);
    }

    @Override
    protected @NotNull MenuKeys getKey() {
        return MenuKeys.TRIGGERS;
    }

    @Override
    protected @NotNull Menu reopen() {
        return new TriggersMenu(menuController, block);
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

        for (ClickTrigger trigger : ClickTrigger.values()) {
            List<String> actions = block.getActions(trigger);
            String preview = actions.isEmpty()
                    ? MessageKeys.PLACEHOLDER_NONE.getMessage()
                    : String.join("\n", actions.stream().limit(PREVIEW_LINES).toList()) + (actions.size() > PREVIEW_LINES ? "\n..." : "");

            place(ITEMS.get(trigger),
                    "{trigger}", MessageKeys.getTriggerName(trigger),
                    "{count}", String.valueOf(actions.size()),
                    "{preview}", preview);
        }

        place(ItemKeys.TRIGGERS_BACK);
        decorate();
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        for (Map.Entry<ClickTrigger, ItemKeys> entry : ITEMS.entrySet()) {
            if (!entry.getValue().matches(slot)) continue;

            sound("action");
            openLater(new ActionsMenu(menuController, block, entry.getKey()));
            return;
        }

        if (ItemKeys.TRIGGERS_BACK.matches(slot)) {
            sound("action");
            openLater(new EditorMenu(menuController, block));
        }
    }
}