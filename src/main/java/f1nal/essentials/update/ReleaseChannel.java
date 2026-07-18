package f1nal.essentials.update;

public enum ReleaseChannel {
    STABLE_ONLY("stable-only"),
    INCLUDE_PRERELEASES("including prereleases");

    private final String displayName;

    ReleaseChannel(String displayName) {
        this.displayName = displayName;
    }

    public boolean accepts(String versionType) {
        return "release".equals(versionType)
                || this == INCLUDE_PRERELEASES
                    && ("beta".equals(versionType) || "alpha".equals(versionType));
    }

    public String displayName() {
        return displayName;
    }
}
