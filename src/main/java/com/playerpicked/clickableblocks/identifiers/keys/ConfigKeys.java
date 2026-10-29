package com.playerpicked.clickableblocks.identifiers.keys;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.config.Config;
import com.playerpicked.clickableblocks.processor.MessageProcessor;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum ConfigKeys {
    DATABASE_MYSQL("database.mysql"),
    DATABASE_POOL("database.pool"),

    CLICK_DEBOUNCE("click.debounce"),
    CLICK_CANCEL_INTERACTION("click.cancel-interaction-by-default"),
    CLICK_PROTECT_BLOCKS("click.protect-blocks"),

    EDITOR_DEFAULT_ITEM("editor.default-item"),
    EDITOR_DEFAULT_BLOCK("editor.default-block"),
    EDITOR_REACH("editor.reach"),
    EDITOR_STEP_SCALE("editor.steps.scale"),
    EDITOR_STEP_ROTATION("editor.steps.rotation"),
    EDITOR_STEP_OFFSET("editor.steps.offset"),
    EDITOR_STEP_HOLOGRAM("editor.steps.hologram-height"),
    EDITOR_STEP_VIEW_RANGE("editor.steps.view-range"),
    EDITOR_GLOW_COLORS("editor.glow-colors"),
    EDITOR_PARTICLES("editor.particles"),
    EDITOR_SPIN_SPEEDS("editor.spin-speeds"),
    EDITOR_BOB_HEIGHTS("editor.bob-heights"),

    WAND("wand"),

    VISUAL_ANIMATION_INTERVAL("visuals.animation-interval"),
    VISUAL_PARTICLE_INTERVAL("visuals.particle-interval"),
    VISUAL_WATCHDOG_INTERVAL("visuals.watchdog-interval"),
    VISUAL_HOLOGRAM_LINE_WIDTH("visuals.hologram-line-width"),

    SAVE_INTERVAL("save-interval");

    private static final Config config = ClickableBlocks.getInstance().getConfiguration();
    private final String path;

    ConfigKeys(@NotNull String path) {
        this.path = path;
    }

    public @NotNull String getString() {
        return MessageProcessor.process(config.getString(path));
    }

    public @NotNull String getRawString(@NotNull String def) {
        String value = config.getString(path, def);
        return value == null ? def : value;
    }

    public boolean getBoolean(boolean def) {
        return config.getBoolean(path, def);
    }

    public int getInt(int def) {
        return config.getInt(path, def);
    }

    public long getLong(long def) {
        return config.getLong(path, def);
    }

    public double getDouble(double def) {
        return config.getDouble(path, def);
    }

    public List<String> getList(List<String> def) {
        return config.getList(path, def);
    }

    public @NotNull Section getSection() {
        return config.getSection(path);
    }
}