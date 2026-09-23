package f1nal.essentials.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import f1nal.essentials.config.CommandConfig.CommandSettings;
import f1nal.essentials.home.HomeNames;

class HomeConfigTest {
    @Test
    void parsesLimitsTimingNameRulesAndMessages() {
        HomeConfig config = HomeConfig.parse("""
                homes:
                  default_limit: "2"
                  maximum_limit: 8
                  allow_cross_dimension: false
                  warmup_seconds: 0
                  cooldown_seconds: 15
                  cancel_on_movement: false
                  cancel_on_damage: false
                  default_name: "Base"
                  name_pattern: "^[A-Za-z]{1,8}$"
                  set_message: "&bSaved {name}."
                """);

        assertEquals(2, config.defaultLimit);
        assertEquals(8, config.maximumLimit);
        assertFalse(config.allowCrossDimension);
        assertEquals(0, config.warmupSeconds);
        assertEquals(15, config.cooldownSeconds);
        assertFalse(config.cancelOnMovement);
        assertFalse(config.cancelOnDamage);
        assertEquals("Base", config.defaultName);
        assertEquals("^[A-Za-z]{1,8}$", config.namePattern.pattern());
        assertEquals("&bSaved {name}.", config.setMessage);
    }

    @Test
    void invalidValuesFallBackPerField() {
        HomeConfig config = HomeConfig.parse("""
                homes:
                  default_limit: 0
                  maximum_limit: 9999
                  warmup_seconds: -1
                  name_pattern: "["
                  default_name: "bad name"
                  duplicate_message: ""
                """);

        assertEquals(5, config.maximumLimit);
        assertEquals(1, config.defaultLimit);
        assertEquals(3, config.warmupSeconds);
        assertEquals(HomeNames.DEFAULT_PATTERN, config.namePattern.pattern());
        assertEquals("home", config.defaultName);
        assertEquals("&cYou already have a home named &d{name}&c.", config.duplicateMessage);
    }

    @Test
    void defaultLimitCannotExceedMaximum() {
        HomeConfig config = HomeConfig.parse("""
                homes:
                  default_limit: 9
                  maximum_limit: 3
                """);

        assertEquals(3, config.maximumLimit);
        assertEquals(1, config.defaultLimit);
    }

    @Test
    void bundledDefaultYamlMatchesDocumentedHomes() throws Exception {
        String yaml;
        try (var input = HomeConfig.class.getClassLoader()
                .getResourceAsStream("essentials.default.yaml")) {
            yaml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        HomeConfig config = HomeConfig.parse(yaml);
        assertEquals(1, config.defaultLimit);
        assertEquals(5, config.maximumLimit);
        assertTrue(config.allowCrossDimension);
        assertEquals(3, config.warmupSeconds);
        assertEquals(30, config.cooldownSeconds);
        assertEquals("home", config.defaultName);
        assertEquals(HomeNames.DEFAULT_PATTERN, config.namePattern.pattern());
        assertEquals(HomeNames.Validity.OK,
                HomeNames.validate(config.defaultName, config.namePattern));
        assertEquals(new CommandSettings(true, "all"), CommandConfig.parse(yaml).get("home"));
        assertEquals(new CommandSettings(true, "all"), CommandConfig.parse(yaml).get("sethome"));
        assertEquals(new CommandSettings(true, "all"), CommandConfig.parse(yaml).get("delhome"));
        assertEquals(new CommandSettings(true, "all"), CommandConfig.parse(yaml).get("homes"));
    }

    @Test
    void missingSectionAndMalformedYamlUseDefaults() {
        HomeConfig missing = HomeConfig.parse("other: {}\n");
        assertEquals(1, missing.defaultLimit);
        assertEquals(5, missing.maximumLimit);
        assertTrue(missing.allowCrossDimension);
        assertTrue(missing.cancelOnMovement);
        assertEquals(30, HomeConfig.parse("{{{ not yaml").cooldownSeconds);
    }
}
