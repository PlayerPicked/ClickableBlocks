package com.playerpicked.clickableblocks.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public final class HologramSettings {
    public static final int MAX_LINES = 10;

    private volatile boolean enabled;
    @Setter(AccessLevel.NONE) private volatile List<String> lines = List.of();
    @Setter(AccessLevel.NONE) private volatile double height = 0.25;
    private volatile boolean background;
    private volatile boolean shadow = true;
    private volatile String billboard = "CENTER";
    @Setter(AccessLevel.NONE) private volatile float viewRange = 1f;

    public boolean isVisible() {
        return enabled && !lines.isEmpty();
    }

    public void setLines(@NotNull List<String> newLines) {
        lines = new CopyOnWriteArrayList<>(newLines.stream().limit(MAX_LINES).toList());
    }

    public void setHeight(double value) {
        height = Math.clamp(Math.round(value * 100d) / 100d, -2d, 8d);
    }

    public void setViewRange(float value) {
        viewRange = Math.clamp(Math.round(value * 100f) / 100f, 0.1f, 10f);
    }

    public void copyFrom(@NotNull HologramSettings other) {
        enabled = other.enabled;
        setLines(other.lines);
        height = other.height;
        background = other.background;
        shadow = other.shadow;
        billboard = other.billboard;
        viewRange = other.viewRange;
    }
}