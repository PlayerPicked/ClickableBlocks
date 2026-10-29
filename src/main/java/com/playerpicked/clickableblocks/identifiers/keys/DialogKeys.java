package com.playerpicked.clickableblocks.identifiers.keys;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public enum DialogKeys {
    CREATE("dialogs.create"),
    CLONE("dialogs.clone"),
    ACTION_ADD("dialogs.action-add"),
    ACTION_EDIT("dialogs.action-edit"),
    PERMISSION("dialogs.permission"),
    COOLDOWN("dialogs.cooldown"),
    COST("dialogs.cost"),
    MAX_USES("dialogs.max-uses"),
    HOLOGRAM_LINES("dialogs.hologram-lines"),
    GLOW_COLOR("dialogs.glow-color"),
    SCALE("dialogs.scale"),
    ROTATION("dialogs.rotation"),
    OFFSET("dialogs.offset"),
    HITBOX("dialogs.hitbox");

    private final String path;

    DialogKeys(@NotNull final String path) {
        this.path = path;
    }
}