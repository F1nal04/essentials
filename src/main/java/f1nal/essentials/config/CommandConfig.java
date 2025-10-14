package f1nal.essentials.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.fabricmc.loader.api.FabricLoader;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

public final class CommandConfig {

    public static Map<String, CommandSettings> loadCommandSettings() {
        Path cfg = FabricLoader.getInstance().getConfigDir().resolve("essentials.yaml");
        if (!Files.exists(cfg)) {
            return defaults();
        }
        try (Reader reader = Files.newBufferedReader(cfg, StandardCharsets.UTF_8)) {
            Yaml yaml = new Yaml(new LoaderOptions());
            Object root = yaml.load(reader);
            if (!(root instanceof Map<?, ?> map)) {
                return defaults();
            }
            Object commandsObj = map.get("commands");
            if (!(commandsObj instanceof Map<?, ?> commands)) {
                return defaults();
            }

            Map<String, CommandSettings> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : commands.entrySet()) {
                String commandName = entry.getKey().toString();
                Object configObj = entry.getValue();
                if (configObj instanceof Map<?, ?> config) {
                    Boolean enabled = config.get("enabled") instanceof Boolean ? (Boolean) config.get("enabled") : null;
                    String access = config.get("access") instanceof String ? (String) config.get("access") : null;

                    if (enabled != null && access != null) {
                        result.put(commandName, new CommandSettings(enabled, access));
                    }
                }
            }
            return result;
        } catch (IOException e) {
            return defaults();
        }
    }

    private static Map<String, CommandSettings> defaults() {
        Map<String, CommandSettings> defaults = new HashMap<>();
        defaults.put("repair", new CommandSettings(true, "op"));
        defaults.put("heal", new CommandSettings(true, "op"));
        defaults.put("feed", new CommandSettings(true, "op"));
        defaults.put("flight", new CommandSettings(true, "op"));
        defaults.put("disposal", new CommandSettings(true, "all"));
        defaults.put("tpa", new CommandSettings(true, "all"));
        return defaults;
    }

    public record CommandSettings(boolean enabled, String access) {

        public java.util.function.Predicate<net.minecraft.server.command.ServerCommandSource> getPermissionRequirement() {
            return switch (access.toLowerCase()) {
                case "op" -> source -> source.hasPermissionLevel(2);
                case "all" -> source -> true; // Allow everyone
                default -> source -> source.hasPermissionLevel(2); // Default to op
            };
        }
    }
}
