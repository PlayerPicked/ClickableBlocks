package com.playerpicked.clickableblocks.model;

import org.jetbrains.annotations.NotNull;

public record StoredBlock(@NotNull String id, @NotNull String world, int x, int y, int z, @NotNull String type, @NotNull String data, long createdAt) {}