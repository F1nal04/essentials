package f1nal.essentials.command;

import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

/** A whitespace-delimited token that permits IPv6's colon characters. */
public final class IpBanTargetArgument implements ArgumentType<String> {

    private static final SimpleCommandExceptionType EXPECTED_TARGET =
            new SimpleCommandExceptionType(new LiteralMessage("Expected an IP address or player name"));
    private static final Collection<String> EXAMPLES =
            List.of("192.0.2.10", "2001:db8::10", "PlayerName");

    private IpBanTargetArgument() {
    }

    public static IpBanTargetArgument target() {
        return new IpBanTargetArgument();
    }

    public static String getTarget(CommandContext<?> context, String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }
        if (reader.getCursor() == start) {
            throw EXPECTED_TARGET.createWithContext(reader);
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
