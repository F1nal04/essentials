package f1nal.essentials.moderation;

import java.util.Objects;
import java.util.UUID;

public record Moderator(UUID uuid, String name) {

    public Moderator {
        Objects.requireNonNull(name, "name");
    }
}
