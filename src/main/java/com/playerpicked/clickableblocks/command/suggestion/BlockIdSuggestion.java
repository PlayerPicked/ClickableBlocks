package com.playerpicked.clickableblocks.command.suggestion;

import com.playerpicked.clickableblocks.ClickableBlocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.node.ExecutionContext;

import java.util.Collection;

public final class BlockIdSuggestion implements SuggestionProvider<BukkitCommandActor> {

    @Override
    public @NotNull @Unmodifiable Collection<String> getSuggestions(@NotNull ExecutionContext<BukkitCommandActor> context) {
        return ClickableBlocks.getInstance().getBlockManager().ids();
    }
}