package f1nal.essentials.permission;

import java.util.Map;
import java.util.function.Predicate;

import f1nal.essentials.Essentials;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;

/** Stable permission nodes and provider-aware command requirements. */
public final class EssentialsPermissions {

    private static final Map<String, String> KNOWN_PROVIDERS = Map.of(
            "luckperms", "LuckPerms");

    private EssentialsPermissions() {
    }

    public static Predicate<CommandSourceStack> require(
            String path,
            Predicate<CommandSourceStack> legacyRequirement) {
        PermissionNode<Boolean> node = node(PermissionCatalog.path(path));
        return source -> {
            boolean legacyAllowed = legacyRequirement.test(source);
            // Permission providers are player-oriented. Keeping non-player
            // sources on the legacy path preserves every command's console behavior.
            boolean playerSource = source.getPlayer() != null;
            Boolean providerResult = playerSource ? source.checkPermission(node) : null;
            return PermissionDecision.resolve(
                    providerResult, legacyAllowed, playerSource);
        };
    }

    public static PermissionNode<Boolean> node(String path) {
        if (!path.matches("[a-z0-9]+(?:\\.[a-z0-9]+)*")) {
            throw new IllegalArgumentException("Invalid Essentials permission path: " + path);
        }
        return PermissionNode.of(Essentials.MOD_ID, path);
    }

    public static void logDetectedProvider() {
        for (Map.Entry<String, String> provider : KNOWN_PROVIDERS.entrySet()) {
            if (FabricLoader.getInstance().isModLoaded(provider.getKey())) {
                Essentials.LOGGER.info(
                        "Using {} through Fabric's permission API for Essentials permission nodes",
                        provider.getValue());
                return;
            }
        }
        Essentials.LOGGER.info(
                "No supported permission provider detected; using commands.*.access (op/all)");
    }
}
