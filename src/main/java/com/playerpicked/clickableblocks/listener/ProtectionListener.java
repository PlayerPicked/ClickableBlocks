package com.playerpicked.clickableblocks.listener;

import com.playerpicked.clickableblocks.identifiers.keys.ConfigKeys;
import com.playerpicked.clickableblocks.manager.ClickableBlockManager;
import com.playerpicked.clickableblocks.model.BlockPosition;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ProtectionListener implements Listener {
    private final ClickableBlockManager manager;

    public ProtectionListener(@NotNull ClickableBlockManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(final @NotNull BlockBreakEvent event) {
        if (isProtected(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockExplode(final @NotNull BlockExplodeEvent event) {
        if (enabled()) event.blockList().removeIf(this::isProtected);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityExplode(final @NotNull EntityExplodeEvent event) {
        if (enabled()) event.blockList().removeIf(this::isProtected);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPistonExtend(final @NotNull BlockPistonExtendEvent event) {
        if (anyProtected(event.getBlocks())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPistonRetract(final @NotNull BlockPistonRetractEvent event) {
        if (anyProtected(event.getBlocks())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBurn(final @NotNull BlockBurnEvent event) {
        if (isProtected(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFade(final @NotNull BlockFadeEvent event) {
        if (isProtected(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDecay(final @NotNull LeavesDecayEvent event) {
        if (isProtected(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFlow(final @NotNull BlockFromToEvent event) {
        if (isProtected(event.getToBlock())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityChange(final @NotNull EntityChangeBlockEvent event) {
        if (isProtected(event.getBlock())) event.setCancelled(true);
    }

    private boolean anyProtected(@NotNull List<Block> blocks) {
        if (!enabled()) return false;

        for (Block block : blocks) {
            if (manager.isProtected(BlockPosition.of(block))) return true;
        }

        return false;
    }

    private boolean isProtected(@NotNull Block block) {
        return enabled() && manager.isProtected(BlockPosition.of(block));
    }

    private boolean enabled() {
        return ConfigKeys.CLICK_PROTECT_BLOCKS.getBoolean(true);
    }
}