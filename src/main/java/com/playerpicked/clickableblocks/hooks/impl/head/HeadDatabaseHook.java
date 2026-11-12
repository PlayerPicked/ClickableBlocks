package com.playerpicked.clickableblocks.hooks.impl.head;

import com.playerpicked.clickableblocks.utils.LoggerUtils;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public final class HeadDatabaseHook {
    private static final String API_CLASS = "me.arcaniax.hdb.api.HeadDatabaseAPI";

    private final Object api;
    private final Method getItemHead;

    private HeadDatabaseHook(@NotNull Object api, @NotNull Method getItemHead) {
        this.api = api;
        this.getItemHead = getItemHead;
    }

    @Nullable
    public static HeadDatabaseHook create() {
        try {
            Class<?> type = Class.forName(API_CLASS);
            Object api = type.getConstructor().newInstance();
            Method method = type.getMethod("getItemHead", String.class);
            return new HeadDatabaseHook(api, method);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LoggerUtils.warn("   [Hook] HeadDatabase API could not be loaded: {}", exception.getMessage());
            return null;
        }
    }

    @Nullable
    public ItemStack getHead(@NotNull String id) {
        try {
            Object result = getItemHead.invoke(api, id.trim());
            return result instanceof ItemStack item ? item.clone() : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}