package com.playerpicked.clickableblocks.utils;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.identifiers.keys.MessageKeys;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import com.playerpicked.clickableblocks.model.DisplaySettings;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public final class Placeholders {

    @NotNull
    public String[] of(@NotNull ClickableBlock block) {
        DisplaySettings display = block.getDisplay();
        ClickableBlocks plugin = ClickableBlocks.getInstance();

        String cost = block.getCost() <= 0
                ? MessageKeys.PLACEHOLDER_FREE.getMessage()
                : plugin.getHookManager().getEconomy() != null ? plugin.getHookManager().getEconomy().format(block.getCost()) : FormatUtil.number(block.getCost());

        return new String[]{
                "{id}", block.getId(),
                "{type}", MessageKeys.getTypeName(block.getType()),
                "{location}", block.getPosition().format(),
                "{state}", MessageKeys.state(block.isEnabled()),
                "{actions}", String.valueOf(block.getTotalActions()),
                "{permission}", block.hasPermission() ? block.getPermission() : MessageKeys.PLACEHOLDER_NONE.getMessage(),
                "{cooldown}", FormatUtil.duration(block.getCooldown()),
                "{cost}", cost,
                "{max_uses}", block.getMaxUses() <= 0 ? MessageKeys.PLACEHOLDER_UNLIMITED.getMessage() : String.valueOf(block.getMaxUses()),
                "{used_by}", String.valueOf(plugin.getUsageManager().countPlayers(block.getId())),
                "{particle}", block.getParticle(),
                "{hologram}", MessageKeys.state(block.getHologram().isEnabled()),
                "{glow}", MessageKeys.state(display.isGlow()),
                "{item}", ItemUtil.prettyName(display.getMaterial())
        };
    }
}