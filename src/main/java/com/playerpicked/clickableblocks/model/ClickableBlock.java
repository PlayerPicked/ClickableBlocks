package com.playerpicked.clickableblocks.model;

import com.playerpicked.clickableblocks.identifiers.ClickTrigger;
import com.playerpicked.clickableblocks.identifiers.ClickableType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Setter
public final class ClickableBlock {
    public static final int MAX_ACTIONS = 54;

    private final String id;
    private final long createdAt;

    private volatile ClickableType type;
    private volatile BlockPosition position;
    private volatile boolean enabled = true;

    private volatile String permission = "";
    private volatile long cooldown;
    private volatile double cost;
    private volatile int maxUses;
    private volatile boolean cancelInteraction = true;
    private volatile String particle = "NONE";

    @Getter(AccessLevel.NONE)
    private final Map<ClickTrigger, List<String>> actions;

    private final DisplaySettings display = new DisplaySettings();
    private final HologramSettings hologram = new HologramSettings();

    public ClickableBlock(@NotNull String id, @NotNull ClickableType type, @NotNull BlockPosition position, long createdAt) {
        this.id = id;
        this.type = type;
        this.position = position;
        this.createdAt = createdAt;

        Map<ClickTrigger, List<String>> map = new EnumMap<>(ClickTrigger.class);
        for (ClickTrigger trigger : ClickTrigger.values()) map.put(trigger, new CopyOnWriteArrayList<>());
        this.actions = Collections.unmodifiableMap(map);
    }

    @NotNull
    public List<String> getActions(@NotNull ClickTrigger trigger) {
        return actions.get(trigger);
    }

    @NotNull
    public List<String> resolveActions(@NotNull ClickTrigger trigger) {
        List<String> resolved = new ArrayList<>(actions.get(trigger));
        if (trigger != ClickTrigger.ANY) resolved.addAll(actions.get(ClickTrigger.ANY));
        return resolved;
    }

    public int getTotalActions() {
        int total = 0;
        for (List<String> list : actions.values()) total += list.size();
        return total;
    }

    public boolean hasPermission() {
        String value = permission;
        return value != null && !value.isBlank();
    }

    public void copySettingsFrom(@NotNull ClickableBlock other) {
        enabled = other.enabled;
        permission = other.permission;
        cooldown = other.cooldown;
        cost = other.cost;
        maxUses = other.maxUses;
        cancelInteraction = other.cancelInteraction;
        particle = other.particle;

        for (ClickTrigger trigger : ClickTrigger.values()) {
            List<String> target = actions.get(trigger);
            target.clear();
            target.addAll(other.getActions(trigger));
        }

        display.copyFrom(other.display);
        hologram.copyFrom(other.hologram);
    }
}