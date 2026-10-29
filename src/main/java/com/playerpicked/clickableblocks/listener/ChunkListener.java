package com.playerpicked.clickableblocks.listener;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.service.VisualService;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.NotNull;

public final class ChunkListener implements Listener {
    private final VisualService visualService;

    public ChunkListener(@NotNull VisualService visualService) {
        this.visualService = visualService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(final @NotNull ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();
        int x = chunk.getX();
        int z = chunk.getZ();

        ClickableBlocks.getScheduler().runTask(() -> {
            if (world.isChunkLoaded(x, z)) visualService.onChunkLoad(world, x, z);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(final @NotNull ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        visualService.onChunkUnload(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(final @NotNull EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (visualService.isTagged(entity) && !visualService.isTracked(entity)) entity.remove();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(final @NotNull WorldLoadEvent event) {
        World world = event.getWorld();
        ClickableBlocks.getScheduler().runTask(() -> visualService.onWorldLoad(world));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnload(final @NotNull WorldUnloadEvent event) {
        visualService.onWorldUnload(event.getWorld());
    }
}