package com.playerpicked.clickableblocks.hooks.impl.placeholder;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.identifiers.keys.MessageKeys;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import com.playerpicked.clickableblocks.utils.FormatUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class PlaceholderHook extends PlaceholderExpansion {

    @NotNull
    public static String apply(@NotNull Player player, @NotNull String text) {
        return text.indexOf('%') < 0 ? text : PlaceholderAPI.setPlaceholders(player, text);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "clickableblocks";
    }

    @Override
    public @NotNull String getAuthor() {
        return "kxtsoo";
    }

    @Override
    public @NotNull String getVersion() {
        return ClickableBlocks.getInstance().getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        ClickableBlocks plugin = ClickableBlocks.getInstance();
        String key = params.toLowerCase(Locale.ROOT);

        if (key.equals("total")) return String.valueOf(plugin.getBlockManager().size());

        int split = key.indexOf('_');
        if (split <= 0) return null;

        String field = key.substring(0, split);
        ClickableBlock block = plugin.getBlockManager().get(key.substring(split + 1)).orElse(null);
        if (block == null) return MessageKeys.PLACEHOLDER_NONE.getMessage();

        return switch (field) {
            case "enabled" -> block.isEnabled() ? MessageKeys.PLACEHOLDER_YES.getMessage() : MessageKeys.PLACEHOLDER_NO.getMessage();
            case "type" -> MessageKeys.getTypeName(block.getType());
            case "location" -> block.getPosition().format();
            case "uses" -> player == null ? "0" : String.valueOf(plugin.getUsageManager().getUses(block.getId(), player.getUniqueId()));
            case "remaining" -> {
                if (block.getMaxUses() <= 0) yield MessageKeys.PLACEHOLDER_UNLIMITED.getMessage();
                int used = player == null ? 0 : plugin.getUsageManager().getUses(block.getId(), player.getUniqueId());
                yield String.valueOf(Math.max(0, block.getMaxUses() - used));
            }
            case "cooldown" -> {
                long remaining = player == null ? 0 : plugin.getCooldownManager().getRemaining(block.getId(), player.getUniqueId());
                yield remaining <= 0 ? MessageKeys.PLACEHOLDER_READY.getMessage() : FormatUtil.duration(remaining);
            }
            default -> null;
        };
    }
}