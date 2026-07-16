package f1nal.essentials.moderation;

/** A finite moderation duration or a ban that never expires. */
public record BanDuration(boolean permanent, long durationMs) {

    public BanDuration {
        if (permanent && durationMs != 0) {
            throw new IllegalArgumentException("Permanent bans cannot have a finite duration");
        }
        if (!permanent && durationMs <= 0) {
            throw new IllegalArgumentException("Timed bans require a positive duration");
        }
    }

    public static BanDuration permanentBan() {
        return new BanDuration(true, 0);
    }

    public static BanDuration timed(long durationMs) {
        return new BanDuration(false, durationMs);
    }

    public Long expiresAt(long issuedAtMs, String action) {
        if (permanent) {
            return null;
        }
        try {
            return Math.addExact(issuedAtMs, durationMs);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(action + " expiration is too far in the future", e);
        }
    }

    public String commandDescription() {
        return permanent ? "permanently" : "for " + DurationParser.formatDuration(durationMs);
    }
}
