package com.playerpicked.clickableblocks.manager;

import com.playerpicked.clickableblocks.database.DatabaseManager;
import com.playerpicked.clickableblocks.utils.LoggerUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

public final class UsageManager {
    private final Map<String, Map<UUID, Integer>> uses = new ConcurrentHashMap<>();

    public void load() {
        Map<String, Map<UUID, Integer>> stored = DatabaseManager.getDatabase().loadUses().join();
        uses.clear();
        stored.forEach((id, map) -> uses.put(id, new ConcurrentHashMap<>(map)));
    }

    public int getUses(@NotNull String blockId, @NotNull UUID player) {
        Map<UUID, Integer> map = uses.get(blockId);
        return map == null ? 0 : map.getOrDefault(player, 0);
    }

    public void increment(@NotNull String blockId, @NotNull UUID player) {
        int updated = uses.computeIfAbsent(blockId, key -> new ConcurrentHashMap<>()).merge(player, 1, Integer::sum);

        try {
            DatabaseManager.getDatabase().saveUses(blockId, player, updated);
        } catch (RejectedExecutionException | NullPointerException exception) {
            LoggerUtils.warn("Could not store the uses of '{}', the database is shutting down.", blockId);
        }
    }

    public void reset(@NotNull String blockId) {
        uses.remove(blockId);

        try {
            DatabaseManager.getDatabase().resetUses(blockId);
        } catch (RejectedExecutionException | NullPointerException exception) {
            LoggerUtils.warn("Could not reset the uses of '{}', the database is shutting down.", blockId);
        }
    }

    public void forget(@NotNull String blockId) {
        uses.remove(blockId);
    }

    public int countPlayers(@NotNull String blockId) {
        Map<UUID, Integer> map = uses.get(blockId);
        return map == null ? 0 : map.size();
    }
}