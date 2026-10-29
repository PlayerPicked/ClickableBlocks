package com.playerpicked.clickableblocks.manager;

import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.identifiers.keys.ConfigKeys;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CooldownManager {
    private final Map<String, Map<UUID, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastClick = new ConcurrentHashMap<>();

    private MyScheduledTask cleanupTask;

    public void start() {
        cleanupTask = ClickableBlocks.getScheduler().runTaskTimerAsynchronously(this::cleanup, 1200L, 1200L);
    }

    public void shutdown() {
        if (cleanupTask != null) cleanupTask.cancel();
        cooldowns.clear();
        lastClick.clear();
    }

    public boolean tryDebounce(@NotNull UUID player) {
        long now = System.currentTimeMillis();
        long window = Math.max(0, ConfigKeys.CLICK_DEBOUNCE.getLong(200L));
        boolean[] accepted = {false};

        lastClick.compute(player, (uuid, last) -> {
            if (last != null && now - last < window) return last;
            accepted[0] = true;
            return now;
        });

        return accepted[0];
    }

    public long getRemaining(@NotNull String blockId, @NotNull UUID player) {
        Map<UUID, Long> map = cooldowns.get(blockId);
        if (map == null) return 0;

        Long expires = map.get(player);
        return expires == null ? 0 : Math.max(0, expires - System.currentTimeMillis());
    }

    public void apply(@NotNull String blockId, @NotNull UUID player, long millis) {
        if (millis <= 0) return;
        cooldowns.computeIfAbsent(blockId, key -> new ConcurrentHashMap<>()).put(player, System.currentTimeMillis() + millis);
    }

    public void clear(@NotNull String blockId) {
        cooldowns.remove(blockId);
    }

    public void forget(@NotNull UUID player) {
        lastClick.remove(player);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();

        cooldowns.values().forEach(map -> map.values().removeIf(expires -> expires <= now));
        cooldowns.values().removeIf(Map::isEmpty);
        lastClick.values().removeIf(last -> now - last > 60_000L);
    }
}
