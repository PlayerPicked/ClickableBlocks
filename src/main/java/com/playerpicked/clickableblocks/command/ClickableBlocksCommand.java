package com.playerpicked.clickableblocks.command;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.command.suggestion.BlockIdSuggestion;
import com.playerpicked.clickableblocks.command.suggestion.TriggerSuggestion;
import com.playerpicked.clickableblocks.command.suggestion.TypeSuggestion;
import com.playerpicked.clickableblocks.data.MenuController;
import com.playerpicked.clickableblocks.guis.impl.EditorMenu;
import com.playerpicked.clickableblocks.guis.impl.ListMenu;
import com.playerpicked.clickableblocks.identifiers.ClickTrigger;
import com.playerpicked.clickableblocks.identifiers.ClickableType;
import com.playerpicked.clickableblocks.identifiers.keys.ConfigKeys;
import com.playerpicked.clickableblocks.identifiers.keys.MessageKeys;
import com.playerpicked.clickableblocks.model.BlockPosition;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import com.playerpicked.clickableblocks.service.EditorService;
import com.playerpicked.clickableblocks.utils.Placeholders;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.CommandPlaceholder;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.annotation.SuggestWith;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command({"clickableblocks", "cb", "clickable"})
@CommandPermission(EditorService.ADMIN_PERMISSION)
@SuppressWarnings("unused")
public final class ClickableBlocksCommand {
    private final ClickableBlocks plugin = ClickableBlocks.getInstance();

    @CommandPlaceholder
    public void root(@NotNull BukkitCommandActor actor) {
        if (actor.sender() instanceof Player player) {
            new ListMenu(MenuController.getMenuUtils(player)).open();
            return;
        }

        MessageKeys.HELP.sendList(actor.sender());
    }

    @Subcommand("help")
    public void help(@NotNull BukkitCommandActor actor) {
        MessageKeys.HELP.sendList(actor.sender());
    }

    @Subcommand("reload")
    public void reload(@NotNull BukkitCommandActor actor) {
        plugin.reload();
        MessageKeys.RELOAD.send(actor.sender());
    }

    @Subcommand("list")
    public void list(@NotNull Player player) {
        new ListMenu(MenuController.getMenuUtils(player)).open();
    }

    @Subcommand("wand")
    public void wand(@NotNull Player player) {
        player.getInventory().addItem(plugin.getEditorService().createWand()).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));

        MessageKeys.WAND_RECEIVED.send(player);
        MessageKeys.WAND_HINT.sendList(player);
    }

    @Subcommand("create")
    public void create(@NotNull Player player, @NotNull String id, @Optional @SuggestWith(TypeSuggestion.class) @Nullable String type) {
        ClickableType resolved = type == null ? ClickableType.BLOCK : ClickableType.fromString(type);
        if (resolved == null) resolved = ClickableType.BLOCK;

        BlockPosition position = plugin.getEditorService().resolveTarget(player, resolved);
        if (position == null) {
            MessageKeys.BLOCK_NO_TARGET.send(player);
            return;
        }

        plugin.getEditorService().create(player, id, resolved, position)
                .ifPresent(block -> new EditorMenu(MenuController.getMenuUtils(player), block).open());
    }

    @Subcommand("edit")
    public void edit(@NotNull Player player, @Optional @SuggestWith(BlockIdSuggestion.class) @Nullable String id) {
        ClickableBlock block = id == null ? looking(player) : plugin.getBlockManager().get(id).orElse(null);

        if (block == null) {
            MessageKeys.BLOCK_NOT_FOUND.send(player, "{id}", id == null ? "?" : id);
            return;
        }

        new EditorMenu(MenuController.getMenuUtils(player), block).open();
    }

    @Subcommand("delete")
    public void delete(@NotNull BukkitCommandActor actor, @SuggestWith(BlockIdSuggestion.class) @NotNull String id) {
        ClickableBlock block = plugin.getBlockManager().get(id).orElse(null);

        if (block == null) {
            MessageKeys.BLOCK_NOT_FOUND.send(actor.sender(), "{id}", id);
            return;
        }

        plugin.getEditorService().delete(block);
        MessageKeys.BLOCK_DELETED.send(actor.sender(), "{id}", block.getId());
    }

    @Subcommand("info")
    public void info(@NotNull BukkitCommandActor actor, @Optional @SuggestWith(BlockIdSuggestion.class) @Nullable String id) {
        ClickableBlock block = id != null
                ? plugin.getBlockManager().get(id).orElse(null)
                : actor.sender() instanceof Player player ? looking(player) : null;

        if (block == null) {
            MessageKeys.BLOCK_NOT_FOUND.send(actor.sender(), "{id}", id == null ? "?" : id);
            return;
        }

        MessageKeys.BLOCK_INFO.sendList(actor.sender(), Placeholders.of(block));
    }

    @Subcommand("tp")
    public void teleport(@NotNull Player player, @SuggestWith(BlockIdSuggestion.class) @NotNull String id) {
        plugin.getBlockManager().get(id).ifPresentOrElse(
                block -> plugin.getEditorService().teleport(player, block),
                () -> MessageKeys.BLOCK_NOT_FOUND.send(player, "{id}", id));
    }

    @Subcommand("movehere")
    public void moveHere(@NotNull Player player, @SuggestWith(BlockIdSuggestion.class) @NotNull String id) {
        ClickableBlock block = plugin.getBlockManager().get(id).orElse(null);
        if (block == null) {
            MessageKeys.BLOCK_NOT_FOUND.send(player, "{id}", id);
            return;
        }

        BlockPosition target = plugin.getEditorService().resolveTarget(player, block.getType());
        if (target == null) {
            MessageKeys.BLOCK_NO_TARGET.send(player);
            return;
        }

        plugin.getEditorService().move(player, block, target);
    }

    @Subcommand("toggle")
    public void toggle(@NotNull BukkitCommandActor actor, @SuggestWith(BlockIdSuggestion.class) @NotNull String id) {
        ClickableBlock block = plugin.getBlockManager().get(id).orElse(null);
        if (block == null) {
            MessageKeys.BLOCK_NOT_FOUND.send(actor.sender(), "{id}", id);
            return;
        }

        block.setEnabled(!block.isEnabled());
        plugin.getEditorService().changed(block);
        (block.isEnabled() ? MessageKeys.BLOCK_ENABLED : MessageKeys.BLOCK_DISABLED).send(actor.sender(), "{id}", block.getId());
    }

    @Subcommand("resetuses")
    public void resetUses(@NotNull BukkitCommandActor actor, @SuggestWith(BlockIdSuggestion.class) @NotNull String id) {
        ClickableBlock block = plugin.getBlockManager().get(id).orElse(null);
        if (block == null) {
            MessageKeys.BLOCK_NOT_FOUND.send(actor.sender(), "{id}", id);
            return;
        }

        plugin.getUsageManager().reset(block.getId());
        plugin.getCooldownManager().clear(block.getId());
        MessageKeys.BLOCK_USES_RESET.send(actor.sender(), "{id}", block.getId());
    }

    @Subcommand("run")
    public void run(@NotNull BukkitCommandActor actor, @SuggestWith(BlockIdSuggestion.class) @NotNull String id, @NotNull Player target,
                    @Optional @SuggestWith(TriggerSuggestion.class) @Nullable String trigger) {
        ClickableBlock block = plugin.getBlockManager().get(id).orElse(null);
        if (block == null) {
            MessageKeys.BLOCK_NOT_FOUND.send(actor.sender(), "{id}", id);
            return;
        }

        ClickTrigger resolved = trigger == null ? ClickTrigger.RIGHT : ClickTrigger.fromString(trigger);
        if (resolved == null) resolved = ClickTrigger.RIGHT;

        if (!plugin.getClickService().forceRun(target, block, resolved)) {
            MessageKeys.BLOCK_NO_ACTIONS.send(actor.sender(), "{trigger}", MessageKeys.getTriggerName(resolved));
            return;
        }

        MessageKeys.BLOCK_FORCE_RUN.send(actor.sender(), "{id}", block.getId(), "{player}", target.getName());
    }

    @Nullable
    private ClickableBlock looking(@NotNull Player player) {
        int reach = Math.clamp(ConfigKeys.EDITOR_REACH.getInt(6), 2, 32);

        Entity entity = player.getTargetEntity(reach);
        if (entity instanceof Interaction) {
            String id = plugin.getVisualService().getBlockId(entity);
            if (id != null) return plugin.getBlockManager().get(id).orElse(null);
        }

        Block block = player.getTargetBlockExact(reach);
        return block == null ? null : plugin.getBlockManager().getAt(BlockPosition.of(block)).orElse(null);
    }
}