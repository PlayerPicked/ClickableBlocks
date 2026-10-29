package com.playerpicked.clickableblocks.utils;

import com.playerpicked.clickableblocks.identifiers.keys.MessageKeys;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@UtilityClass
@SuppressWarnings("deprecation")
public final class ItemUtil {

    public void replace(@NotNull ItemStack item, @NotNull String... replacements) {
        if (replacements.length < 2) return;

        item.editMeta(meta -> {
            if (meta.hasDisplayName()) meta.setDisplayName(MessageKeys.replace(meta.getDisplayName(), replacements));

            List<String> lore = meta.getLore();
            if (lore == null || lore.isEmpty()) return;

            List<String> result = new ArrayList<>(lore.size());
            for (String line : lore) {
                String replaced = MessageKeys.replace(line, replacements);
                if (replaced.indexOf('\n') < 0) {
                    result.add(replaced);
                    continue;
                }

                String color = line.startsWith("§x") && line.length() >= 14 ? line.substring(0, 14) : "";
                String[] parts = replaced.split("\n", -1);
                result.add(parts[0]);
                for (int index = 1; index < parts.length; index++) result.add(color + parts[index]);
            }

            meta.setLore(result);
        });
    }

    @NotNull
    public String serialize(@NotNull ItemStack item) {
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    @Nullable
    public ItemStack deserialize(@Nullable String data) {
        if (data == null || data.isEmpty()) return null;

        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(data));
        } catch (Exception exception) {
            LoggerUtils.warn("Could not read a stored display item: {}", exception.getMessage());
            return null;
        }
    }

    public boolean isEmpty(@Nullable ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    @NotNull
    public Material material(@Nullable String raw, @NotNull Material fallback) {
        if (raw == null || raw.isBlank()) return fallback;

        Material material = Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT));
        return material == null || material.isAir() ? fallback : material;
    }

    @NotNull
    public String prettyName(@NotNull Material material) {
        String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }

        return builder.toString();
    }
}