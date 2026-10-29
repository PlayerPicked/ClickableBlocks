package com.playerpicked.clickableblocks.item;

import com.playerpicked.clickableblocks.processor.MessageProcessor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("all")
public class ItemBuilder implements ItemFactory {
    private final ItemStack is;
    private final ItemMeta meta;

    public ItemBuilder(@NotNull Material type, @Range(from = 0, to = 64) int amount) {
        this.is = new ItemStack(type, Math.max(1, Math.min(64, amount)));
        this.meta = is.getItemMeta();
    }

    @Override
    public ItemBuilder setName(@NotNull String name) {
        meta.setDisplayName(MessageProcessor.process(name));
        return this;
    }

    @Override
    public ItemBuilder addLore(@NotNull String... lores) {
        List<String> loreList = Arrays.asList(lores);
        List<String> currentLores = meta.getLore();
        currentLores = currentLores == null ? Collections.synchronizedList(new ArrayList<>()) : currentLores;

        currentLores.addAll(loreList);
        meta.setLore(currentLores);

        return this;
    }

    @Override
    public ItemStack finish() {
        is.setItemMeta(meta);
        return is;
    }
}