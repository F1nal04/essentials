package f1nal.essentials.command;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import f1nal.essentials.home.HomeManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/** Suggests the executing player's saved home names. */
public final class HomeNameSuggestions {
    private HomeNameSuggestions() {
    }

    public static SuggestionProvider<CommandSourceStack> names() {
        return (context, builder) -> suggest(context.getSource().getPlayer(), builder);
    }

    private static CompletableFuture<Suggestions> suggest(
            ServerPlayer player, SuggestionsBuilder builder) {
        if (player == null) return builder.buildFuture();
        String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String name : HomeManager.names(player.getUUID())) {
            if (name.startsWith(prefix)) builder.suggest(name);
        }
        return builder.buildFuture();
    }
}
