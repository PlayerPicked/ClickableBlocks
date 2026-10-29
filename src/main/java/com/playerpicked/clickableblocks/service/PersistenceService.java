package com.playerpicked.clickableblocks.service;

import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.database.Database;
import com.playerpicked.clickableblocks.database.DatabaseManager;
import com.playerpicked.clickableblocks.identifiers.keys.ConfigKeys;
import com.playerpicked.clickableblocks.manager.ClickableBlockManager;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import com.playerpicked.clickableblocks.model.StoredBlock;
import com.playerpicked.clickableblocks.utils.LoggerUtils;
import com.playerpicked.clickableblocks.utils.SerializerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

public final class PersistenceService {
    private final ClickableBlockManager manager;
    private final Set<String> dirty = ConcurrentHashMap.newKeySet();

    private MyScheduledTask task;

    public PersistenceService(@NotNull ClickableBlockManager manager) {
        this.manager = manager;
    }

    public void loadAll() {
        List<StoredBlock> rows = DatabaseManager.getDatabase().loadBlocks().join();
        manager.clear();

        int skipped = 0;
        for (StoredBlock row : rows) {
            ClickableBlock block = SerializerUtil.fromStored(row);

            if (block == null || !manager.register(block)) {
                skipped++;
                LoggerUtils.warn("Clickable block '{}' could not be loaded (duplicate position or broken row).", row.id());
            }
        }

        if (skipped > 0) LoggerUtils.warn("{} clickable blocks were skipped while loading.", skipped);
    }

    public void start() {
        long interval = Math.max(20L, ConfigKeys.SAVE_INTERVAL.getLong(40L));
        task = ClickableBlocks.getScheduler().runTaskTimer(this::flush, interval, interval);
    }

    public void markDirty(@NotNull ClickableBlock block) {
        dirty.add(block.getId());
    }

    public void saveNow(@NotNull ClickableBlock block) {
        dirty.remove(block.getId());
        write(SerializerUtil.toStored(block));
    }

    public void delete(@NotNull String id) {
        dirty.remove(id);

        try {
            DatabaseManager.getDatabase().deleteBlock(id);
        } catch (RejectedExecutionException exception) {
            LoggerUtils.warn("Could not delete '{}', the database is shutting down.", id);
        }
    }

    public void flush() {
        if (dirty.isEmpty()) return;

        List<StoredBlock> snapshot = new ArrayList<>();
        for (String id : List.copyOf(dirty)) {
            dirty.remove(id);
            manager.get(id).ifPresent(block -> snapshot.add(SerializerUtil.toStored(block)));
        }

        snapshot.forEach(this::write);
    }

    public void shutdown() {
        if (task != null) task.cancel();
        flush();
    }

    private void write(@NotNull StoredBlock stored) {
        try {
            DatabaseManager.getDatabase().saveBlock(stored);
        } catch (RejectedExecutionException exception) {
            LoggerUtils.warn("Could not save '{}', the database is shutting down.", stored.id());
        }
    }
}