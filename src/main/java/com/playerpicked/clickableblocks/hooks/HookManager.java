package com.playerpicked.clickableblocks.hooks;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.hooks.impl.economy.VaultHook;
import com.playerpicked.clickableblocks.hooks.impl.placeholder.PlaceholderHook;
import com.playerpicked.clickableblocks.utils.LoggerUtils;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class HookManager {
    private static final String GREEN = "\u001B[32m";
    private static final String RESET = "\u001B[0m";

    private final ClickableBlocks plugin = ClickableBlocks.getInstance();

    @Getter @Nullable private volatile VaultHook economy;
    @Nullable private volatile PlaceholderHook placeholder;

    public void load() {
        loadVault();
        loadPlaceholderApi();
    }

    public void unload() {
        economy = null;

        PlaceholderHook current = placeholder;
        if (current != null) {
            current.unregister();
            placeholder = null;
        }
    }

    public void reload() {
        unload();
        load();
    }

    @NotNull
    public String applyPlaceholders(@NotNull Player player, @NotNull String text) {
        return placeholder != null ? PlaceholderHook.apply(player, text) : text;
    }

    private void loadVault() {
        economy = null;
        if (!isEnabled("vault")) return;

        if (!isPluginPresent("Vault")) {
            LoggerUtils.warn("   [Hook] Vault not found, skipping. Click costs and [money] actions are disabled.");
            return;
        }

        VaultHook hook = VaultHook.create();
        if (hook == null) {
            LoggerUtils.warn("   [Hook] Vault found but no Economy provider. Click costs and [money] actions are disabled.");
            return;
        }

        economy = hook;
        LoggerUtils.info("{}   [Hook] Vault successfully enabled ({}).{}", GREEN, hook.name(), RESET);
    }

    private void loadPlaceholderApi() {
        if (placeholder != null) {
            placeholder.unregister();
            placeholder = null;
        }

        if (!isEnabled("placeholderapi")) return;

        if (!isPluginPresent("PlaceholderAPI")) {
            LoggerUtils.warn("   [Hook] PlaceholderAPI not found, skipping.");
            return;
        }

        PlaceholderHook hook = new PlaceholderHook();
        if (!hook.register()) {
            LoggerUtils.warn("   [Hook] PlaceholderAPI found but the expansion could not be registered.");
            return;
        }

        placeholder = hook;
        LoggerUtils.info("{}   [Hook] PlaceholderAPI successfully enabled.{}", GREEN, RESET);
    }

    private boolean isEnabled(@NotNull String hook) {
        return plugin.getHooks().getBoolean("hooks." + hook + ".enabled", true);
    }

    private boolean isPluginPresent(@NotNull String name) {
        Plugin found = Bukkit.getPluginManager().getPlugin(name);
        return found != null && found.isEnabled();
    }
}