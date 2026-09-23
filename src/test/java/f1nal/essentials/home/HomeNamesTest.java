package f1nal.essentials.home;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class HomeNamesTest {
    private final Pattern pattern = HomeNames.compileOrDefault(null);

    @Test
    void defaultPatternAcceptsASingleWordAndRejectsBlankOrSpacedNames() {
        assertEquals(HomeNames.Validity.OK, HomeNames.validate("Home", pattern));
        assertEquals(HomeNames.Validity.OK, HomeNames.validate("base-camp", pattern));
        assertEquals(HomeNames.Validity.BLANK, HomeNames.validate("  ", pattern));
        assertEquals(HomeNames.Validity.INVALID, HomeNames.validate("my home", pattern));
        assertEquals(HomeNames.Validity.INVALID, HomeNames.validate("bad/name", pattern));
    }

    @Test
    void canonicalNameIsTrimmedAndLowerCase() {
        assertEquals("home", HomeNames.canonical(" Home "));
        assertEquals("", HomeNames.canonical(null));
    }

    @Test
    void brokenPatternFallsBackToTheDefault() {
        Pattern fallback = HomeNames.compileOrDefault("[");
        assertEquals(HomeNames.Validity.OK, HomeNames.validate("home", fallback));
        assertEquals(HomeNames.Validity.INVALID, HomeNames.validate("has space", fallback));
    }
}
