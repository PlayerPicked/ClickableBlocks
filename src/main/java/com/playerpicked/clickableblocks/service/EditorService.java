package com.playerpicked.clickableblocks.service;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.guis.BlockBoundMenu;
import com.playerpicked.clickableblocks.identifiers.ClickableType;
import com.playerpicked.clickableblocks.identifiers.keys.ConfigKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MessageKeys;
import com.playerpicked.clickableblocks.item.ItemFactory;
import com.playerpicked.clickableblocks.manager.ClickableBlockManager;
import com.playerpicked.clickableblocks.manager.CooldownManager;
import com.playerpicked.clickableblocks.manager.UsageManager;
import com.playerpicked.clickableblocks.model.BlockPosition;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import com.playerpicked.clickableblocks.utils.ItemUtil;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class EditorService {
    public static final String ADMIN_PERMISSION = "clickableblocks.admin";

    private final ClickableBlockManager manager;
    private final PersistenceService persistence;
    private final VisualService visuals;
    private final CooldownManager cooldowns;
    private final UsageManager usages;
    private final NamespacedKey wandKey;

    public EditorService(@NotNull ClickableBlockManager manager, @NotNull PersistenceService persistence, @NotNull VisualService visuals, @NotNull CooldownManager cooldowns, @NotNull UsageManager usages) {
        this.manager = manager;
        this.persistence = persistence;
        this.visuals = visuals;
        this.cooldowns = cooldowns;
        this.usages = usages;
        this.wandKey = new NamespacedKey(ClickableBlocks.getInstance(), "wand");
    }

    @NotNull
    public Optional<ClickableBlock> create(@NotNull Player player, @NotNull String rawId, @NotNull ClickableType type, @NotNull BlockPosition position) {
        String id = ClickableBlockManager.normalizeId(rawId);

        if (!ClickableBlockManager.isValidId(id)) {
            MessageKeys.BLOCK_INVALID_ID.send(player, "{id}", rawId);
            return Optional.empty();
        }

        if (manager.exists(id)) {
            MessageKeys.BLOCK_ALREADY_EXISTS.send(player, "{id}", id);
            return Optional.empty();
        }

        if (type == ClickableType.BLOCK) {
            Block physical = position.getBlock();
            if (physical == null || physical.getType().isAir()) {
                MessageKeys.BLOCK_NO_TARGET.send(player);
                return Optional.empty();
            }
        }

        ClickableBlock block = new ClickableBlock(id, type, position, System.currentTimeMillis());
        block.setCancelInteraction(ConfigKeys.CLICK_CANCEL_INTERACTION.getBoolean(true));
        block.getDisplay().setItem(defaultItem(player, type));

        if (!manager.register(block)) {
            if (manager.exists(id)) MessageKeys.BLOCK_ALREADY_EXISTS.send(player, "{id}", id);
            else MessageKeys.BLOCK_POSITION_TAKEN.send(player, "{id}", manager.getAt(position).map(ClickableBlock::getId).orElse("?"));
            return Optional.empty();
        }

        persistence.saveNow(block);
        visuals.sync(block);

        MessageKeys.BLOCK_CREATED.send(player, "{id}", id, "{type}", MessageKeys.getTypeName(type));
        return Optional.of(block);
    }

    @NotNull
    public Optional<ClickableBlock> duplicate(@NotNull Player player, @NotNull ClickableBlock source, @NotNull String rawId) {
        ClickableType type = source.getType();
        BlockPosition position = resolveTarget(player, type);

        if (position == null) {
            MessageKeys.BLOCK_NO_TARGET.send(player);
            return Optional.empty();
        }

        Optional<ClickableBlock> created = create(player, rawId, type, position);
        created.ifPresent(copy -> {
            copy.copySettingsFrom(source);
            changed(copy);
            MessageKeys.BLOCK_CLONED.send(player, "{id}", copy.getId(), "{source}", source.getId());
        });

        return created;
    }

    public void delete(@NotNull ClickableBlock block) {
        if (manager.unregister(block.getId()) == null) return;

        visuals.despawn(block.getId());
        persistence.delete(block.getId());
        cooldowns.clear(block.getId());
        usages.forget(block.getId());
        closeEditors(block);
    }

    public void changed(@NotNull ClickableBlock block) {
        if (manager.get(block.getId()).orElse(null) != block) return;

        visuals.sync(block);
        persistence.markDirty(block);
    }

    public boolean move(@NotNull Player player, @NotNull ClickableBlock block, @NotNull BlockPosition target) {
        if (block.getType() == ClickableType.BLOCK) {
            Block physical = target.getBlock();
            if (physical == null || physical.getType().isAir()) {
                MessageKeys.BLOCK_NO_TARGET.send(player);
                return false;
            }
        }

        if (!manager.move(block, target)) {
            MessageKeys.BLOCK_POSITION_TAKEN.send(player, "{id}", manager.getAt(target).map(ClickableBlock::getId).orElse("?"));
            return false;
        }

        visuals.despawn(block.getId());
        changed(block);
        MessageKeys.BLOCK_MOVED.send(player, "{id}", block.getId(), "{location}", target.format());
        return true;
    }

    public boolean changeType(@NotNull Player player, @NotNull ClickableBlock block, @NotNull ClickableType type) {
        if (type == ClickableType.BLOCK) {
            Block physical = block.getPosition().getBlock();

            if (physical != null && physical.getType().isAir()) {
                Material material = block.getDisplay().getMaterial();
                physical.setType(material.isBlock() && !material.isAir() ? material : ItemUtil.material(ConfigKeys.EDITOR_DEFAULT_BLOCK.getRawString("GOLD_BLOCK"), Material.GOLD_BLOCK), false);
            }
        }

        if (!manager.changeType(block, type)) {
            MessageKeys.BLOCK_POSITION_TAKEN.send(player, "{id}", manager.getAt(block.getPosition()).map(ClickableBlock::getId).orElse("?"));
            return false;
        }

        if (type == ClickableType.BLOCK_DISPLAY && !block.getDisplay().getMaterial().isBlock()) {
            block.getDisplay().setItem(new ItemStack(ItemUtil.material(ConfigKeys.EDITOR_DEFAULT_BLOCK.getRawString("GOLD_BLOCK"), Material.GOLD_BLOCK)));
        }

        changed(block);
        MessageKeys.BLOCK_TYPE_CHANGED.send(player, "{id}", block.getId(), "{type}", MessageKeys.getTypeName(type));
        return true;
    }

    public void teleport(@NotNull Player player, @NotNull ClickableBlock block) {
        Location target = visuals.getDisplayLocation(block);

        if (target == null) {
            MessageKeys.BLOCK_WORLD_NOT_LOADED.send(player, "{world}", block.getPosition().world());
            return;
        }

        Location destination = target.clone().add(0, block.getType() == ClickableType.BLOCK ? 0.5 : 0, 0);
        Vector back = player.getLocation().getDirection().setY(0);
        if (back.lengthSquared() < 1.0E-4) back = new Vector(0, 0, 1);
        destination.subtract(back.normalize().multiply(2.0));
        destination.setDirection(target.toVector().subtract(destination.toVector()));

        player.teleportAsync(destination).thenAccept(success -> {
            if (success) ClickableBlocks.getScheduler().runTask(player, () -> MessageKeys.BLOCK_TELEPORTED.send(player, "{id}", block.getId()));
        });
    }

    public boolean setPhysicalBlock(@NotNull Player player, @NotNull ClickableBlock block, @NotNull Material material) {
        if (!material.isBlock() || material.isAir()) {
            MessageKeys.BLOCK_NOT_A_BLOCK.send(player);
            return false;
        }

        Block physical = block.getPosition().getBlock();
        if (physical == null) {
            MessageKeys.BLOCK_WORLD_NOT_LOADED.send(player, "{world}", block.getPosition().world());
            return false;
        }

        physical.setType(material, false);
        changed(block);
        MessageKeys.BLOCK_MATERIAL_SET.send(player, "{item}", ItemUtil.prettyName(material));
        return true;
    }

    @Nullable
    public BlockPosition resolveTarget(@NotNull Player player, @NotNull ClickableType type) {
        int reach = Math.clamp(ConfigKeys.EDITOR_REACH.getInt(6), 2, 32);
        RayTraceResult result = player.rayTraceBlocks(reach);
        Block hit = result == null ? null : result.getHitBlock();

        if (type == ClickableType.BLOCK) return hit == null || hit.getType().isAir() ? null : BlockPosition.of(hit);

        if (hit != null && result.getHitBlockFace() != null) return BlockPosition.of(hit.getRelative(result.getHitBlockFace()));

        Location eye = player.getEyeLocation();
        return BlockPosition.of(eye.add(eye.getDirection().multiply(2.0)));
    }

    @NotNull
    public ItemStack createWand() {
        Section section = ConfigKeys.WAND.getSection();
        ItemStack wand = ItemFactory.buildItem(section, "wand").orElse(null);
        if (wand == null) wand = new ItemStack(Material.BLAZE_ROD);

        wand.editMeta(meta -> meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1));
        return wand;
    }

    public boolean isWand(@Nullable ItemStack item) {
        if (ItemUtil.isEmpty(item) || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    public void closeEditors(@NotNull ClickableBlock block) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!(online.getOpenInventory().getTopInventory().getHolder(false) instanceof BlockBoundMenu bound)) continue;
            if (bound.getBlock() != block) continue;

            ClickableBlocks.getScheduler().runTask(online, () -> {
                online.closeInventory();
                MessageKeys.EDITOR_BLOCK_REMOVED.send(online, "{id}", block.getId());
            });
        }
    }

    @NotNull
    private ItemStack defaultItem(@NotNull Player player, @NotNull ClickableType type) {
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (!ItemUtil.isEmpty(hand) && !isWand(hand)) {
            if (type != ClickableType.BLOCK_DISPLAY || hand.getType().isBlock()) return hand.clone();
        }

        if (type == ClickableType.BLOCK) {
            Block physical = player.getTargetBlockExact(Math.clamp(ConfigKeys.EDITOR_REACH.getInt(6), 2, 32));
            if (physical != null && physical.getType().isItem()) return new ItemStack(physical.getType());
        }

        String configured = type == ClickableType.ITEM_DISPLAY
                ? ConfigKeys.EDITOR_DEFAULT_ITEM.getRawString("DIAMOND_BLOCK")
                : ConfigKeys.EDITOR_DEFAULT_BLOCK.getRawString("GOLD_BLOCK");

        Material material = ItemUtil.material(configured, type == ClickableType.ITEM_DISPLAY ? Material.DIAMOND_BLOCK : Material.GOLD_BLOCK);
        if (type == ClickableType.BLOCK_DISPLAY && !material.isBlock()) material = Material.GOLD_BLOCK;
        return new ItemStack(material);
    }
}