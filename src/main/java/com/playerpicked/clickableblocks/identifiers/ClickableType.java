package com.playerpicked.clickableblocks.identifiers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum ClickableType {
    BLOCK,
    ITEM_DISPLAY,
    BLOCK_DISPLAY;

    public boolean isDisplay() {
        return this != BLOCK;
    }

    @NotNull
    public ClickableType next() {
        ClickableType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    @NotNull
    public ClickableType previous() {
        ClickableType[] values = values();
        return values[(ordinal() - 1 + values.length) % values.length];
    }

    @Nullable
    public static ClickableType fromString(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return null;

        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        for (ClickableType type : values()) {
            if (type.name().equals(normalized)) return type;
        }

        return switch (normalized) {
            case "ITEM", "ITEMDISPLAY" -> ITEM_DISPLAY;
            case "BLOCKDISPLAY" -> BLOCK_DISPLAY;
            default -> null;
        };
    }
}