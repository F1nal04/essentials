package f1nal.essentials.update;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Small SemVer comparator used without trusting provider response ordering. */
public record SemanticVersion(List<Integer> core, List<String> prerelease)
        implements Comparable<SemanticVersion> {

    public SemanticVersion {
        core = List.copyOf(core);
        prerelease = List.copyOf(prerelease);
    }

    public static Optional<SemanticVersion> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.strip();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        int buildStart = normalized.indexOf('+');
        if (buildStart >= 0) {
            normalized = normalized.substring(0, buildStart);
        }
        String[] halves = normalized.split("-", 2);
        String[] coreParts = halves[0].split("\\.", -1);
        if (coreParts.length == 0) {
            return Optional.empty();
        }
        List<Integer> core = new ArrayList<>();
        try {
            for (String part : coreParts) {
                if (part.isEmpty() || !part.chars().allMatch(Character::isDigit)) {
                    return Optional.empty();
                }
                core.add(Integer.parseInt(part));
            }
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        List<String> prerelease = List.of();
        if (halves.length == 2) {
            if (halves[1].isBlank()) {
                return Optional.empty();
            }
            prerelease = List.of(halves[1].split("\\."));
            if (prerelease.stream().anyMatch(String::isBlank)) {
                return Optional.empty();
            }
        }
        return Optional.of(new SemanticVersion(core, prerelease));
    }

    @Override
    public int compareTo(SemanticVersion other) {
        int width = Math.max(core.size(), other.core.size());
        for (int i = 0; i < width; i++) {
            int left = i < core.size() ? core.get(i) : 0;
            int right = i < other.core.size() ? other.core.get(i) : 0;
            int comparison = Integer.compare(left, right);
            if (comparison != 0) {
                return comparison;
            }
        }
        if (prerelease.isEmpty() != other.prerelease.isEmpty()) {
            return prerelease.isEmpty() ? 1 : -1;
        }
        for (int i = 0; i < Math.min(prerelease.size(), other.prerelease.size()); i++) {
            int comparison = compareIdentifier(prerelease.get(i), other.prerelease.get(i));
            if (comparison != 0) {
                return comparison;
            }
        }
        return Integer.compare(prerelease.size(), other.prerelease.size());
    }

    private static int compareIdentifier(String left, String right) {
        boolean leftNumeric = left.chars().allMatch(Character::isDigit);
        boolean rightNumeric = right.chars().allMatch(Character::isDigit);
        if (leftNumeric && rightNumeric) {
            try {
                return Long.compare(Long.parseLong(left), Long.parseLong(right));
            } catch (NumberFormatException ignored) {
                return Integer.compare(left.length(), right.length());
            }
        }
        if (leftNumeric != rightNumeric) {
            return leftNumeric ? -1 : 1;
        }
        return left.compareTo(right);
    }
}
