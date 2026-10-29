package com.playerpicked.clickableblocks.service;

import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.identifiers.ClickableType;
import com.playerpicked.clickableblocks.identifiers.keys.ConfigKeys;
import com.playerpicked.clickableblocks.manager.ClickableBlockManager;
import com.playerpicked.clickableblocks.model.BlockPosition;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import com.playerpicked.clickableblocks.model.DisplaySettings;
import com.playerpicked.clickableblocks.model.HologramSettings;
import com.playerpicked.clickableblocks.processor.MessageProcessor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class VisualService {
    private static final Color TRANSPARENT = Color.fromARGB(0, 0, 0, 0);

    private final ClickableBlockManager manager;
    private final NamespacedKey blockKey;
    private final Map<String, VisualHandle> handles = new ConcurrentHashMap<>();
    private final Map<String, Optional<Particle>> particleCache = new ConcurrentHashMap<>();

    private MyScheduledTask animationTask;
    private MyScheduledTask particleTask;
    private MyScheduledTask watchdogTask;
    private long animationTicks;

    public VisualService(@NotNull ClickableBlockManager manager) {
        this.manager = manager;
        this.blockKey = new NamespacedKey(ClickableBlocks.getInstance(), "block");
    }

    public void start() {
        removeStrays();
        manager.all().forEach(this::sync);

        int animation = Math.max(1, ConfigKeys.VISUAL_ANIMATION_INTERVAL.getInt(2));
        int particles = Math.max(1, ConfigKeys.VISUAL_PARTICLE_INTERVAL.getInt(20));
        int watchdog = Math.max(20, ConfigKeys.VISUAL_WATCHDOG_INTERVAL.getInt(100));

        animationTask = ClickableBlocks.getScheduler().runTaskTimer(() -> animate(animation), animation, animation);
        particleTask = ClickableBlocks.getScheduler().runTaskTimer(this::particles, particles, particles);
        watchdogTask = ClickableBlocks.getScheduler().runTaskTimer(this::watchdog, watchdog, watchdog);
    }

    public void shutdown() {
        cancelTasks();
        despawnAll();
    }

    public void restart() {
        shutdown();
        particleCache.clear();
        start();
    }

    public void sync(@NotNull ClickableBlock block) {
        if (!Bukkit.isPrimaryThread()) {
            ClickableBlocks.getScheduler().runTask(() -> sync(block));
            return;
        }

        if (manager.get(block.getId()).orElse(null) != block) {
            despawn(block.getId());
            return;
        }

        BlockPosition position = block.getPosition();
        World world = position.getWorld();

        if (!block.isEnabled() || world == null || !world.isChunkLoaded(position.chunkX(), position.chunkZ())) {
            despawn(block.getId());
            return;
        }

        VisualHandle handle = handles.computeIfAbsent(block.getId(), id -> new VisualHandle());
        handle.position = position;

        Location anchor = displayLocation(block, world);
        syncMain(block, handle, world, anchor);
        syncHitbox(block, handle, world, anchor);
        syncOutline(block, handle, world);
        syncHologram(block, handle, world, anchor);
    }

    public void despawn(@NotNull String id) {
        VisualHandle handle = handles.remove(id);
        if (handle != null) handle.removeAll();
    }

    public void despawnAll() {
        handles.values().forEach(VisualHandle::removeAll);
        handles.clear();
    }

    public void onChunkLoad(@NotNull World world, int chunkX, int chunkZ) {
        manager.inChunk(world.getName(), chunkX, chunkZ).forEach(this::sync);
    }

    public void onChunkUnload(@NotNull World world, int chunkX, int chunkZ) {
        manager.inChunk(world.getName(), chunkX, chunkZ).forEach(block -> despawn(block.getId()));
    }

    public void onWorldLoad(@NotNull World world) {
        manager.inWorld(world.getName()).forEach(this::sync);
    }

    public void onWorldUnload(@NotNull World world) {
        manager.inWorld(world.getName()).forEach(block -> despawn(block.getId()));
    }

    @Nullable
    public String getBlockId(@NotNull Entity entity) {
        return entity.getPersistentDataContainer().get(blockKey, PersistentDataType.STRING);
    }

    public boolean isTagged(@NotNull Entity entity) {
        return entity.getPersistentDataContainer().has(blockKey, PersistentDataType.STRING);
    }

    public boolean isTracked(@NotNull Entity entity) {
        String id = getBlockId(entity);
        if (id == null) return false;

        VisualHandle handle = handles.get(id);
        return handle != null && handle.contains(entity);
    }

    @Nullable
    public Location getDisplayLocation(@NotNull ClickableBlock block) {
        World world = block.getPosition().getWorld();
        return world == null ? null : displayLocation(block, world);
    }

    private void syncMain(@NotNull ClickableBlock block, @NotNull VisualHandle handle, @NotNull World world, @NotNull Location anchor) {
        ClickableType type = block.getType();

        if (!type.isDisplay()) {
            handle.main = remove(handle.main);
            return;
        }

        Class<? extends Display> wanted = type == ClickableType.ITEM_DISPLAY ? ItemDisplay.class : BlockDisplay.class;

        if (!isValid(handle.main) || !wanted.isInstance(handle.main)) {
            remove(handle.main);
            handle.main = world.spawn(anchor, wanted, entity -> prepare(entity, block));
        } else teleportIfMoved(handle.main, anchor);

        Display display = handle.main;
        DisplaySettings settings = block.getDisplay();

        display.setBillboard(billboard(settings.getBillboard()));
        display.setViewRange(settings.getViewRange());
        display.setBrightness(settings.getBrightness() < 0 ? null : new Display.Brightness(settings.getBrightness(), settings.getBrightness()));
        display.setGlowing(settings.isGlow());
        display.setGlowColorOverride(settings.isGlow() ? Color.fromRGB(settings.getGlowColor() & 0xFFFFFF) : null);
        display.setShadowRadius(0f);
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(0);
        display.setTransformation(transformation(block, spinAngle(settings), bobOffset(settings)));

        if (display instanceof ItemDisplay itemDisplay) {
            itemDisplay.setItemStack(settings.getItem());
            itemDisplay.setItemDisplayTransform(itemTransform(settings.getTransform()));
        }

        if (display instanceof BlockDisplay blockDisplay) blockDisplay.setBlock(blockData(settings.getMaterial()));
    }

    private void syncHitbox(@NotNull ClickableBlock block, @NotNull VisualHandle handle, @NotNull World world, @NotNull Location anchor) {
        if (!block.getType().isDisplay()) {
            handle.hitbox = remove(handle.hitbox);
            return;
        }

        DisplaySettings settings = block.getDisplay();
        float width = settings.resolveHitboxWidth();
        float height = settings.resolveHitboxHeight();
        Location location = anchor.clone().subtract(0, height / 2f, 0);

        if (!isValid(handle.hitbox)) {
            remove(handle.hitbox);
            handle.hitbox = world.spawn(location, Interaction.class, entity -> prepare(entity, block));
        } else teleportIfMoved(handle.hitbox, location);

        handle.hitbox.setInteractionWidth(width);
        handle.hitbox.setInteractionHeight(height);
        handle.hitbox.setResponsive(true);
    }

    private void syncOutline(@NotNull ClickableBlock block, @NotNull VisualHandle handle, @NotNull World world) {
        BlockPosition position = block.getPosition();
        DisplaySettings settings = block.getDisplay();

        if (block.getType() != ClickableType.BLOCK || !settings.isGlow()) {
            handle.outline = remove(handle.outline);
            return;
        }

        Block physical = world.getBlockAt(position.x(), position.y(), position.z());
        if (physical.getType().isAir()) {
            handle.outline = remove(handle.outline);
            return;
        }

        Location location = new Location(world, position.x(), position.y(), position.z());

        if (!isValid(handle.outline)) {
            remove(handle.outline);
            handle.outline = world.spawn(location, BlockDisplay.class, entity -> prepare(entity, block));
        } else teleportIfMoved(handle.outline, location);

        BlockDisplay outline = handle.outline;
        outline.setBlock(physical.getBlockData());
        outline.setGlowing(true);
        outline.setGlowColorOverride(Color.fromRGB(settings.getGlowColor() & 0xFFFFFF));
        outline.setTransformation(new Transformation(new Vector3f(-0.001f), new Quaternionf(), new Vector3f(1.002f), new Quaternionf()));
    }

    private void syncHologram(@NotNull ClickableBlock block, @NotNull VisualHandle handle, @NotNull World world, @NotNull Location anchor) {
        HologramSettings hologram = block.getHologram();

        if (!hologram.isVisible()) {
            handle.hologram = remove(handle.hologram);
            return;
        }

        double top = block.getType().isDisplay() ? block.getDisplay().resolveHitboxHeight() / 2d : 0.5d;
        Location location = anchor.clone().add(0, top + hologram.getHeight(), 0);

        if (!isValid(handle.hologram)) {
            remove(handle.hologram);
            handle.hologram = world.spawn(location, TextDisplay.class, entity -> prepare(entity, block));
        } else teleportIfMoved(handle.hologram, location);

        TextDisplay text = handle.hologram;
        text.text(MessageProcessor.toComponent(hologram.getLines().stream()
                .map(MessageProcessor::process)
                .collect(Collectors.joining("\n§r"))));
        text.setBillboard(billboard(hologram.getBillboard()));
        text.setViewRange(hologram.getViewRange());
        text.setAlignment(TextDisplay.TextAlignment.CENTER);
        text.setLineWidth(Math.max(10, ConfigKeys.VISUAL_HOLOGRAM_LINE_WIDTH.getInt(200)));
        text.setShadowed(hologram.isShadow());
        text.setSeeThrough(false);
        text.setDefaultBackground(hologram.isBackground());
        if (!hologram.isBackground()) text.setBackgroundColor(TRANSPARENT);
    }

    private void prepare(@NotNull Entity entity, @NotNull ClickableBlock block) {
        entity.setPersistent(false);
        entity.setSilent(true);
        entity.getPersistentDataContainer().set(blockKey, PersistentDataType.STRING, block.getId());
    }

    @NotNull
    private Location displayLocation(@NotNull ClickableBlock block, @NotNull World world) {
        BlockPosition position = block.getPosition();
        DisplaySettings settings = block.getDisplay();

        if (!block.getType().isDisplay()) return new Location(world, position.x() + 0.5, position.y() + 0.5, position.z() + 0.5);

        return new Location(world,
                position.x() + 0.5 + settings.getOffsetX(),
                position.y() + 0.5 + settings.getOffsetY(),
                position.z() + 0.5 + settings.getOffsetZ());
    }

    @NotNull
    private Transformation transformation(@NotNull ClickableBlock block, float extraYaw, float bob) {
        DisplaySettings settings = block.getDisplay();

        Quaternionf rotation = new Quaternionf()
                .rotateY((float) Math.toRadians(-(settings.getYaw() + extraYaw)))
                .rotateX((float) Math.toRadians(settings.getPitch()))
                .rotateZ((float) Math.toRadians(settings.getRoll()));

        Vector3f scale = new Vector3f(settings.getScaleX(), settings.getScaleY(), settings.getScaleZ());
        Vector3f translation = new Vector3f(0f, bob, 0f);

        if (block.getType() == ClickableType.BLOCK_DISPLAY) {
            Vector3f half = new Vector3f(-0.5f * scale.x, -0.5f * scale.y, -0.5f * scale.z);
            rotation.transform(half);
            translation.add(half);
        }

        return new Transformation(translation, rotation, scale, new Quaternionf());
    }

    private float spinAngle(@NotNull DisplaySettings settings) {
        if (!settings.isSpin()) return 0f;
        return (animationTicks / 20f * settings.getSpinSpeed()) % 360f;
    }

    private float bobOffset(@NotNull DisplaySettings settings) {
        if (!settings.isBob()) return 0f;
        return (float) Math.sin(animationTicks / 40d * Math.PI * 2d) * settings.getBobHeight();
    }

    private void animate(int interval) {
        animationTicks += interval;

        for (ClickableBlock block : manager.all()) {
            DisplaySettings settings = block.getDisplay();
            if (!block.isEnabled() || !block.getType().isDisplay() || (!settings.isSpin() && !settings.isBob())) continue;

            VisualHandle handle = handles.get(block.getId());
            if (handle == null || !isValid(handle.main)) continue;

            handle.main.setInterpolationDelay(0);
            handle.main.setInterpolationDuration(interval);
            handle.main.setTransformation(transformation(block, spinAngle(settings), bobOffset(settings)));
        }
    }

    private void particles() {
        for (ClickableBlock block : manager.all()) {
            String name = block.getParticle();
            if (!block.isEnabled() || name == null || name.equalsIgnoreCase("NONE")) continue;

            BlockPosition position = block.getPosition();
            if (!position.isLoaded()) continue;

            Particle particle = particle(name);
            World world = position.getWorld();
            if (particle == null || world == null) continue;

            Location location = displayLocation(block, world);
            if (block.getType() == ClickableType.BLOCK) location.add(0, 0.3, 0);

            world.spawnParticle(particle, location, 6, 0.35, 0.35, 0.35, 0.01);
        }
    }

    private void watchdog() {
        for (ClickableBlock block : manager.all()) {
            boolean loaded = block.getPosition().isLoaded();
            VisualHandle handle = handles.get(block.getId());

            if (!loaded || !block.isEnabled()) {
                if (handle != null) despawn(block.getId());
                continue;
            }

            if (handle == null || !handle.isHealthy(block)) sync(block);
        }

        handles.keySet().removeIf(id -> {
            if (manager.exists(id)) return false;
            VisualHandle orphan = handles.get(id);
            if (orphan != null) orphan.removeAll();
            return true;
        });
    }

    private void removeStrays() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (isTagged(entity) && !isTracked(entity)) entity.remove();
            }
        }
    }

    private void cancelTasks() {
        if (animationTask != null) animationTask.cancel();
        if (particleTask != null) particleTask.cancel();
        if (watchdogTask != null) watchdogTask.cancel();

        animationTask = null;
        particleTask = null;
        watchdogTask = null;
    }

    @Nullable
    private Particle particle(@NotNull String name) {
        return particleCache.computeIfAbsent(name.toUpperCase(Locale.ROOT), key -> {
            try {
                Particle particle = Particle.valueOf(key);
                return particle.getDataType() == Void.class ? Optional.of(particle) : Optional.empty();
            } catch (IllegalArgumentException exception) {
                return Optional.empty();
            }
        }).orElse(null);
    }

    @NotNull
    private static Display.Billboard billboard(@NotNull String raw) {
        try {
            return Display.Billboard.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Display.Billboard.FIXED;
        }
    }

    @NotNull
    private static ItemDisplay.ItemDisplayTransform itemTransform(@NotNull String raw) {
        try {
            return ItemDisplay.ItemDisplayTransform.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return ItemDisplay.ItemDisplayTransform.NONE;
        }
    }

    @NotNull
    private static BlockData blockData(@NotNull Material material) {
        return material.isBlock() && !material.isAir() ? material.createBlockData() : Material.STONE.createBlockData();
    }

    private static void teleportIfMoved(@NotNull Entity entity, @NotNull Location target) {
        Location current = entity.getLocation();
        if (current.getWorld() == target.getWorld() && current.distanceSquared(target) < 1.0E-8) return;
        entity.teleport(target);
    }

    private static boolean isValid(@Nullable Entity entity) {
        return entity != null && entity.isValid();
    }

    @Nullable
    private static <T extends Entity> T remove(@Nullable T entity) {
        if (entity != null && entity.isValid()) entity.remove();
        return null;
    }

    private static final class VisualHandle {
        private volatile BlockPosition position;
        private volatile Display main;
        private volatile Interaction hitbox;
        private volatile BlockDisplay outline;
        private volatile TextDisplay hologram;

        private boolean contains(@NotNull Entity entity) {
            return entity.equals(main) || entity.equals(hitbox) || entity.equals(outline) || entity.equals(hologram);
        }

        private boolean isHealthy(@NotNull ClickableBlock block) {
            if (!block.getPosition().equals(position)) return false;

            boolean display = block.getType().isDisplay();
            if (display != isValid(main) || display != isValid(hitbox)) return false;
            if (block.getHologram().isVisible() != isValid(hologram)) return false;

            if (block.getType() == ClickableType.BLOCK && block.getDisplay().isGlow()) {
                Block physical = block.getPosition().getBlock();
                boolean wanted = physical != null && !physical.getType().isAir();

                if (wanted != isValid(outline)) return false;
                return !wanted || physical.getBlockData().equals(outline.getBlock());
            } else return !isValid(outline);
        }

        private void removeAll() {
            main = remove(main);
            hitbox = remove(hitbox);
            outline = remove(outline);
            hologram = remove(hologram);
        }
    }
}