package f1nal.essentials.messaging;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Runtime reply and message-spy state. */
public final class MessagingState {
    /** Reserved conversation participant used for the server console. */
    public static final UUID CONSOLE_ID = new UUID(0L, 0L);

    private final Map<UUID, UUID> replyTargets = new ConcurrentHashMap<>();
    private final Set<UUID> spies = ConcurrentHashMap.newKeySet();

    public void recordConversation(UUID first, UUID second) {
        replyTargets.put(first, second);
        replyTargets.put(second, first);
    }

    public void recordConsoleMessage(UUID recipient) {
        replyTargets.put(recipient, CONSOLE_ID);
    }

    public Optional<UUID> replyTarget(UUID player) {
        return Optional.ofNullable(replyTargets.get(player));
    }

    public boolean toggleSpy(UUID player) {
        if (spies.remove(player)) return false;
        spies.add(player);
        return true;
    }

    public boolean isSpying(UUID player) {
        return spies.contains(player);
    }

    public void removeSpy(UUID player) {
        spies.remove(player);
    }

    public void clear() {
        replyTargets.clear();
        spies.clear();
    }
}
