package com.playerpicked.clickableblocks.database;

import com.playerpicked.clickableblocks.model.StoredBlock;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface Database {
    CompletableFuture<Void> initialize();

    CompletableFuture<List<StoredBlock>> loadBlocks();
    CompletableFuture<Void> saveBlock(StoredBlock block);
    CompletableFuture<Void> deleteBlock(String id);

    CompletableFuture<Map<String, Map<UUID, Integer>>> loadUses();
    CompletableFuture<Void> saveUses(String blockId, UUID player, int uses);
    CompletableFuture<Void> resetUses(String blockId);

    void shutdown();
}