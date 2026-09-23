package f1nal.essentials.home;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HomeTextTest {
    @Test
    void fillsOnlyProvidedPlaceholders() {
        assertEquals("Home cottage in 3s (2).",
                HomeText.fill("Home {name} in {seconds}s ({limit}).", "cottage", 3L, 2, null));
        assertEquals("Homes (4): ",
                HomeText.fill("Homes ({count}): ", null, null, null, 4));
    }
}
