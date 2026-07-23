package f1nal.essentials.permission;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class PermissionCatalogTest {

    @Test
    void aliasesResolveToTheirPrimaryCommandNode() {
        Map.ofEntries(
                Map.entry("trash", "disposal"),
                Map.entry("trashcan", "disposal"),
                Map.entry("bp", "backpack"),
                Map.entry("bpsee", "backpacksee"),
                Map.entry("esee", "enderchestsee"),
                Map.entry("isee", "inventorysee"),
                Map.entry("unban", "pardon"),
                Map.entry("ban-ip", "banip"),
                Map.entry("unban-ip", "pardonip"),
                Map.entry("audit", "history"),
                Map.entry("tell", "msg"),
                Map.entry("w", "msg"),
                Map.entry("r", "reply"))
                .forEach((alias, primary) -> assertEquals(primary, PermissionCatalog.path(alias)));
    }

    @Test
    void primaryAndSubPermissionPathsRemainStable() {
        assertEquals("repair", PermissionCatalog.path("repair"));
        assertEquals("repair.others", PermissionCatalog.path("repair.others"));
        assertEquals("tpahere.all", PermissionCatalog.path("tpahere.all"));
        assertEquals("tps", PermissionCatalog.path("tps"));
    }
}
