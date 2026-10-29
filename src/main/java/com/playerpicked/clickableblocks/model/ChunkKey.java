package com.playerpicked.clickableblocks.model;

import org.jetbrains.annotations.NotNull;

public record ChunkKey(@NotNull String world, int x, int z) {}