package f1nal.essentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;

import f1nal.essentials.command.DisposalCommand;
import f1nal.essentials.command.FeedCommand;
import f1nal.essentials.command.FlightCommand;
import f1nal.essentials.command.HealCommand;
import f1nal.essentials.command.RepairCommand;
import f1nal.essentials.command.TpaCommands;
import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.config.CommandConfig.CommandSettings;
import java.util.Map;

public class Essentials implements ModInitializer {

    public static final String MOD_ID = "essentials";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        copyDefaultConfigIfMissing();
        registerCommands();
        LOGGER.info("Essentials initialized");
    }

    private void registerCommands() {
        Map<String, CommandSettings> commandSettings = CommandConfig.loadCommandSettings();

        CommandSettings repairSettings = commandSettings.get("repair");
        if (repairSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                RepairCommand.register(dispatcher, registryAccess, environment, repairSettings)
            );
        }

        CommandSettings healSettings = commandSettings.get("heal");
        if (healSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                HealCommand.register(dispatcher, registryAccess, environment, healSettings)
            );
        }

        CommandSettings feedSettings = commandSettings.get("feed");
        if (feedSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                FeedCommand.register(dispatcher, registryAccess, environment, feedSettings)
            );
        }

        CommandSettings flightSettings = commandSettings.get("flight");
        if (flightSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                FlightCommand.register(dispatcher, registryAccess, environment, flightSettings)
            );
        }

        CommandSettings disposalSettings = commandSettings.get("disposal");
        if (disposalSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                DisposalCommand.register(dispatcher, registryAccess, environment, disposalSettings)
            );
        }

        CommandSettings tpaSettings = commandSettings.get("tpa");
        if (tpaSettings != null && tpaSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                TpaCommands.register(dispatcher, registryAccess, environment, tpaSettings)
            );
        }
    }

    private void copyDefaultConfigIfMissing() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path target = configDir.resolve("essentials.yaml");
        if (Files.exists(target)) {
            return;
        }
        try {
            Files.createDirectories(configDir);
            try (InputStream in = Essentials.class.getClassLoader().getResourceAsStream("essentials.default.yaml")) {
                if (in == null) {
                    LOGGER.warn("Missing bundled essentials.default.yaml resource; skipping config copy.");
                    return;
                }
                Files.copy(in, target);
                LOGGER.info("Wrote default config to {}", target.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to write default config: {}", e.toString());
        }
    }
}
