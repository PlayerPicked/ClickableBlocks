package com.playerpicked.clickableblocks.identifiers.keys;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.config.Config;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum MenuKeys {
    EDITOR("editor"),
    TRIGGERS("triggers"),
    ACTIONS("actions"),
    SETTINGS("settings"),
    APPEARANCE("appearance"),
    HOLOGRAM("hologram"),
    LIST("list"),
    CREATE("create"),
    CONFIRM("confirm");

    private final String path;

    MenuKeys(@NotNull String path) {
        this.path = path;
    }

    private static Config config() {
        return ClickableBlocks.getInstance().getGuis();
    }

    @NotNull
    public String getPath() {
        return path;
    }

    @NotNull
    public String getTitle() {
        return config().getString(path + ".title", "");
    }

    public int getSize() {
        int size = config().getInt(path + ".size", 54);
        return Math.clamp((size / 9) * 9, 9, 54);
    }

    public int getTick() {
        return config().getInt(path + ".tick", 0);
    }

    @NotNull
    public String getSound(@NotNull String name) {
        return config().getString(path + ".sounds." + name, "");
    }

    @NotNull
    public List<Integer> getContentSlots() {
        return config().getIntList(path + ".content-slots", List.of());
    }

    @NotNull
    public String getDecorationPath() {
        return path + ".decoration";
    }
}