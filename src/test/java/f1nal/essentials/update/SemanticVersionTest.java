package f1nal.essentials.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SemanticVersionTest {

    @Test
    void comparesStableAndPrereleaseVersions() {
        assertTrue(version("3.3.0").compareTo(version("3.3.0-beta.2")) > 0);
        assertTrue(version("3.3.0-beta.10").compareTo(version("3.3.0-beta.2")) > 0);
        assertTrue(version("3.4.0-alpha.1").compareTo(version("3.3.9")) > 0);
        assertEquals(0, version("v3.3").compareTo(version("3.3.0+build.4")));
    }

    private static SemanticVersion version(String value) {
        return SemanticVersion.parse(value).orElseThrow();
    }
}
