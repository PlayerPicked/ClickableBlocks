package com.playerpicked.clickableblocks.guis.impl;

import com.playerpicked.clickableblocks.action.ParsedAction;
import com.playerpicked.clickableblocks.data.MenuController;
import com.playerpicked.clickableblocks.guis.BlockBoundMenu;
import com.playerpicked.clickableblocks.guis.Menu;
import com.playerpicked.clickableblocks.guis.PaginatedMenu;
import com.playerpicked.clickableblocks.identifiers.ActionType;
import com.playerpicked.clickableblocks.identifiers.ClickTrigger;
import com.playerpicked.clickableblocks.identifiers.keys.DialogKeys;
import com.playerpicked.clickableblocks.identifiers.keys.ItemKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MenuKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MessageKeys;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public final class ActionsMenu extends PaginatedMenu implements BlockBoundMenu {
    private final ClickableBlock block;
    private final ClickTrigger trigger;

    private List<String> rendered = List.of();
    private List<Integer> contentSlots = List.of();

    public ActionsMenu(@NotNull MenuController menuController, @NotNull ClickableBlock block, @NotNull ClickTrigger trigger) {
        this(menuController, block, trigger, 0);
    }

    private ActionsMenu(@NotNull MenuController menuController, @NotNull ClickableBlock block, @NotNull ClickTrigger trigger, int page) {
        super(menuController);
        this.block = block;
        this.trigger = trigger;
        this.page = page;
    }

    @Override
    protected @NotNull MenuKeys getKey() {
        return MenuKeys.ACTIONS;
    }

    @Override
    public @NotNull ClickableBlock getBlock() {
        return block;
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

        contentSlots = getKey().getContentSlots();
        rendered = List.copyOf(block.getActions(trigger));
        clampPage(rendered.size(), contentSlots.size());

        String triggerName = MessageKeys.getTriggerName(trigger);
        int start = page * contentSlots.size();

        for (int index = 0; index < contentSlots.size() && start + index < rendered.size(); index++) {
            String line = rendered.get(start + index);
            ParsedAction parsed = ParsedAction.parse(line);

            ItemStack item = ItemKeys.ACTIONS_ENTRY.getItem(
                    "{index}", String.valueOf(start + index + 1),
                    "{action}", line,
                    "{type}", parsed == null ? "?" : parsed.type().getKey(),
                    "{trigger}", triggerName);

            if (item != null) inventory.setItem(contentSlots.get(index), item);
        }

        String[] common = {
                "{trigger}", triggerName,
                "{count}", String.valueOf(rendered.size()),
                "{max}", String.valueOf(ClickableBlock.MAX_ACTIONS),
                "{page}", String.valueOf(page + 1),
                "{pages}", String.valueOf(maxPage(rendered.size(), contentSlots.size()) + 1),
                "{types}", ActionType.keys()
        };

        if (rendered.isEmpty()) place(ItemKeys.ACTIONS_EMPTY, common);
        if (page > 0) place(ItemKeys.ACTIONS_PREVIOUS, common);
        if (page < maxPage(rendered.size(), contentSlots.size())) place(ItemKeys.ACTIONS_NEXT, common);

        place(ItemKeys.ACTIONS_ADD, common);
        place(ItemKeys.ACTIONS_HELP, common);
        place(ItemKeys.ACTIONS_BACK, common);

        decorate();
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        Player player = player();

        if (ItemKeys.ACTIONS_BACK.matches(slot)) {
            sound("action");
            openLater(new TriggersMenu(menuController, block));
            return;
        }

        if (ItemKeys.ACTIONS_PREVIOUS.matches(slot) && page > 0) {
            page--;
            sound("page");
            updateMenuItems();
            return;
        }

        if (ItemKeys.ACTIONS_NEXT.matches(slot) && page < maxPage(rendered.size(), contentSlots.size())) {
            page++;
            sound("page");
            updateMenuItems();
            return;
        }

        if (ItemKeys.ACTIONS_HELP.matches(slot)) {
            sound("action");
            MessageKeys.EDITOR_ACTION_HELP.sendList(player, "{types}", ActionType.keys());
            return;
        }

        if (ItemKeys.ACTIONS_ADD.matches(slot)) {
            if (block.getActions(trigger).size() >= ClickableBlock.MAX_ACTIONS) {
                MessageKeys.EDITOR_ACTION_LIMIT.send(player, "{max}", String.valueOf(ClickableBlock.MAX_ACTIONS));
                sound("error");
                return;
            }

            ask(DialogKeys.ACTION_ADD, "", value -> {
                String normalized = validate(player, value);
                if (normalized == null) return;

                List<String> actions = block.getActions(trigger);
                if (actions.size() >= ClickableBlock.MAX_ACTIONS) {
                    MessageKeys.EDITOR_ACTION_LIMIT.send(player, "{max}", String.valueOf(ClickableBlock.MAX_ACTIONS));
                    return;
                }

                actions.add(normalized);
                page = maxPage(actions.size(), Math.max(1, contentSlots.size()));
                changed();
                MessageKeys.EDITOR_ACTION_ADDED.send(player, "{action}", normalized, "{trigger}", MessageKeys.getTriggerName(trigger));
            });
            return;
        }

        int position = contentSlots.indexOf(slot);
        if (position < 0) return;

        int index = page * contentSlots.size() + position;
        if (index >= rendered.size()) return;

        String expected = rendered.get(index);
        List<String> actions = block.getActions(trigger);

        if (index >= actions.size() || !actions.get(index).equals(expected)) {
            sound("error");
            updateMenuItems();
            return;
        }

        if (event.isShiftClick()) {
            int target = event.isLeftClick() ? index - 1 : index + 1;
            if (target < 0 || target >= actions.size()) {
                sound("error");
                return;
            }

            String other = actions.get(target);
            actions.set(target, expected);
            actions.set(index, other);

            if (target / contentSlots.size() != page) page = target / contentSlots.size();
            changed();
            sound("action");
            updateMenuItems();
            return;
        }

        if (event.isRightClick()) {
            actions.remove(index);
            changed();
            MessageKeys.EDITOR_ACTION_REMOVED.send(player, "{action}", expected);
            sound("action");
            updateMenuItems();
            return;
        }

        ask(DialogKeys.ACTION_EDIT, expected, value -> {
            if (value.isEmpty() || value.equals(expected)) return;

            String normalized = validate(player, value);
            if (normalized == null) return;

            int current = actions.indexOf(expected);
            if (current < 0) return;

            actions.set(current, normalized);
            changed();
            MessageKeys.EDITOR_ACTION_UPDATED.send(player, "{action}", normalized);
        });
    }

    @Nullable
    private String validate(@NotNull Player player, @NotNull String value) {
        if (value.isEmpty()) return null;

        String normalized = ParsedAction.normalize(value);
        if (normalized == null) {
            MessageKeys.EDITOR_INVALID_ACTION.send(player, "{input}", value, "{types}", ActionType.keys());
            sound("error");
        }

        return normalized;
    }

    private void changed() {
        plugin.getEditorService().changed(block);
    }

    private void ask(@NotNull DialogKeys key, @NotNull String initial, @NotNull Consumer<String> onSubmit) {
        plugin.getDialogInputManager().request(player(), key, initial, value -> {
            if (isStale()) {
                MessageKeys.EDITOR_BLOCK_REMOVED.send(player(), "{id}", block.getId());
                return;
            }

            onSubmit.accept(value);
            reopen().open();
        }, () -> {
            if (!isStale()) reopen().open();
        }, "{id}", block.getId(), "{trigger}", MessageKeys.getTriggerName(trigger), "{types}", ActionType.keys());
    }

    @NotNull
    private Menu reopen() {
        return new ActionsMenu(menuController, block, trigger, page);
    }

    @NotNull
    @Override
    public String getMenuName() {
        return getKey().getTitle().replace("{id}", block.getId()).replace("{trigger}", MessageKeys.getTriggerName(trigger));
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