package f1nal.essentials.update;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Minimal Modrinth API client. It sends only loader and game-version filters. */
public final class ModrinthUpdateClient {

    static final String PROJECT_SLUG = "essentialsfabric";
    private static final String API_ROOT = "https://api.modrinth.com/v2/project/" + PROJECT_SLUG + "/version";
    private static final String RELEASE_PAGE = "https://modrinth.com/mod/" + PROJECT_SLUG + "/version/";

    private final HttpClient httpClient;
    private final Duration timeout;

    public ModrinthUpdateClient(Duration timeout) {
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Optional<UpdateRelease> check(
            String installedVersion,
            String minecraftVersion,
            ReleaseChannel channel) throws UpdateCheckException {
        SemanticVersion installed = SemanticVersion.parse(installedVersion)
                .orElseThrow(() -> new UpdateCheckException(
                        "installed version is not valid semantic versioning: " + installedVersion));

        String loaders = encode("[\"fabric\"]");
        String gameVersions = encode("[\"" + minecraftVersion + "\"]");
        URI uri = URI.create(API_ROOT + "?loaders=" + loaders
                + "&game_versions=" + gameVersions + "&include_changelog=false");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Accept", "application/json")
                .header("User-Agent", "F1nal04/essentials")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UpdateCheckException("request interrupted", e);
        } catch (IOException | RuntimeException e) {
            throw new UpdateCheckException("provider unavailable: " + e.getMessage(), e);
        }
        return handleResponse(response.statusCode(), response.body(), installed,
                minecraftVersion, channel);
    }

    static Optional<UpdateRelease> handleResponse(
            int statusCode,
            String responseBody,
            SemanticVersion installed,
            String minecraftVersion,
            ReleaseChannel channel) throws UpdateCheckException {
        if (statusCode == 429) {
            throw new UpdateCheckException("provider rate limit reached");
        }
        if (statusCode < 200 || statusCode >= 300) {
            throw new UpdateCheckException("provider returned HTTP " + statusCode);
        }
        return selectRelease(responseBody, installed, minecraftVersion, channel);
    }

    static Optional<UpdateRelease> selectRelease(
            String responseBody,
            SemanticVersion installed,
            String minecraftVersion,
            ReleaseChannel channel) throws UpdateCheckException {
        JsonArray versions;
        try {
            JsonElement root = JsonParser.parseString(responseBody);
            if (!root.isJsonArray()) {
                throw new UpdateCheckException("provider response was not a version list");
            }
            versions = root.getAsJsonArray();
        } catch (UpdateCheckException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new UpdateCheckException("provider returned malformed JSON", e);
        }

        try {
            return versions.asList().stream()
                    .filter(JsonElement::isJsonObject)
                    .map(JsonElement::getAsJsonObject)
                    .filter(version -> compatible(version, minecraftVersion, channel))
                    .map(ModrinthUpdateClient::candidate)
                    .flatMap(Optional::stream)
                    .filter(candidate -> candidate.semanticVersion().compareTo(installed) > 0)
                    .max(Comparator.comparing(Candidate::semanticVersion))
                    .map(Candidate::release);
        } catch (RuntimeException e) {
            throw new UpdateCheckException("provider returned an invalid version response", e);
        }
    }

    private static boolean compatible(
            JsonObject version,
            String minecraftVersion,
            ReleaseChannel channel) {
        return hasString(version, "status", "listed")
                && channel.accepts(requiredString(version, "version_type"))
                && contains(version, "loaders", "fabric")
                && contains(version, "game_versions", minecraftVersion);
    }

    private static Optional<Candidate> candidate(JsonObject version) {
        String versionNumber = requiredString(version, "version_number");
        String versionType = requiredString(version, "version_type");
        String id = requiredString(version, "id");
        return SemanticVersion.parse(versionNumber)
                .map(semantic -> new Candidate(semantic,
                        new UpdateRelease(versionNumber, versionType, RELEASE_PAGE + id)));
    }

    private static boolean hasString(JsonObject object, String key, String expected) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                && expected.equals(object.get(key).getAsString());
    }

    private static String requiredString(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            throw new IllegalArgumentException("missing string field " + key);
        }
        return object.get(key).getAsString();
    }

    private static boolean contains(JsonObject object, String key, String expected) {
        if (!object.has(key) || !object.get(key).isJsonArray()) {
            return false;
        }
        for (JsonElement element : object.getAsJsonArray(key)) {
            if (element.isJsonPrimitive() && expected.equals(element.getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record Candidate(SemanticVersion semanticVersion, UpdateRelease release) {
    }
}
