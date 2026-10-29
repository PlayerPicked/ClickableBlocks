package com.playerpicked.clickableblocks.identifiers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum ClickTrigger {
    LEFT,
    RIGHT,
    SHIFT_LEFT,
    SHIFT_RIGHT,
    ANY;

    @NotNull
    public static ClickTrigger of(boolean left, boolean sneaking) {
        if (left) return sneaking ? SHIFT_LEFT : LEFT;
        return sneaking ? SHIFT_RIGHT : RIGHT;
    }

    @Nullable
    public static ClickTrigger fromString(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return null;

        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}