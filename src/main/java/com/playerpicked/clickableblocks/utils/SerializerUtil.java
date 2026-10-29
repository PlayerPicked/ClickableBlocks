package com.playerpicked.clickableblocks.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.playerpicked.clickableblocks.identifiers.ClickTrigger;
import com.playerpicked.clickableblocks.identifiers.ClickableType;
import com.playerpicked.clickableblocks.model.BlockPosition;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import com.playerpicked.clickableblocks.model.DisplaySettings;
import com.playerpicked.clickableblocks.model.HologramSettings;
import com.playerpicked.clickableblocks.model.StoredBlock;
import lombok.experimental.UtilityClass;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public final class SerializerUtil {

    @NotNull
    public StoredBlock toStored(@NotNull ClickableBlock block) {
        BlockPosition position = block.getPosition();

        return new StoredBlock(
                block.getId(),
                position.world(),
                position.x(),
                position.y(),
                position.z(),
                block.getType().name(),
                toJson(block).toString(),
                block.getCreatedAt());
    }

    @Nullable
    public ClickableBlock fromStored(@NotNull StoredBlock stored) {
        ClickableType type = ClickableType.fromString(stored.type());
        if (type == null) {
            LoggerUtils.warn("Unknown type '{}' for clickable block '{}', skipping it.", stored.type(), stored.id());
            return null;
        }

        ClickableBlock block = new ClickableBlock(stored.id(), type, new BlockPosition(stored.world(), stored.x(), stored.y(), stored.z()), stored.createdAt());

        try {
            JsonElement element = JsonParser.parseString(stored.data());
            if (element.isJsonObject()) read(block, element.getAsJsonObject());
        } catch (Exception exception) {
            LoggerUtils.warn("Settings of clickable block '{}' are damaged, loading it with defaults: {}", stored.id(), exception.getMessage());
        }

        return block;
    }

    @NotNull
    private JsonObject toJson(@NotNull ClickableBlock block) {
        JsonObject root = new JsonObject();
        root.addProperty("enabled", block.isEnabled());
        root.addProperty("permission", block.getPermission());
        root.addProperty("cooldown", block.getCooldown());
        root.addProperty("cost", block.getCost());
        root.addProperty("max-uses", block.getMaxUses());
        root.addProperty("cancel-interaction", block.isCancelInteraction());
        root.addProperty("particle", block.getParticle());

        JsonObject actions = new JsonObject();
        for (ClickTrigger trigger : ClickTrigger.values()) {
            List<String> list = block.getActions(trigger);
            if (list.isEmpty()) continue;

            JsonArray array = new JsonArray();
            list.forEach(array::add);
            actions.add(trigger.name(), array);
        }
        root.add("actions", actions);

        DisplaySettings display = block.getDisplay();
        JsonObject displayJson = new JsonObject();
        displayJson.addProperty("item", ItemUtil.serialize(display.getItem()));
        displayJson.addProperty("scale-x", display.getScaleX());
        displayJson.addProperty("scale-y", display.getScaleY());
        displayJson.addProperty("scale-z", display.getScaleZ());
        displayJson.addProperty("yaw", display.getYaw());
        displayJson.addProperty("pitch", display.getPitch());
        displayJson.addProperty("roll", display.getRoll());
        displayJson.addProperty("offset-x", display.getOffsetX());
        displayJson.addProperty("offset-y", display.getOffsetY());
        displayJson.addProperty("offset-z", display.getOffsetZ());
        displayJson.addProperty("glow", display.isGlow());
        displayJson.addProperty("glow-color", display.getGlowColor());
        displayJson.addProperty("billboard", display.getBillboard());
        displayJson.addProperty("transform", display.getTransform());
        displayJson.addProperty("brightness", display.getBrightness());
        displayJson.addProperty("view-range", display.getViewRange());
        displayJson.addProperty("spin", display.isSpin());
        displayJson.addProperty("spin-speed", display.getSpinSpeed());
        displayJson.addProperty("bob", display.isBob());
        displayJson.addProperty("bob-height", display.getBobHeight());
        displayJson.addProperty("hitbox-width", display.getHitboxWidth());
        displayJson.addProperty("hitbox-height", display.getHitboxHeight());
        root.add("display", displayJson);

        HologramSettings hologram = block.getHologram();
        JsonObject hologramJson = new JsonObject();
        hologramJson.addProperty("enabled", hologram.isEnabled());
        JsonArray lines = new JsonArray();
        hologram.getLines().forEach(lines::add);
        hologramJson.add("lines", lines);
        hologramJson.addProperty("height", hologram.getHeight());
        hologramJson.addProperty("background", hologram.isBackground());
        hologramJson.addProperty("shadow", hologram.isShadow());
        hologramJson.addProperty("billboard", hologram.getBillboard());
        hologramJson.addProperty("view-range", hologram.getViewRange());
        root.add("hologram", hologramJson);

        return root;
    }

    private void read(@NotNull ClickableBlock block, @NotNull JsonObject root) {
        block.setEnabled(bool(root, "enabled", true));
        block.setPermission(string(root, "permission", ""));
        block.setCooldown(Math.max(0, number(root, "cooldown", 0).longValue()));
        block.setCost(Math.max(0, number(root, "cost", 0).doubleValue()));
        block.setMaxUses(Math.max(0, number(root, "max-uses", 0).intValue()));
        block.setCancelInteraction(bool(root, "cancel-interaction", true));
        block.setParticle(string(root, "particle", "NONE"));

        if (root.has("actions") && root.get("actions").isJsonObject()) {
            JsonObject actions = root.getAsJsonObject("actions");

            for (ClickTrigger trigger : ClickTrigger.values()) {
                if (!actions.has(trigger.name()) || !actions.get(trigger.name()).isJsonArray()) continue;
                block.getActions(trigger).addAll(strings(actions.getAsJsonArray(trigger.name())));
            }
        }

        if (root.has("display") && root.get("display").isJsonObject()) {
            JsonObject json = root.getAsJsonObject("display");
            DisplaySettings display = block.getDisplay();

            ItemStack item = ItemUtil.deserialize(string(json, "item", ""));
            if (item != null && !ItemUtil.isEmpty(item)) display.setItem(item);

            display.setScale(number(json, "scale-x", 1).floatValue(), number(json, "scale-y", 1).floatValue(), number(json, "scale-z", 1).floatValue());
            display.setRotation(number(json, "yaw", 0).floatValue(), number(json, "pitch", 0).floatValue(), number(json, "roll", 0).floatValue());
            display.setOffset(number(json, "offset-x", 0).doubleValue(), number(json, "offset-y", 0).doubleValue(), number(json, "offset-z", 0).doubleValue());
            display.setGlow(bool(json, "glow", false));
            display.setGlowColor(number(json, "glow-color", 0xFFFFFF).intValue());
            display.setBillboard(string(json, "billboard", "FIXED"));
            display.setTransform(string(json, "transform", "NONE"));
            display.setBrightness(Math.clamp(number(json, "brightness", -1).intValue(), -1, 15));
            display.setViewRange(Math.clamp(number(json, "view-range", 1).floatValue(), 0.1f, 10f));
            display.setSpin(bool(json, "spin", false));
            display.setSpinSpeed(number(json, "spin-speed", 45).floatValue());
            display.setBob(bool(json, "bob", false));
            display.setBobHeight(number(json, "bob-height", 0.15).floatValue());
            display.setHitbox(number(json, "hitbox-width", -1).floatValue(), number(json, "hitbox-height", -1).floatValue());
        }

        if (root.has("hologram") && root.get("hologram").isJsonObject()) {
            JsonObject json = root.getAsJsonObject("hologram");
            HologramSettings hologram = block.getHologram();

            hologram.setEnabled(bool(json, "enabled", false));
            if (json.has("lines") && json.get("lines").isJsonArray()) hologram.setLines(strings(json.getAsJsonArray("lines")));
            hologram.setHeight(number(json, "height", 0.25).doubleValue());
            hologram.setBackground(bool(json, "background", false));
            hologram.setShadow(bool(json, "shadow", true));
            hologram.setBillboard(string(json, "billboard", "CENTER"));
            hologram.setViewRange(number(json, "view-range", 1).floatValue());
        }
    }

    @NotNull
    private List<String> strings(@NotNull JsonArray array) {
        List<String> list = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            if (element.isJsonPrimitive()) list.add(element.getAsString());
        }
        return list;
    }

    private boolean bool(@NotNull JsonObject json, @NotNull String key, boolean def) {
        try {
            return json.has(key) ? json.get(key).getAsBoolean() : def;
        } catch (Exception exception) {
            return def;
        }
    }

    @NotNull
    private String string(@NotNull JsonObject json, @NotNull String key, @NotNull String def) {
        try {
            return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : def;
        } catch (Exception exception) {
            return def;
        }
    }

    @NotNull
    private Number number(@NotNull JsonObject json, @NotNull String key, @NotNull Number def) {
        try {
            if (!json.has(key)) return def;

            double value = json.get(key).getAsDouble();
            return Double.isFinite(value) ? value : def;
        } catch (Exception exception) {
            return def;
        }
    }
}