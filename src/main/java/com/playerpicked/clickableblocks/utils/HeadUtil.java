package com.playerpicked.clickableblocks.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.hooks.impl.head.HeadDatabaseHook;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@UtilityClass
public final class HeadUtil {
    private static final Pattern NAME = Pattern.compile("^[A-Za-z0-9_]{1,16}$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}$");
    private static final Pattern HASH = Pattern.compile("^[0-9a-fA-F]{32,128}$");
    private static final String TEXTURE_URL = "http://textures.minecraft.net/texture/";

    public enum Failure {
        INVALID,
        NOT_FOUND,
        HDB_MISSING
    }

    public record Result(@Nullable ItemStack head, @Nullable Failure failure) {
        public boolean success() {
            return head != null;
        }

        @NotNull
        static Result of(@NotNull ItemStack head) {
            return new Result(head, null);
        }

        @NotNull
        static Result fail(@NotNull Failure failure) {
            return new Result(null, failure);
        }
    }

    public boolean isSkull(@NotNull Material material) {
        return Tag.ITEMS_SKULLS.isTagged(material);
    }

    public boolean isPlayerHead(@Nullable ItemStack item) {
        return item != null && (item.getType() == Material.PLAYER_HEAD || item.getType() == Material.PLAYER_WALL_HEAD);
    }

    @NotNull
    public CompletableFuture<Result> resolve(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return CompletableFuture.completedFuture(Result.fail(Failure.INVALID));

        String input = raw.trim();
        String prefix = "";
        String value = input;

        int colon = input.indexOf(':');
        if (colon > 0 && !input.regionMatches(true, 0, "http", 0, 4)) {
            prefix = input.substring(0, colon).toLowerCase(Locale.ROOT);
            value = input.substring(colon + 1).trim();
        }

        String finalValue = value;
        return switch (prefix) {
            case "hdb", "headdatabase" -> CompletableFuture.completedFuture(fromHeadDatabase(finalValue));
            case "texture", "base64", "value" -> CompletableFuture.completedFuture(fromTexture(finalValue));
            case "url", "hash" -> CompletableFuture.completedFuture(fromUrl(finalValue));
            case "uuid", "id" -> async(() -> fromUuid(finalValue));
            case "player", "name" -> async(() -> fromName(finalValue));
            case "" -> autodetect(finalValue);
            default -> CompletableFuture.completedFuture(Result.fail(Failure.INVALID));
        };
    }

    @NotNull
    private CompletableFuture<Result> autodetect(@NotNull String value) {
        if (value.regionMatches(true, 0, "http", 0, 4) || (HASH.matcher(value).matches() && !UUID_PATTERN.matcher(value).matches())) {
            return CompletableFuture.completedFuture(fromUrl(value));
        }
        if (UUID_PATTERN.matcher(value).matches()) return async(() -> fromUuid(value));
        if (NAME.matcher(value).matches()) return async(() -> fromName(value));
        return CompletableFuture.completedFuture(fromTexture(value));
    }

    @NotNull
    private CompletableFuture<Result> async(@NotNull java.util.function.Supplier<Result> supplier) {
        return CompletableFuture.supplyAsync(supplier).exceptionally(throwable -> {
            LoggerUtils.warn("Head lookup failed: {}", throwable.getMessage());
            return Result.fail(Failure.NOT_FOUND);
        });
    }

    @NotNull
    private Result fromName(@NotNull String name) {
        if (!NAME.matcher(name).matches()) return Result.fail(Failure.INVALID);
        return complete(Bukkit.createProfile(name));
    }

    @NotNull
    private Result fromUuid(@NotNull String raw) {
        UUID uuid = parseUuid(raw);
        if (uuid == null) return Result.fail(Failure.INVALID);
        return complete(Bukkit.createProfile(uuid));
    }

    @NotNull
    private Result complete(@NotNull PlayerProfile profile) {
        if (!profile.complete(true) || !profile.hasTextures()) return Result.fail(Failure.NOT_FOUND);
        return Result.of(head(profile));
    }

    @NotNull
    private Result fromUrl(@NotNull String raw) {
        String hash = raw.trim();
        int slash = hash.lastIndexOf('/');
        if (slash >= 0) hash = hash.substring(slash + 1);
        if (!HASH.matcher(hash).matches()) return Result.fail(Failure.INVALID);

        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + TEXTURE_URL + hash.toLowerCase(Locale.ROOT) + "\"}}}";
        return fromTexture(Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8)));
    }

    @NotNull
    private Result fromTexture(@NotNull String raw) {
        String value = raw.trim();

        try {
            String decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            if (!decoded.contains("\"textures\"")) return Result.fail(Failure.INVALID);
        } catch (IllegalArgumentException exception) {
            return Result.fail(Failure.INVALID);
        }

        UUID uuid = UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
        PlayerProfile profile = Bukkit.createProfile(uuid, null);
        profile.setProperty(new ProfileProperty("textures", value));
        return Result.of(head(profile));
    }

    @NotNull
    private Result fromHeadDatabase(@NotNull String id) {
        HeadDatabaseHook hook = ClickableBlocks.getInstance().getHookManager().getHeadDatabase();
        if (hook == null) return Result.fail(Failure.HDB_MISSING);

        ItemStack head = hook.getHead(id);
        return head == null || ItemUtil.isEmpty(head) ? Result.fail(Failure.NOT_FOUND) : Result.of(head);
    }

    @NotNull
    public ItemStack head(@NotNull PlayerProfile profile) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        item.editMeta(SkullMeta.class, meta -> meta.setPlayerProfile(profile));
        return item;
    }

    @Nullable
    public PlayerProfile profile(@Nullable ItemStack item) {
        if (!isPlayerHead(item) || !(item.getItemMeta() instanceof SkullMeta meta)) return null;
        return meta.getPlayerProfile();
    }

    public void place(@NotNull Block block, @NotNull ItemStack item) {
        block.setType(item.getType(), false);

        PlayerProfile profile = profile(item);
        if (profile == null) return;

        BlockState state = block.getState();
        if (!(state instanceof Skull skull)) return;

        skull.setPlayerProfile(profile);
        skull.update(true, false);
    }

    @NotNull
    public ItemStack toItem(@NotNull Block block) {
        Material material = block.getType() == Material.PLAYER_WALL_HEAD ? Material.PLAYER_HEAD : block.getType();
        ItemStack item = new ItemStack(material.isItem() ? material : Material.STONE);

        if (block.getState(false) instanceof Skull skull && skull.getPlayerProfile() != null) {
            item.editMeta(SkullMeta.class, meta -> meta.setPlayerProfile(skull.getPlayerProfile()));
        }
        return item;
    }

    @Nullable
    public String owner(@Nullable ItemStack item) {
        PlayerProfile profile = profile(item);
        if (profile == null) return null;

        String name = profile.getName();
        return name == null || name.isBlank() ? "" : name;
    }

    @Nullable
    private UUID parseUuid(@NotNull String raw) {
        String value = raw.trim();
        if (!UUID_PATTERN.matcher(value).matches()) return null;

        if (value.indexOf('-') < 0) {
            value = value.substring(0, 8) + "-" + value.substring(8, 12) + "-" + value.substring(12, 16) + "-" + value.substring(16, 20) + "-" + value.substring(20);
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}