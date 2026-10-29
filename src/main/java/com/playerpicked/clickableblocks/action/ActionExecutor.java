package com.playerpicked.clickableblocks.action;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.hooks.HookManager;
import com.playerpicked.clickableblocks.hooks.impl.economy.VaultHook;
import com.playerpicked.clickableblocks.identifiers.ActionType;
import com.playerpicked.clickableblocks.model.BlockPosition;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import com.playerpicked.clickableblocks.processor.MessageProcessor;
import com.playerpicked.clickableblocks.utils.FormatUtil;
import com.playerpicked.clickableblocks.utils.LoggerUtils;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ActionExecutor {
    private static final long MAX_DELAY = 20L * 60L * 60L;

    private final ClickableBlocks plugin = ClickableBlocks.getInstance();
    private final HookManager hookManager;

    public ActionExecutor(@NotNull HookManager hookManager) {
        this.hookManager = hookManager;
    }

    public void execute(@NotNull Player player, @NotNull ClickableBlock block, @NotNull List<String> lines) {
        UUID uuid = player.getUniqueId();
        BlockPosition position = block.getPosition();
        long delay = 0;

        for (String line : List.copyOf(lines)) {
            ParsedAction action = ParsedAction.parse(line);

            if (action == null) {
                LoggerUtils.warn("Skipping invalid action '{}' on clickable block '{}'.", line, block.getId());
                continue;
            }

            if (action.type() == ActionType.DELAY) {
                Integer ticks = FormatUtil.parseInt(action.value());
                if (ticks != null && ticks > 0) delay = Math.min(MAX_DELAY, delay + ticks);
                continue;
            }

            if (delay <= 0) {
                run(player, block.getId(), position, action);
                continue;
            }

            ClickableBlocks.getScheduler().runTaskLater(() -> {
                Player online = Bukkit.getPlayer(uuid);
                if (online != null && online.isOnline()) run(online, block.getId(), position, action);
            }, delay);
        }
    }

    private void run(@NotNull Player player, @NotNull String blockId, @NotNull BlockPosition position, @NotNull ParsedAction action) {
        String value = placeholders(player, blockId, position, action.value());

        try {
            switch (action.type()) {
                case PLAYER -> player.performCommand(value);
                case CONSOLE -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value);
                case MESSAGE -> {
                    for (String line : value.split("\\\\n|\n")) player.sendMessage(MessageProcessor.toComponent(MessageProcessor.process(line)));
                }
                case BROADCAST -> {
                    for (String line : value.split("\\\\n|\n")) Bukkit.broadcast(MessageProcessor.toComponent(MessageProcessor.process(line)));
                }
                case ACTIONBAR -> player.sendActionBar(MessageProcessor.toComponent(MessageProcessor.process(value)));
                case TITLE -> title(player, value);
                case SOUND -> sound(player, value);
                case PARTICLE -> particle(player, position, value);
                case EFFECT -> effect(player, value);
                case TELEPORT -> teleport(player, value);
                case SERVER -> server(player, value);
                case MONEY -> money(player, blockId, value);
                case CLOSE -> player.closeInventory();
                case DELAY -> {
                }
            }
        } catch (Exception exception) {
            LoggerUtils.warn("Action '{}' of clickable block '{}' failed: {}", action.type().format(action.value()), blockId, exception.getMessage());
        }
    }

    @NotNull
    private String placeholders(@NotNull Player player, @NotNull String blockId, @NotNull BlockPosition position, @NotNull String value) {
        String result = value
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{block}", blockId)
                .replace("{world}", position.world())
                .replace("{x}", String.valueOf(position.x()))
                .replace("{y}", String.valueOf(position.y()))
                .replace("{z}", String.valueOf(position.z()));

        return hookManager.applyPlaceholders(player, result);
    }

    private void title(@NotNull Player player, @NotNull String value) {
        String[] parts = value.split(";", -1);

        int fadeIn = parts.length > 2 ? intOr(parts[2], 10) : 10;
        int stay = parts.length > 3 ? intOr(parts[3], 40) : 40;
        int fadeOut = parts.length > 4 ? intOr(parts[4], 10) : 10;

        player.showTitle(Title.title(
                MessageProcessor.toComponent(MessageProcessor.process(parts[0])),
                MessageProcessor.toComponent(MessageProcessor.process(parts.length > 1 ? parts[1] : "")),
                Title.Times.times(ticks(fadeIn), ticks(stay), ticks(fadeOut))));
    }

    private void sound(@NotNull Player player, @NotNull String value) {
        String[] parts = value.split(";");
        if (parts[0].isBlank()) return;

        float volume = parts.length > 1 ? floatOr(parts[1], 1f) : 1f;
        float pitch = parts.length > 2 ? floatOr(parts[2], 1f) : 1f;

        player.playSound(player.getLocation(), parts[0].trim().toLowerCase(Locale.ROOT), volume, pitch);
    }

    private void particle(@NotNull Player player, @NotNull BlockPosition position, @NotNull String value) {
        String[] parts = value.split(";");
        Particle particle = Particle.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
        if (particle.getDataType() != Void.class) return;

        int count = parts.length > 1 ? Math.clamp(intOr(parts[1], 10), 1, 500) : 10;
        Location location = position.center();
        if (location == null) return;

        player.getWorld().spawnParticle(particle, location.add(0, 0.5, 0), count, 0.4, 0.4, 0.4, 0.02);
    }

    private void effect(@NotNull Player player, @NotNull String value) {
        String[] parts = value.split(";");
        NamespacedKey key = NamespacedKey.fromString(parts[0].trim().toLowerCase(Locale.ROOT));
        PotionEffectType type = key == null ? null : Registry.EFFECT.get(key);

        if (type == null) {
            LoggerUtils.warn("Unknown potion effect '{}'.", parts[0]);
            return;
        }

        int seconds = parts.length > 1 ? Math.max(1, intOr(parts[1], 10)) : 10;
        int amplifier = parts.length > 2 ? Math.clamp(intOr(parts[2], 1) - 1, 0, 255) : 0;

        player.addPotionEffect(new PotionEffect(type, seconds * 20, amplifier));
    }

    private void teleport(@NotNull Player player, @NotNull String value) {
        String[] parts = value.split(";");
        if (parts.length < 4) return;

        World world = Bukkit.getWorld(parts[0].trim());
        if (world == null) {
            LoggerUtils.warn("Teleport action points to an unknown world '{}'.", parts[0]);
            return;
        }

        Double x = FormatUtil.parseDouble(parts[1]);
        Double y = FormatUtil.parseDouble(parts[2]);
        Double z = FormatUtil.parseDouble(parts[3]);
        if (x == null || y == null || z == null) return;

        float yaw = parts.length > 4 ? floatOr(parts[4], player.getYaw()) : player.getYaw();
        float pitch = parts.length > 5 ? floatOr(parts[5], player.getPitch()) : player.getPitch();

        player.teleportAsync(new Location(world, x, y, z, yaw, pitch));
    }

    private void server(@NotNull Player player, @NotNull String value) {
        if (value.isBlank()) return;

        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF("Connect");
        output.writeUTF(value.trim());
        player.sendPluginMessage(plugin, "BungeeCord", output.toByteArray());
    }

    private void money(@NotNull Player player, @NotNull String blockId, @NotNull String value) {
        VaultHook economy = hookManager.getEconomy();
        Double amount = FormatUtil.parseDouble(value);

        if (economy == null || amount == null || amount == 0) {
            if (economy == null) LoggerUtils.warn("Clickable block '{}' uses [money] but Vault is not available.", blockId);
            return;
        }

        if (amount > 0) economy.deposit(player, amount);
        else economy.withdraw(player, -amount);
    }

    @NotNull
    private static Duration ticks(int ticks) {
        return Duration.ofMillis(Math.max(0, ticks) * 50L);
    }

    private static int intOr(@NotNull String raw, int def) {
        Integer value = FormatUtil.parseInt(raw);
        return value == null ? def : value;
    }

    private static float floatOr(@NotNull String raw, float def) {
        Double value = FormatUtil.parseDouble(raw);
        return value == null ? def : value.floatValue();
    }
}