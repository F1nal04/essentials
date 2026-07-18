package f1nal.essentials.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class ModrinthUpdateClientTest {

    private static final String VERSIONS = """
            [
              {"id":"wrong-game","version_number":"9.0.0","version_type":"release","status":"listed","loaders":["fabric"],"game_versions":["1.21.10"]},
              {"id":"wrong-loader","version_number":"8.0.0","version_type":"release","status":"listed","loaders":["forge"],"game_versions":["26.2"]},
              {"id":"beta","version_number":"3.4.0-beta.1","version_type":"beta","status":"listed","loaders":["fabric"],"game_versions":["26.2"]},
              {"id":"stable","version_number":"3.3.1","version_type":"release","status":"listed","loaders":["fabric"],"game_versions":["26.2"]},
              {"id":"archived","version_number":"10.0.0","version_type":"release","status":"archived","loaders":["fabric"],"game_versions":["26.2"]}
            ]
            """;

    @Test
    void stableChannelFiltersCompatibilityAndPrereleases() throws Exception {
        Optional<UpdateRelease> result = select("3.3.0", ReleaseChannel.STABLE_ONLY);
        assertEquals("3.3.1", result.orElseThrow().version());
        assertTrue(result.orElseThrow().downloadUrl().endsWith("/stable"));
    }

    @Test
    void prereleaseChannelSelectsHighestCompatibleVersion() throws Exception {
        assertEquals("3.4.0-beta.1",
                select("3.3.0", ReleaseChannel.INCLUDE_PRERELEASES).orElseThrow().version());
    }

    @Test
    void currentOrNewerInstallationProducesNoNotification() throws Exception {
        assertTrue(select("3.4.0", ReleaseChannel.INCLUDE_PRERELEASES).isEmpty());
    }

    @Test
    void malformedAndRateLimitedResponsesAreFailures() {
        SemanticVersion installed = SemanticVersion.parse("3.3.0").orElseThrow();
        assertThrows(UpdateCheckException.class, () -> ModrinthUpdateClient.handleResponse(
                200, "not-json", installed, "26.2", ReleaseChannel.STABLE_ONLY));
        assertThrows(UpdateCheckException.class, () -> ModrinthUpdateClient.handleResponse(
                429, "[]", installed, "26.2", ReleaseChannel.STABLE_ONLY));
        assertThrows(UpdateCheckException.class, () -> ModrinthUpdateClient.handleResponse(
                503, "[]", installed, "26.2", ReleaseChannel.STABLE_ONLY));
    }

    private static Optional<UpdateRelease> select(String installed, ReleaseChannel channel)
            throws UpdateCheckException {
        return ModrinthUpdateClient.selectRelease(VERSIONS,
                SemanticVersion.parse(installed).orElseThrow(), "26.2", channel);
    }
}
