package f1nal.essentials.home;

import java.util.UUID;

/** One persisted home. Ownership is the player UUID, never the display name. */
public record StoredHome(UUID owner, String name, HomePoint point) {
    public StoredHome {
        if (owner == null) {
            throw new IllegalArgumentException("owner must not be null");
        }
        if (point == null) {
            throw new IllegalArgumentException("point must not be null");
        }
        String canonical = HomeNames.canonical(name);
        if (canonical.isEmpty() || canonical.indexOf('\n') >= 0 || canonical.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("name must not be blank");
        }
        name = canonical;
    }
}
