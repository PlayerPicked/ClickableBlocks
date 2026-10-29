package com.playerpicked.clickableblocks.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public final class DisplaySettings {
    public static final float MIN_SCALE = 0.05f;
    public static final float MAX_SCALE = 16f;
    public static final double MAX_OFFSET = 8;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private volatile ItemStack item = new ItemStack(Material.DIAMOND_BLOCK);

    @Setter(AccessLevel.NONE) private volatile float scaleX = 1f;
    @Setter(AccessLevel.NONE) private volatile float scaleY = 1f;
    @Setter(AccessLevel.NONE) private volatile float scaleZ = 1f;

    @Setter(AccessLevel.NONE) private volatile float yaw;
    @Setter(AccessLevel.NONE) private volatile float pitch;
    @Setter(AccessLevel.NONE) private volatile float roll;

    @Setter(AccessLevel.NONE) private volatile double offsetX;
    @Setter(AccessLevel.NONE) private volatile double offsetY;
    @Setter(AccessLevel.NONE) private volatile double offsetZ;

    private volatile boolean glow;
    private volatile int glowColor = 0xFFFFFF;

    private volatile String billboard = "FIXED";
    private volatile String transform = "NONE";
    private volatile int brightness = -1;
    private volatile float viewRange = 1f;

    private volatile boolean spin;
    private volatile float spinSpeed = 45f;
    private volatile boolean bob;
    private volatile float bobHeight = 0.15f;

    private volatile float hitboxWidth = -1f;
    private volatile float hitboxHeight = -1f;

    @NotNull
    public ItemStack getItem() {
        return item.clone();
    }

    public void setItem(@NotNull ItemStack item) {
        ItemStack copy = item.clone();
        copy.setAmount(1);
        this.item = copy;
    }

    @NotNull
    public Material getMaterial() {
        return item.getType();
    }

    public void setScale(float x, float y, float z) {
        scaleX = clampScale(x);
        scaleY = clampScale(y);
        scaleZ = clampScale(z);
    }

    public void scaleUniform(float delta) {
        setScale(scaleX + delta, scaleY + delta, scaleZ + delta);
    }

    public void setOffset(double x, double y, double z) {
        offsetX = clampOffset(x);
        offsetY = clampOffset(y);
        offsetZ = clampOffset(z);
    }

    public void setRotation(float yaw, float pitch, float roll) {
        this.yaw = normalize(yaw);
        this.pitch = normalize(pitch);
        this.roll = normalize(roll);
    }

    public boolean hasAutoHitbox() {
        return hitboxWidth <= 0 || hitboxHeight <= 0;
    }

    public float resolveHitboxWidth() {
        return hasAutoHitbox() ? Math.clamp(Math.max(scaleX, scaleZ), 0.1f, MAX_SCALE) : hitboxWidth;
    }

    public float resolveHitboxHeight() {
        return hasAutoHitbox() ? Math.clamp(scaleY, 0.1f, MAX_SCALE) : hitboxHeight;
    }

    public void setHitbox(float width, float height) {
        if (width <= 0 || height <= 0) {
            hitboxWidth = -1f;
            hitboxHeight = -1f;
            return;
        }

        hitboxWidth = Math.clamp(width, 0.1f, MAX_SCALE);
        hitboxHeight = Math.clamp(height, 0.1f, MAX_SCALE);
    }

    public void resetTransform() {
        setScale(1f, 1f, 1f);
        setRotation(0f, 0f, 0f);
        setOffset(0, 0, 0);
        setHitbox(-1f, -1f);
    }

    public void copyFrom(@NotNull DisplaySettings other) {
        setItem(other.getItem());
        setScale(other.scaleX, other.scaleY, other.scaleZ);
        setRotation(other.yaw, other.pitch, other.roll);
        setOffset(other.offsetX, other.offsetY, other.offsetZ);
        glow = other.glow;
        glowColor = other.glowColor;
        billboard = other.billboard;
        transform = other.transform;
        brightness = other.brightness;
        viewRange = other.viewRange;
        spin = other.spin;
        spinSpeed = other.spinSpeed;
        bob = other.bob;
        bobHeight = other.bobHeight;
        hitboxWidth = other.hitboxWidth;
        hitboxHeight = other.hitboxHeight;
    }

    public static float clampScale(float value) {
        return Math.clamp(Math.round(value * 1000f) / 1000f, MIN_SCALE, MAX_SCALE);
    }

    public static double clampOffset(double value) {
        return Math.clamp(Math.round(value * 10000d) / 10000d, -MAX_OFFSET, MAX_OFFSET);
    }

    public static float normalize(float degrees) {
        float value = degrees % 360f;
        if (value > 180f) value -= 360f;
        if (value <= -180f) value += 360f;
        return Math.round(value * 100f) / 100f;
    }
}