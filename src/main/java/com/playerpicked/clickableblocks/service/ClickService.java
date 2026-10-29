package com.playerpicked.clickableblocks.service;

import com.playerpicked.clickableblocks.action.ActionExecutor;
import com.playerpicked.clickableblocks.hooks.HookManager;
import com.playerpicked.clickableblocks.hooks.impl.economy.VaultHook;
import com.playerpicked.clickableblocks.identifiers.ClickTrigger;
import com.playerpicked.clickableblocks.identifiers.keys.MessageKeys;
import com.playerpicked.clickableblocks.manager.CooldownManager;
import com.playerpicked.clickableblocks.manager.UsageManager;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import com.playerpicked.clickableblocks.utils.FormatUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public final class ClickService {
    public static final String USE_PERMISSION = "clickableblocks.use";
    public static final String BYPASS_PERMISSION = "clickableblocks.bypass";

    private final CooldownManager cooldownManager;
    private final UsageManager usageManager;
    private final ActionExecutor actionExecutor;
    private final HookManager hookManager;

    public ClickService(@NotNull CooldownManager cooldownManager, @NotNull UsageManager usageManager, @NotNull ActionExecutor actionExecutor, @NotNull HookManager hookManager) {
        this.cooldownManager = cooldownManager;
        this.usageManager = usageManager;
        this.actionExecutor = actionExecutor;
        this.hookManager = hookManager;
    }

    public void handle(@NotNull Player player, @NotNull ClickableBlock block, @NotNull ClickTrigger trigger) {
        List<String> actions = block.resolveActions(trigger);
        if (actions.isEmpty()) return;

        UUID uuid = player.getUniqueId();
        if (!cooldownManager.tryDebounce(uuid)) return;

        if (!block.isEnabled()) {
            MessageKeys.CLICK_DISABLED.send(player);
            return;
        }

        if (!player.hasPermission(USE_PERMISSION) || (block.hasPermission() && !player.hasPermission(block.getPermission()))) {
            MessageKeys.CLICK_NO_PERMISSION.send(player);
            return;
        }

        boolean bypass = player.hasPermission(BYPASS_PERMISSION);

        long remaining = cooldownManager.getRemaining(block.getId(), uuid);
        if (!bypass && remaining > 0) {
            MessageKeys.CLICK_COOLDOWN.send(player, "{time}", FormatUtil.duration(remaining));
            return;
        }

        int maxUses = block.getMaxUses();
        if (!bypass && maxUses > 0 && usageManager.getUses(block.getId(), uuid) >= maxUses) {
            MessageKeys.CLICK_MAX_USES.send(player, "{max}", String.valueOf(maxUses));
            return;
        }

        double cost = block.getCost();
        if (cost > 0) {
            VaultHook economy = hookManager.getEconomy();

            if (economy == null) {
                MessageKeys.CLICK_ECONOMY_UNAVAILABLE.send(player);
                return;
            }

            if (!economy.has(player, cost) || !economy.withdraw(player, cost)) {
                MessageKeys.CLICK_NOT_ENOUGH_MONEY.send(player, "{cost}", economy.format(cost));
                return;
            }

            MessageKeys.CLICK_PAID.send(player, "{cost}", economy.format(cost));
        }

        if (!bypass) cooldownManager.apply(block.getId(), uuid, block.getCooldown());
        if (maxUses > 0) usageManager.increment(block.getId(), uuid);

        actionExecutor.execute(player, block, actions);
    }

    public boolean forceRun(@NotNull Player player, @NotNull ClickableBlock block, @NotNull ClickTrigger trigger) {
        List<String> actions = block.resolveActions(trigger);
        if (actions.isEmpty()) return false;

        actionExecutor.execute(player, block, actions);
        return true;
    }
}