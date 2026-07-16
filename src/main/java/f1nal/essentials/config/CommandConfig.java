package f1nal.essentials.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import f1nal.essentials.Essentials;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class CommandConfig {

    public static Map<String, CommandSettings> loadCommandSettings() {
        Path cfg = ConfigPaths.configFile();
        if (!Files.exists(cfg)) {
            return defaults();
        }
        try {
            return parse(Files.readString(cfg, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Essentials.LOGGER.warn("Failed to read command settings from essentials.yaml, using defaults: {}", e.toString());
            return defaults();
        }
    }

    static Map<String, CommandSettings> parse(String yamlText) {
        // Start from defaults so every known command always has settings,
        // even when the user's config file is partial or malformed.
        Map<String, CommandSettings> result = defaults();
        try {
            Object root = new Yaml(new LoaderOptions()).load(yamlText);
            if (!(root instanceof Map<?, ?> map)) {
                return result;
            }
            Object commandsObj = map.get("commands");
            if (!(commandsObj instanceof Map<?, ?> commands)) {
                return result;
            }

            for (Map.Entry<?, ?> entry : commands.entrySet()) {
                String configuredName = entry.getKey().toString();
                String commandName = canonicalName(configuredName);
                if (!configuredName.equals(commandName) && commands.containsKey(commandName)) {
                    continue;
                }
                Object configObj = entry.getValue();
                if (configObj instanceof Map<?, ?> config) {
                    CommandSettings base = result.getOrDefault(commandName, new CommandSettings(true, "op"));
                    boolean enabled = config.get("enabled") instanceof Boolean b ? b : base.enabled();
                    String access = config.get("access") instanceof String s ? s : base.access();
                    result.put(commandName, new CommandSettings(enabled, access));
                }
            }
            return result;
        } catch (Exception e) {
            Essentials.LOGGER.warn("Failed to read command settings from essentials.yaml, using defaults: {}", e.toString());
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
        defaults.put("back", new CommandSettings(true, "all"));
        defaults.put("backpack", new CommandSettings(true, "all"));
        defaults.put("backpacksee", new CommandSettings(true, "op"));
        defaults.put("enderchestsee", new CommandSettings(true, "op"));
        defaults.put("inventorysee", new CommandSettings(true, "op"));
        defaults.put("ban", new CommandSettings(true, "op"));
        defaults.put("pardon", new CommandSettings(true, "op"));
        defaults.put("banip", new CommandSettings(true, "op"));
        defaults.put("pardonip", new CommandSettings(true, "op"));
        defaults.put("kick", new CommandSettings(true, "op"));
        defaults.put("history", new CommandSettings(true, "op"));
        return defaults;
    }

    private static String canonicalName(String commandName) {
        return switch (commandName) {
            case "esee" -> "enderchestsee";
            case "isee" -> "inventorysee";
            default -> commandName;
        };
    }

    public record CommandSettings(boolean enabled, String access) {

        public java.util.function.Predicate<CommandSourceStack> getPermissionRequirement() {
            return switch (access.toLowerCase()) {
                case "op" ->
                    Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);
                case "all" ->
                    source -> true; // Allow everyone
                default ->
                    Commands.hasPermission(Commands.LEVEL_GAMEMASTERS); // Default to op
            };
        }
    }
}
