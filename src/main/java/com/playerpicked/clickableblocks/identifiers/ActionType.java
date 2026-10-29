package com.playerpicked.clickableblocks.identifiers;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

@Getter
public enum ActionType {
    PLAYER("player", "spawn"),
    CONSOLE("console", "give {player} diamond 1"),
    MESSAGE("message", "&aHello {player}!"),
    BROADCAST("broadcast", "&e{player} clicked {block}!"),
    ACTIONBAR("actionbar", "&bWelcome!"),
    TITLE("title", "&6Title;&7Subtitle;10;40;10"),
    SOUND("sound", "minecraft:entity.experience_orb.pickup;1;1"),
    PARTICLE("particle", "HAPPY_VILLAGER;20"),
    EFFECT("effect", "speed;10;1"),
    TELEPORT("teleport", "world;0.5;80;0.5;0;0"),
    SERVER("server", "lobby"),
    MONEY("money", "100"),
    CLOSE("close", ""),
    DELAY("delay", "20");

    private final String key;
    private final String example;

    ActionType(@NotNull String key, @NotNull String example) {
        this.key = key;
        this.example = example;
    }

    @NotNull
    public String format(@NotNull String value) {
        return value.isEmpty() ? "[" + key + "]" : "[" + key + "] " + value;
    }

    @Nullable
    public static ActionType fromKey(@Nullable String raw) {
        if (raw == null) return null;

        String key = raw.trim().toLowerCase(Locale.ROOT);
        for (ActionType type : values()) {
            if (type.key.equals(key)) return type;
        }

        return null;
    }

    @NotNull
    public static String keys() {
        return Arrays.stream(values()).map(type -> type.key).collect(Collectors.joining(", "));
    }
}