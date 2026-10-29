package com.playerpicked.clickableblocks.manager;

import com.playerpicked.clickableblocks.identifiers.ClickableType;
import com.playerpicked.clickableblocks.model.BlockPosition;
import com.playerpicked.clickableblocks.model.ChunkKey;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class ClickableBlockManager {
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_-]{1,48}");

    private final Map<String, ClickableBlock> blocks = new ConcurrentHashMap<>();
    private final Map<BlockPosition, String> blockIndex = new ConcurrentHashMap<>();
    private final Map<ChunkKey, Set<String>> chunkIndex = new ConcurrentHashMap<>();

    @NotNull
    public static String normalizeId(@NotNull String raw) {
        return raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    public static boolean isValidId(@NotNull String id) {
        return ID_PATTERN.matcher(id).matches();
    }

    @NotNull
    public Optional<ClickableBlock> get(@Nullable String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(blocks.get(normalizeId(id)));
    }

    public boolean exists(@NotNull String id) {
        return blocks.containsKey(normalizeId(id));
    }

    @NotNull
    public Optional<ClickableBlock> getAt(@NotNull BlockPosition position) {
        String id = blockIndex.get(position);
        return id == null ? Optional.empty() : Optional.ofNullable(blocks.get(id));
    }

    public boolean isProtected(@NotNull BlockPosition position) {
        return blockIndex.containsKey(position);
    }

    @NotNull
    public List<ClickableBlock> inChunk(@NotNull String world, int chunkX, int chunkZ) {
        Set<String> ids = chunkIndex.get(new ChunkKey(world, chunkX, chunkZ));
        if (ids == null || ids.isEmpty()) return List.of();

        return ids.stream()
                .map(blocks::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @NotNull
    public @Unmodifiable List<ClickableBlock> inWorld(@NotNull String world) {
        return blocks.values().stream()
                .filter(block -> block.getPosition().world().equals(world))
                .toList();
    }

    @NotNull
    public Collection<ClickableBlock> all() {
        return blocks.values();
    }

    @NotNull
    public @Unmodifiable List<ClickableBlock> sorted() {
        return blocks.values().stream()
                .sorted(Comparator.comparing(ClickableBlock::getId))
                .toList();
    }

    @NotNull
    public @Unmodifiable List<String> ids() {
        return blocks.keySet().stream().sorted().toList();
    }

    public int size() {
        return blocks.size();
    }

    public synchronized boolean register(@NotNull ClickableBlock block) {
        if (blocks.containsKey(block.getId())) return false;
        if (block.getType() == ClickableType.BLOCK && blockIndex.containsKey(block.getPosition())) return false;

        blocks.put(block.getId(), block);
        index(block);
        return true;
    }

    @Nullable
    public synchronized ClickableBlock unregister(@NotNull String id) {
        ClickableBlock removed = blocks.remove(normalizeId(id));
        if (removed != null) unindex(removed);
        return removed;
    }

    public synchronized boolean move(@NotNull ClickableBlock block, @NotNull BlockPosition target) {
        if (blocks.get(block.getId()) != block) return false;
        if (block.getType() == ClickableType.BLOCK && isTakenByOther(block, target)) return false;

        unindex(block);
        block.setPosition(target);
        index(block);
        return true;
    }

    public synchronized boolean changeType(@NotNull ClickableBlock block, @NotNull ClickableType type) {
        if (blocks.get(block.getId()) != block) return false;
        if (type == ClickableType.BLOCK && isTakenByOther(block, block.getPosition())) return false;

        unindex(block);
        block.setType(type);
        index(block);
        return true;
    }

    public boolean isTakenByOther(@NotNull ClickableBlock block, @NotNull BlockPosition position) {
        String owner = blockIndex.get(position);
        return owner != null && !owner.equals(block.getId());
    }

    @NotNull
    public String nextFreeId(@NotNull String base) {
        String normalized = normalizeId(base);
        if (!blocks.containsKey(normalized)) return normalized;

        int index = 2;
        while (blocks.containsKey(normalized + "_" + index)) index++;
        return normalized + "_" + index;
    }

    public synchronized void clear() {
        blocks.clear();
        blockIndex.clear();
        chunkIndex.clear();
    }

    private void index(@NotNull ClickableBlock block) {
        BlockPosition position = block.getPosition();
        if (block.getType() == ClickableType.BLOCK) blockIndex.put(position, block.getId());
        chunkIndex.computeIfAbsent(position.chunk(), key -> ConcurrentHashMap.newKeySet()).add(block.getId());
    }

    private void unindex(@NotNull ClickableBlock block) {
        BlockPosition position = block.getPosition();
        blockIndex.remove(position, block.getId());

        chunkIndex.computeIfPresent(position.chunk(), (key, ids) -> {
            ids.remove(block.getId());
            return ids.isEmpty() ? null : ids;
        });
    }
}