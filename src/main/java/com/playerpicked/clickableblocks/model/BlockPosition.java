package com.playerpicked.clickableblocks.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record BlockPosition(@NotNull String world, int x, int y, int z) {

    @NotNull
    @Contract("_ -> new")
    public static BlockPosition of(@NotNull Block block) {
        return new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    @NotNull
    @Contract("_ -> new")
    public static BlockPosition of(@NotNull Location location) {
        return new BlockPosition(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Nullable
    public World getWorld() {
        return Bukkit.getWorld(world);
    }

    public int chunkX() {
        return x >> 4;
    }

    public int chunkZ() {
        return z >> 4;
    }

    @NotNull
    public ChunkKey chunk() {
        return new ChunkKey(world, chunkX(), chunkZ());
    }

    public boolean isLoaded() {
        World loaded = getWorld();
        return loaded != null && loaded.isChunkLoaded(chunkX(), chunkZ());
    }

    @Nullable
    public Block getBlock() {
        World loaded = getWorld();
        return loaded == null ? null : loaded.getBlockAt(x, y, z);
    }

    @Nullable
    public Location center() {
        World loaded = getWorld();
        return loaded == null ? null : new Location(loaded, x + 0.5, y + 0.5, z + 0.5);
    }

    @NotNull
    public String format() {
        return world + " " + x + ", " + y + ", " + z;
    }
}