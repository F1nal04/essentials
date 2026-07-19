package f1nal.essentials.messaging;

import java.io.IOException;

import f1nal.essentials.Essentials;
import f1nal.essentials.config.ConfigPaths;

/** Server-lifecycle owner for private-message state and persistent ignores. */
public final class MessagingManager {
    private static IgnoreStore ignores;
    private static MessagingState state;

    private MessagingManager() {
    }

    public static synchronized void initialize() {
        ignores = new IgnoreStore(ConfigPaths.ignoredPlayersFile());
        state = new MessagingState();
        try {
            ignores.load();
        } catch (IOException e) {
            Essentials.LOGGER.error("Failed to load ignored players", e);
        }
    }

    public static IgnoreStore ignores() {
        if (ignores == null) throw new IllegalStateException("Messaging is not initialized");
        return ignores;
    }

    public static MessagingState state() {
        if (state == null) throw new IllegalStateException("Messaging is not initialized");
        return state;
    }

    public static synchronized void close() {
        if (state != null) state.clear();
        state = null;
        ignores = null;
    }
}
