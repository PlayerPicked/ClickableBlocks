package com.playerpicked.clickableblocks.guis.service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuService {

    private static final long DEFAULT_COOLDOWN_MS = 75;

    private static final ConcurrentHashMap<UUID, Long> lastOpen = new ConcurrentHashMap<>();

    private MenuService() {}

    public static boolean canOpen(UUID uuid) {
        long now = System.currentTimeMillis();
        boolean[] allowed = {false};

        lastOpen.compute(uuid, (key, last) -> {
            if (last != null && now - last < DEFAULT_COOLDOWN_MS) return last;
            allowed[0] = true;
            return now;
        });

        return allowed[0];
    }

    public static void clear(UUID uuid) {
        lastOpen.remove(uuid);
    }
}