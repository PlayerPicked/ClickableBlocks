package com.playerpicked.clickableblocks.listener;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.data.MenuController;
import com.playerpicked.clickableblocks.guis.impl.CreateMenu;
import com.playerpicked.clickableblocks.guis.impl.EditorMenu;
import com.playerpicked.clickableblocks.identifiers.ClickTrigger;
import com.playerpicked.clickableblocks.manager.ClickableBlockManager;
import com.playerpicked.clickableblocks.model.BlockPosition;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import com.playerpicked.clickableblocks.service.ClickService;
import com.playerpicked.clickableblocks.service.EditorService;
import com.playerpicked.clickableblocks.service.VisualService;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ClickListener implements Listener {
    private final ClickableBlocks plugin = ClickableBlocks.getInstance();
    private final ClickableBlockManager manager;
    private final ClickService clickService;
    private final EditorService editorService;
    private final VisualService visualService;

    public ClickListener(@NotNull ClickableBlockManager manager, @NotNull ClickService clickService, @NotNull EditorService editorService, @NotNull VisualService visualService) {
        this.manager = manager;
        this.clickService = clickService;
        this.editorService = editorService;
        this.visualService = visualService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(final @NotNull PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        Action action = event.getAction();

        if (clicked == null || (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK)) return;

        Player player = event.getPlayer();
        ClickableBlock block = manager.getAt(BlockPosition.of(clicked)).orElse(null);

        if (isEditing(player)) {
            event.setCancelled(true);
            if (event.getHand() != EquipmentSlot.HAND) return;

            if (block != null) openEditor(player, block);
            else if (action == Action.RIGHT_CLICK_BLOCK) {
                BlockPosition position = BlockPosition.of(clicked);
                BlockPosition face = BlockPosition.of(clicked.getRelative(event.getBlockFace()));
                ClickableBlocks.getScheduler().runTask(player, () -> new CreateMenu(MenuController.getMenuUtils(player), position, face).open());
            }
            return;
        }

        if (block == null) return;

        boolean left = action == Action.LEFT_CLICK_BLOCK;
        if (left || block.isCancelInteraction()) event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) return;

        clickService.handle(player, block, ClickTrigger.of(left, player.isSneaking()));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(final @NotNull PlayerInteractEntityEvent event) {
        ClickableBlock block = hitbox(event.getRightClicked());
        if (block == null) return;

        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) return;

        handleEntity(event.getPlayer(), block, false);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAttack(final @NotNull PrePlayerAttackEntityEvent event) {
        ClickableBlock block = hitbox(event.getAttacked());
        if (block == null) return;

        event.setCancelled(true);
        handleEntity(event.getPlayer(), block, true);
    }

    private void handleEntity(@NotNull Player player, @NotNull ClickableBlock block, boolean left) {
        if (isEditing(player)) {
            openEditor(player, block);
            return;
        }

        clickService.handle(player, block, ClickTrigger.of(left, player.isSneaking()));
    }

    @Nullable
    private ClickableBlock hitbox(@NotNull Entity entity) {
        if (!(entity instanceof Interaction)) return null;

        String id = visualService.getBlockId(entity);
        return id == null ? null : manager.get(id).orElse(null);
    }

    private boolean isEditing(@NotNull Player player) {
        return player.hasPermission(EditorService.ADMIN_PERMISSION) && editorService.isWand(player.getInventory().getItemInMainHand());
    }

    private void openEditor(@NotNull Player player, @NotNull ClickableBlock block) {
        ClickableBlocks.getScheduler().runTask(player, () -> {
            if (plugin.getBlockManager().get(block.getId()).orElse(null) == block) new EditorMenu(MenuController.getMenuUtils(player), block).open();
        });
    }
}