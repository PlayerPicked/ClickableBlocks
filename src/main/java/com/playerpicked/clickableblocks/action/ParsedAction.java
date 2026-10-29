package com.playerpicked.clickableblocks.action;

import com.playerpicked.clickableblocks.identifiers.ActionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ParsedAction(@NotNull ActionType type, @NotNull String value) {

    @Nullable
    public static ParsedAction parse(@Nullable String line) {
        if (line == null) return null;

        String trimmed = line.strip();
        if (!trimmed.startsWith("[")) return null;

        int close = trimmed.indexOf(']');
        if (close < 0) return null;

        ActionType type = ActionType.fromKey(trimmed.substring(1, close));
        if (type == null) return null;

        String value = trimmed.substring(close + 1).strip();
        if (type == ActionType.PLAYER || type == ActionType.CONSOLE) {
            while (value.startsWith("/")) value = value.substring(1);
        }

        return new ParsedAction(type, value);
    }

    @Nullable
    public static String normalize(@NotNull String input) {
        String trimmed = input.strip();
        if (trimmed.isEmpty()) return null;

        if (!trimmed.startsWith("[")) trimmed = ActionType.PLAYER.format(trimmed.startsWith("/") ? trimmed.substring(1) : trimmed);

        ParsedAction parsed = parse(trimmed);
        return parsed == null ? null : parsed.type().format(parsed.value());
    }
}