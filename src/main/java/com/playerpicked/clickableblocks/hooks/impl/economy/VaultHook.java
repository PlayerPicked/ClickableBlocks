package com.playerpicked.clickableblocks.hooks.impl.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record VaultHook(@NotNull Economy economy) {

    @Nullable
    public static VaultHook create() {
        RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        return registration == null ? null : new VaultHook(registration.getProvider());
    }

    public boolean has(@NotNull OfflinePlayer player, double amount) {
        return economy.has(player, amount);
    }

    public boolean withdraw(@NotNull OfflinePlayer player, double amount) {
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(@NotNull OfflinePlayer player, double amount) {
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    @NotNull
    public String format(double amount) {
        return economy.format(amount);
    }

    @NotNull
    public String name() {
        return economy.getName();
    }
}