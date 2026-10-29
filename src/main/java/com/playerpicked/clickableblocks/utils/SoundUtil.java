package com.playerpicked.clickableblocks.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public final class SoundUtil {
    public void play(@Nullable Player player, @Nullable String sound) {
        play(player, sound, 1.0f, 1.0f);
    }

    public void play(@Nullable Player player, @Nullable String sound, float volume, float pitch) {
        if (player == null) return;
        if (sound == null || sound.isEmpty()) return;

        player.playSound(player.getLocation(), sound, volume, pitch);
    }
}