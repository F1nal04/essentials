package f1nal.essentials.permission;

/** Canonical permission paths for command literals and aliases. */
public final class PermissionCatalog {

    private PermissionCatalog() {
    }

    public static String path(String commandOrPath) {
        return switch (commandOrPath) {
            case "trash", "trashcan" -> "disposal";
            case "bp" -> "backpack";
            case "bpsee" -> "backpacksee";
            case "esee" -> "enderchestsee";
            case "isee" -> "inventorysee";
            case "unban" -> "pardon";
            case "ban-ip", "banip" -> "banip";
            case "pardon-ip", "unban-ip" -> "pardonip";
            case "audit" -> "history";
            default -> commandOrPath;
        };
    }
}
