package com.rikkamus.autowhisper;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlayerNameArgument implements ArgumentType<String> {

    private static final Collection<String> EXAMPLES = List.of("Player");

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        final String name = reader.readUnquotedString();
        if (name.isEmpty() || reader.canRead()) throw new SimpleCommandExceptionType(new LiteralMessage("Invalid target player name.")).create();

        return name;
    }

    @Override
    public <S> String parse(StringReader reader, S source) throws CommandSyntaxException {
        return parse(reader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        var playerNames = Minecraft.getInstance().player.connection.getListedOnlinePlayers().stream().map(playerInfo -> playerInfo.getProfile().getName()).sorted();
        return SharedSuggestionProvider.suggest(playerNames, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return PlayerNameArgument.EXAMPLES;
    }

}
