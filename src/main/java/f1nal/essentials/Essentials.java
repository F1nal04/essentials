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

        if (commandSettings.get("repair").enabled()) {
            CommandRegistrationCallback.EVENT.register(RepairCommand::register);
        }
        if (commandSettings.get("heal").enabled()) {
            CommandRegistrationCallback.EVENT.register(HealCommand::register);
        }
        if (commandSettings.get("feed").enabled()) {
            CommandRegistrationCallback.EVENT.register(FeedCommand::register);
        }
        if (commandSettings.get("flight").enabled()) {
            CommandRegistrationCallback.EVENT.register(FlightCommand::register);
        }
        if (commandSettings.get("disposal").enabled()) {
            CommandRegistrationCallback.EVENT.register(DisposalCommand::register);
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
