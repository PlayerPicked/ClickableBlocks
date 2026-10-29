package com.playerpicked.clickableblocks.guis;

import com.playerpicked.clickableblocks.ClickableBlocks;
import com.playerpicked.clickableblocks.model.ClickableBlock;
import org.jetbrains.annotations.NotNull;

public interface BlockBoundMenu {

    @NotNull
    ClickableBlock getBlock();

    default boolean isStale() {
        ClickableBlock block = getBlock();
        return ClickableBlocks.getInstance().getBlockManager().get(block.getId()).orElse(null) != block;
    }
}