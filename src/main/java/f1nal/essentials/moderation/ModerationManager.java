package f1nal.essentials.moderation;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import f1nal.essentials.config.ConfigPaths;

/** Server lifecycle holder for the moderation service. */
public final class ModerationManager {

    private static volatile ModerationService service;

    private ModerationManager() {
    }

    public static synchronized void initialize() throws IOException, SQLException {
        if (service != null) {
            throw new IllegalStateException("Moderation service is already initialized");
        }
        service = ModerationService.open(ConfigPaths.databaseFile(), Clock.systemUTC());
    }

    public static ModerationService get() {
        ModerationService current = service;
        if (current == null) {
            throw new IllegalStateException("Moderation service is not initialized");
        }
        return current;
    }

    public static Optional<BanRecord> activeBan(UUID targetUuid) {
        ModerationService current = service;
        return current == null ? Optional.empty() : current.activeBan(targetUuid);
    }

    public static Optional<IpBanRecord> activeIpBan(String address) {
        ModerationService current = service;
        return current == null ? Optional.empty() : current.activeIpBan(address);
    }

    public static Optional<MuteRecord> activeMute(UUID targetUuid) {
        ModerationService current = service;
        return current == null ? Optional.empty() : current.activeMute(targetUuid);
    }

    public static synchronized void close() throws SQLException {
        ModerationService current = service;
        service = null;
        if (current != null) {
            current.close();
        }
    }
}
