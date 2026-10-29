package com.playerpicked.clickableblocks.guis.impl;

import com.playerpicked.clickableblocks.data.MenuController;
import com.playerpicked.clickableblocks.guis.BlockMenu;
import com.playerpicked.clickableblocks.guis.Menu;
import com.playerpicked.clickableblocks.identifiers.ClickableType;
import com.playerpicked.clickableblocks.identifiers.keys.ConfigKeys;
import com.playerpicked.clickableblocks.identifiers.keys.DialogKeys;
import com.playerpicked.clickableblocks.identifiers.keys.ItemKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MenuKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MessageKeys;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import com.playerpicked.clickableblocks.model.DisplaySettings;
import com.playerpicked.clickableblocks.utils.FormatUtil;
import com.playerpicked.clickableblocks.utils.ItemUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public final class AppearanceMenu extends BlockMenu {
    private static final List<String> BILLBOARDS = Arrays.stream(Display.Billboard.values()).map(Enum::name).toList();
    private static final List<String> TRANSFORMS = Arrays.stream(ItemDisplay.ItemDisplayTransform.values()).map(Enum::name).toList();

    public AppearanceMenu(@NotNull MenuController menuController, @NotNull ClickableBlock block) {
        super(menuController, block);
    }

    @Override
    protected @NotNull MenuKeys getKey() {
        return MenuKeys.APPEARANCE;
    }

    @Override
    protected @NotNull Menu reopen() {
        return new AppearanceMenu(menuController, block);
    }

    @Override
    public void setMenuItems() {
        inventory.clear();

        DisplaySettings display = block.getDisplay();
        String[] info = placeholders(display);

        if (block.getType() == ClickableType.BLOCK) {
            place(ItemKeys.APPEARANCE_BLOCK, info);
            place(ItemKeys.APPEARANCE_GLOW, info);
            place(ItemKeys.APPEARANCE_GLOW_COLOR, info);
        } else {
            place(ItemKeys.APPEARANCE_ITEM, info);
            place(ItemKeys.APPEARANCE_SCALE, info);
            place(ItemKeys.APPEARANCE_YAW, info);
            place(ItemKeys.APPEARANCE_PITCH, info);
            place(ItemKeys.APPEARANCE_ROLL, info);
            place(ItemKeys.APPEARANCE_BILLBOARD, info);
            if (block.getType() == ClickableType.ITEM_DISPLAY) place(ItemKeys.APPEARANCE_TRANSFORM, info);
            place(ItemKeys.APPEARANCE_OFFSET_X, info);
            place(ItemKeys.APPEARANCE_OFFSET_Y, info);
            place(ItemKeys.APPEARANCE_OFFSET_Z, info);
            place(ItemKeys.APPEARANCE_GLOW, info);
            place(ItemKeys.APPEARANCE_GLOW_COLOR, info);
            place(ItemKeys.APPEARANCE_BRIGHTNESS, info);
            place(ItemKeys.APPEARANCE_VIEW_RANGE, info);
            place(ItemKeys.APPEARANCE_SPIN, info);
            place(ItemKeys.APPEARANCE_BOB, info);
            place(ItemKeys.APPEARANCE_HITBOX, info);
            place(ItemKeys.APPEARANCE_RESET, info);
        }

        place(ItemKeys.APPEARANCE_BACK, info);
        decorate();
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        event.setCancelled(true);

        Player player = player();
        int slot = event.getRawSlot();

        if (slot >= inventory.getSize()) {
            ItemStack clicked = event.getCurrentItem();
            if (!ItemUtil.isEmpty(clicked)) useItem(player, clicked);
            return;
        }

        if (slot < 0) return;

        if (ItemKeys.APPEARANCE_BACK.matches(slot)) {
            sound("action");
            openLater(new EditorMenu(menuController, block));
            return;
        }

        DisplaySettings display = block.getDisplay();
        boolean forward = !event.isRightClick();
        boolean precise = event.isShiftClick();

        if (ItemKeys.APPEARANCE_GLOW.matches(slot)) {
            display.setGlow(!display.isGlow());
            edited("glow", MessageKeys.state(display.isGlow()));
            return;
        }

        if (ItemKeys.APPEARANCE_GLOW_COLOR.matches(slot)) {
            if (precise) {
                ask(DialogKeys.GLOW_COLOR, FormatUtil.color(display.getGlowColor()), value -> {
                    Integer color = FormatUtil.parseColor(value);
                    if (color == null) {
                        MessageKeys.EDITOR_INVALID_COLOR.send(player, "{input}", value);
                        return;
                    }

                    display.setGlowColor(color);
                    if (!display.isGlow()) display.setGlow(true);
                    edited("glow-color", FormatUtil.color(color));
                });
                return;
            }

            List<String> presets = ConfigKeys.EDITOR_GLOW_COLORS.getList(List.of("#FFFFFF"));
            String next = cycle(presets, FormatUtil.color(display.getGlowColor()), forward);
            Integer color = FormatUtil.parseColor(next);
            if (color == null) return;

            display.setGlowColor(color);
            if (!display.isGlow()) display.setGlow(true);
            edited("glow-color", FormatUtil.color(color));
            return;
        }

        if (block.getType() == ClickableType.BLOCK) {
            if (ItemKeys.APPEARANCE_BLOCK.matches(slot)) useItem(player, player.getInventory().getItemInMainHand());
            return;
        }

        if (ItemKeys.APPEARANCE_ITEM.matches(slot)) {
            useItem(player, player.getInventory().getItemInMainHand());
            return;
        }

        float scaleStep = (float) ConfigKeys.EDITOR_STEP_SCALE.getDouble(0.1);
        float rotationStep = (float) ConfigKeys.EDITOR_STEP_ROTATION.getDouble(15);
        double offsetStep = ConfigKeys.EDITOR_STEP_OFFSET.getDouble(0.0625);

        if (ItemKeys.APPEARANCE_SCALE.matches(slot)) {
            if (precise) {
                askVector(DialogKeys.SCALE, display.getScaleX() + " " + display.getScaleY() + " " + display.getScaleZ(), 3, values -> {
                    display.setScale((float) values[0], (float) values[1], (float) values[2]);
                    edited("scale", scaleText(display));
                });
                return;
            }

            display.scaleUniform(forward ? scaleStep : -scaleStep);
            editedQuietly();
            return;
        }

        if (ItemKeys.APPEARANCE_YAW.matches(slot) || ItemKeys.APPEARANCE_PITCH.matches(slot) || ItemKeys.APPEARANCE_ROLL.matches(slot)) {
            if (precise) {
                askVector(DialogKeys.ROTATION, display.getYaw() + " " + display.getPitch() + " " + display.getRoll(), 3, values -> {
                    display.setRotation((float) values[0], (float) values[1], (float) values[2]);
                    edited("rotation", FormatUtil.number(display.getYaw()) + " / " + FormatUtil.number(display.getPitch()) + " / " + FormatUtil.number(display.getRoll()));
                });
                return;
            }

            float delta = forward ? rotationStep : -rotationStep;
            float yaw = display.getYaw() + (ItemKeys.APPEARANCE_YAW.matches(slot) ? delta : 0);
            float pitch = display.getPitch() + (ItemKeys.APPEARANCE_PITCH.matches(slot) ? delta : 0);
            float roll = display.getRoll() + (ItemKeys.APPEARANCE_ROLL.matches(slot) ? delta : 0);
            display.setRotation(yaw, pitch, roll);
            editedQuietly();
            return;
        }

        if (ItemKeys.APPEARANCE_OFFSET_X.matches(slot) || ItemKeys.APPEARANCE_OFFSET_Y.matches(slot) || ItemKeys.APPEARANCE_OFFSET_Z.matches(slot)) {
            if (precise) {
                askVector(DialogKeys.OFFSET, display.getOffsetX() + " " + display.getOffsetY() + " " + display.getOffsetZ(), 3, values -> {
                    display.setOffset(values[0], values[1], values[2]);
                    edited("offset", FormatUtil.number(display.getOffsetX()) + " / " + FormatUtil.number(display.getOffsetY()) + " / " + FormatUtil.number(display.getOffsetZ()));
                });
                return;
            }

            double delta = forward ? offsetStep : -offsetStep;
            display.setOffset(
                    display.getOffsetX() + (ItemKeys.APPEARANCE_OFFSET_X.matches(slot) ? delta : 0),
                    display.getOffsetY() + (ItemKeys.APPEARANCE_OFFSET_Y.matches(slot) ? delta : 0),
                    display.getOffsetZ() + (ItemKeys.APPEARANCE_OFFSET_Z.matches(slot) ? delta : 0));
            editedQuietly();
            return;
        }

        if (ItemKeys.APPEARANCE_BILLBOARD.matches(slot)) {
            display.setBillboard(cycle(BILLBOARDS, display.getBillboard(), forward));
            edited("billboard", display.getBillboard());
            return;
        }

        if (ItemKeys.APPEARANCE_TRANSFORM.matches(slot) && block.getType() == ClickableType.ITEM_DISPLAY) {
            display.setTransform(cycle(TRANSFORMS, display.getTransform(), forward));
            edited("transform", display.getTransform());
            return;
        }

        if (ItemKeys.APPEARANCE_BRIGHTNESS.matches(slot)) {
            int value = display.getBrightness() + (forward ? 1 : -1);
            if (value > 15) value = -1;
            if (value < -1) value = 15;

            display.setBrightness(value);
            editedQuietly();
            return;
        }

        if (ItemKeys.APPEARANCE_VIEW_RANGE.matches(slot)) {
            float step = (float) ConfigKeys.EDITOR_STEP_VIEW_RANGE.getDouble(0.5);
            float value = Math.round((display.getViewRange() + (forward ? step : -step)) * 100f) / 100f;

            display.setViewRange(Math.clamp(value, 0.1f, 10f));
            editedQuietly();
            return;
        }

        if (ItemKeys.APPEARANCE_SPIN.matches(slot)) {
            if (forward) {
                display.setSpin(!display.isSpin());
                edited("spin", MessageKeys.state(display.isSpin()));
                return;
            }

            List<String> speeds = ConfigKeys.EDITOR_SPIN_SPEEDS.getList(List.of("45"));
            Double speed = FormatUtil.parseDouble(cycle(speeds, FormatUtil.number(display.getSpinSpeed()), true));
            if (speed == null) return;

            display.setSpinSpeed(speed.floatValue());
            display.setSpin(true);
            edited("spin-speed", FormatUtil.number(speed));
            return;
        }

        if (ItemKeys.APPEARANCE_BOB.matches(slot)) {
            if (forward) {
                display.setBob(!display.isBob());
                edited("bob", MessageKeys.state(display.isBob()));
                return;
            }

            List<String> heights = ConfigKeys.EDITOR_BOB_HEIGHTS.getList(List.of("0.15"));
            Double height = FormatUtil.parseDouble(cycle(heights, FormatUtil.number(display.getBobHeight()), true));
            if (height == null) return;

            display.setBobHeight(height.floatValue());
            display.setBob(true);
            edited("bob-height", FormatUtil.number(height));
            return;
        }

        if (ItemKeys.APPEARANCE_HITBOX.matches(slot)) {
            if (!forward) {
                display.setHitbox(-1f, -1f);
                edited("hitbox", MessageKeys.PLACEHOLDER_AUTO.getMessage());
                return;
            }

            String initial = FormatUtil.number(display.resolveHitboxWidth()) + " " + FormatUtil.number(display.resolveHitboxHeight());
            askVector(DialogKeys.HITBOX, initial, 2, values -> {
                display.setHitbox((float) values[0], (float) values[1]);
                edited("hitbox", hitboxText(display));
            });
            return;
        }

        if (ItemKeys.APPEARANCE_RESET.matches(slot)) {
            if (!precise) {
                error();
                return;
            }

            display.resetTransform();
            edited("transform-reset", MessageKeys.PLACEHOLDER_DEFAULT.getMessage());
        }
    }

    private void useItem(@NotNull Player player, @NotNull ItemStack item) {
        if (ItemUtil.isEmpty(item) || plugin.getEditorService().isWand(item)) {
            error();
            return;
        }

        if (block.getType() == ClickableType.BLOCK) {
            if (plugin.getEditorService().setPhysicalBlock(player, block, item.getType())) {
                sound("action");
                updateMenuItems();
            } else error();
            return;
        }

        if (block.getType() == ClickableType.BLOCK_DISPLAY && !item.getType().isBlock()) {
            MessageKeys.BLOCK_NOT_A_BLOCK.send(player);
            error();
            return;
        }

        block.getDisplay().setItem(item);
        plugin.getEditorService().changed(block);
        MessageKeys.BLOCK_ITEM_SET.send(player, "{item}", ItemUtil.prettyName(item.getType()));
        sound("action");
        updateMenuItems();
    }

    private void askVector(@NotNull DialogKeys key, @NotNull String initial, int size, @NotNull java.util.function.Consumer<double[]> onValues) {
        ask(key, initial, value -> {
            double[] values = FormatUtil.parseVector(value, size);
            if (values == null) {
                MessageKeys.EDITOR_INVALID_NUMBER.send(player(), "{input}", value);
                return;
            }

            onValues.accept(values);
        });
    }

    @NotNull
    private String[] placeholders(@NotNull DisplaySettings display) {
        Block physical = block.getPosition().getBlock();
        String blockName = physical == null ? MessageKeys.PLACEHOLDER_NONE.getMessage() : ItemUtil.prettyName(physical.getType());

        return new String[]{
                "{id}", block.getId(),
                "{item}", ItemUtil.prettyName(display.getMaterial()),
                "{block}", blockName,
                "{scale}", scaleText(display),
                "{yaw}", FormatUtil.number(display.getYaw()),
                "{pitch}", FormatUtil.number(display.getPitch()),
                "{roll}", FormatUtil.number(display.getRoll()),
                "{billboard}", display.getBillboard(),
                "{transform}", display.getTransform(),
                "{offset_x}", FormatUtil.number(display.getOffsetX()),
                "{offset_y}", FormatUtil.number(display.getOffsetY()),
                "{offset_z}", FormatUtil.number(display.getOffsetZ()),
                "{glow}", MessageKeys.state(display.isGlow()),
                "{glow_color}", FormatUtil.color(display.getGlowColor()),
                "{brightness}", display.getBrightness() < 0 ? MessageKeys.PLACEHOLDER_DEFAULT.getMessage() : String.valueOf(display.getBrightness()),
                "{view_range}", FormatUtil.number(display.getViewRange()),
                "{spin}", MessageKeys.state(display.isSpin()),
                "{spin_speed}", FormatUtil.number(display.getSpinSpeed()),
                "{bob}", MessageKeys.state(display.isBob()),
                "{bob_height}", FormatUtil.number(display.getBobHeight()),
                "{hitbox}", hitboxText(display),
                "{scale_step}", FormatUtil.number(ConfigKeys.EDITOR_STEP_SCALE.getDouble(0.1)),
                "{rotation_step}", FormatUtil.number(ConfigKeys.EDITOR_STEP_ROTATION.getDouble(15)),
                "{offset_step}", FormatUtil.number(ConfigKeys.EDITOR_STEP_OFFSET.getDouble(0.0625))
        };
    }

    @NotNull
    private static String scaleText(@NotNull DisplaySettings display) {
        if (display.getScaleX() == display.getScaleY() && display.getScaleY() == display.getScaleZ()) return FormatUtil.number(display.getScaleX());
        return FormatUtil.number(display.getScaleX()) + " / " + FormatUtil.number(display.getScaleY()) + " / " + FormatUtil.number(display.getScaleZ());
    }

    @NotNull
    private static String hitboxText(@NotNull DisplaySettings display) {
        String size = FormatUtil.number(display.resolveHitboxWidth()) + " x " + FormatUtil.number(display.resolveHitboxHeight());
        return display.hasAutoHitbox() ? MessageKeys.PLACEHOLDER_AUTO.getMessage() + " (" + size + ")" : size;
    }

    @NotNull
    static String cycle(@NotNull List<String> values, @NotNull String current, boolean forward) {
        if (values.isEmpty()) return current;

        int index = -1;
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equalsIgnoreCase(current)) {
                index = i;
                break;
            }
        }

        if (index < 0) return values.getFirst();
        return values.get(Math.floorMod(index + (forward ? 1 : -1), values.size()));
    }
}