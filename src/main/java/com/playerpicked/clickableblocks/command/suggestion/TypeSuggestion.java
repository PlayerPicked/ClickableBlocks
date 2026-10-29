package com.playerpicked.clickableblocks.command.suggestion;

import com.playerpicked.clickableblocks.identifiers.ClickableType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.node.ExecutionContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

public final class TypeSuggestion implements SuggestionProvider<BukkitCommandActor> {

    @Override
    public @NotNull @Unmodifiable Collection<String> getSuggestions(@NotNull ExecutionContext<BukkitCommandActor> context) {
        return Arrays.stream(ClickableType.values())
                .map(type -> type.name().toLowerCase(Locale.ROOT))
                .toList();
    }
}