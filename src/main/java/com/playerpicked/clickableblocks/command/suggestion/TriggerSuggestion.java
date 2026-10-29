package com.playerpicked.clickableblocks.command.suggestion;

import com.playerpicked.clickableblocks.identifiers.ClickTrigger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.node.ExecutionContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

public final class TriggerSuggestion implements SuggestionProvider<BukkitCommandActor> {

    @Override
    public @NotNull @Unmodifiable Collection<String> getSuggestions(@NotNull ExecutionContext<BukkitCommandActor> context) {
        return Arrays.stream(ClickTrigger.values())
                .map(trigger -> trigger.name().toLowerCase(Locale.ROOT))
                .toList();
    }
}