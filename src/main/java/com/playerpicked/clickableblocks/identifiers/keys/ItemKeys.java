package com.playerpicked.clickableblocks.identifiers.keys;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.item.ItemFactory;
import com.playerpicked.clickableblocks.utils.ItemUtil;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public enum ItemKeys {
    EDITOR_INFO("editor.items.info"),
    EDITOR_STATUS("editor.items.status"),
    EDITOR_ACTIONS("editor.items.actions"),
    EDITOR_SETTINGS("editor.items.settings"),
    EDITOR_APPEARANCE("editor.items.appearance"),
    EDITOR_HOLOGRAM("editor.items.hologram"),
    EDITOR_PARTICLE("editor.items.particle"),
    EDITOR_TYPE("editor.items.type"),
    EDITOR_MOVE("editor.items.move"),
    EDITOR_TELEPORT("editor.items.teleport"),
    EDITOR_TEST("editor.items.test"),
    EDITOR_CLONE("editor.items.clone"),
    EDITOR_DELETE("editor.items.delete"),
    EDITOR_BACK("editor.items.back"),
    EDITOR_CLOSE("editor.items.close"),

    TRIGGERS_LEFT("triggers.items.left"),
    TRIGGERS_RIGHT("triggers.items.right"),
    TRIGGERS_SHIFT_LEFT("triggers.items.shift-left"),
    TRIGGERS_SHIFT_RIGHT("triggers.items.shift-right"),
    TRIGGERS_ANY("triggers.items.any"),
    TRIGGERS_BACK("triggers.items.back"),

    ACTIONS_ENTRY("actions.items.entry"),
    ACTIONS_EMPTY("actions.items.empty"),
    ACTIONS_ADD("actions.items.add"),
    ACTIONS_HELP("actions.items.help"),
    ACTIONS_BACK("actions.items.back"),
    ACTIONS_PREVIOUS("actions.items.previous"),
    ACTIONS_NEXT("actions.items.next"),

    SETTINGS_PERMISSION("settings.items.permission"),
    SETTINGS_COOLDOWN("settings.items.cooldown"),
    SETTINGS_COST("settings.items.cost"),
    SETTINGS_MAX_USES("settings.items.max-uses"),
    SETTINGS_RESET_USES("settings.items.reset-uses"),
    SETTINGS_INTERACTION("settings.items.interaction"),
    SETTINGS_BACK("settings.items.back"),

    APPEARANCE_ITEM("appearance.items.item"),
    APPEARANCE_BLOCK("appearance.items.block"),
    APPEARANCE_SCALE("appearance.items.scale"),
    APPEARANCE_YAW("appearance.items.yaw"),
    APPEARANCE_PITCH("appearance.items.pitch"),
    APPEARANCE_ROLL("appearance.items.roll"),
    APPEARANCE_BILLBOARD("appearance.items.billboard"),
    APPEARANCE_TRANSFORM("appearance.items.transform"),
    APPEARANCE_OFFSET_X("appearance.items.offset-x"),
    APPEARANCE_OFFSET_Y("appearance.items.offset-y"),
    APPEARANCE_OFFSET_Z("appearance.items.offset-z"),
    APPEARANCE_GLOW("appearance.items.glow"),
    APPEARANCE_GLOW_COLOR("appearance.items.glow-color"),
    APPEARANCE_BRIGHTNESS("appearance.items.brightness"),
    APPEARANCE_VIEW_RANGE("appearance.items.view-range"),
    APPEARANCE_SPIN("appearance.items.spin"),
    APPEARANCE_BOB("appearance.items.bob"),
    APPEARANCE_HITBOX("appearance.items.hitbox"),
    APPEARANCE_RESET("appearance.items.reset"),
    APPEARANCE_BACK("appearance.items.back"),

    HOLOGRAM_TOGGLE("hologram.items.toggle"),
    HOLOGRAM_LINES("hologram.items.lines"),
    HOLOGRAM_HEIGHT("hologram.items.height"),
    HOLOGRAM_BILLBOARD("hologram.items.billboard"),
    HOLOGRAM_VIEW_RANGE("hologram.items.view-range"),
    HOLOGRAM_BACKGROUND("hologram.items.background"),
    HOLOGRAM_SHADOW("hologram.items.shadow"),
    HOLOGRAM_BACK("hologram.items.back"),

    LIST_ENTRY("list.items.entry"),
    LIST_EMPTY("list.items.empty"),
    LIST_PREVIOUS("list.items.previous"),
    LIST_NEXT("list.items.next"),
    LIST_WAND("list.items.wand"),
    LIST_CLOSE("list.items.close"),

    CREATE_INFO("create.items.info"),
    CREATE_BLOCK("create.items.block"),
    CREATE_ITEM_DISPLAY("create.items.item-display"),
    CREATE_BLOCK_DISPLAY("create.items.block-display"),
    CREATE_CLOSE("create.items.close"),

    CONFIRM_INFO("confirm.items.info"),
    CONFIRM_ACCEPT("confirm.items.accept"),
    CONFIRM_CANCEL("confirm.items.cancel");

    private final String path;

    ItemKeys(@NotNull final String path) {
        this.path = path;
    }

    @NotNull
    public String getPath() {
        return path;
    }

    public List<Integer> getSlots() {
        Object raw = ClickableBlocks.getInstance().getGuis().get(path + ".slot");

        if (raw instanceof Integer i) return List.of(i);

        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(Integer.class::isInstance)
                    .map(Integer.class::cast)
                    .toList();
        }

        return List.of();
    }

    public boolean matches(int slot) {
        return getSlots().contains(slot);
    }

    @Nullable
    public ItemStack getItem(@NotNull String... replacements) {
        ItemStack item = ItemFactory.createItemFromString(path, ClickableBlocks.getInstance().getGuis()).orElse(null);
        if (item == null) return null;

        ItemUtil.replace(item, replacements);
        return item;
    }
}