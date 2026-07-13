package f1nal.essentials;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import f1nal.essentials.backpack.BackpackManager;
import f1nal.essentials.command.BackCommand;
import f1nal.essentials.command.BackpackCommand;
import f1nal.essentials.command.BackpackSeeCommand;
import f1nal.essentials.command.DisposalCommand;
import f1nal.essentials.command.EnderChestSeeCommand;
import f1nal.essentials.command.FeedCommand;
import f1nal.essentials.command.FlightCommand;
import f1nal.essentials.command.HealCommand;
import f1nal.essentials.command.InventorySeeCommand;
import f1nal.essentials.command.RepairCommand;
import f1nal.essentials.command.TpaCommands;
import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.config.CommandConfig.CommandSettings;
import f1nal.essentials.config.ConfigMigrator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class Essentials implements ModInitializer {

    public static final String MOD_ID = "essentials";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ConfigMigrator.run();
        registerCommands();
        registerLifecycleEvents();
        LOGGER.info("Essentials initialized");
    }

    private void registerCommands() {
        Map<String, CommandSettings> commandSettings = CommandConfig.loadCommandSettings();

        CommandSettings repairSettings = commandSettings.get("repair");
        if (repairSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> RepairCommand.register(dispatcher, registryAccess, environment, repairSettings)
            );
        }

        CommandSettings healSettings = commandSettings.get("heal");
        if (healSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> HealCommand.register(dispatcher, registryAccess, environment, healSettings)
            );
        }

        CommandSettings feedSettings = commandSettings.get("feed");
        if (feedSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> FeedCommand.register(dispatcher, registryAccess, environment, feedSettings)
            );
        }

        CommandSettings flightSettings = commandSettings.get("flight");
        if (flightSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> FlightCommand.register(dispatcher, registryAccess, environment, flightSettings)
            );
        }

        CommandSettings disposalSettings = commandSettings.get("disposal");
        if (disposalSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> DisposalCommand.register(dispatcher, registryAccess, environment, disposalSettings)
            );
        }

        CommandSettings tpaSettings = commandSettings.get("tpa");
        if (tpaSettings != null && tpaSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> TpaCommands.register(dispatcher, registryAccess, environment, tpaSettings)
            );
        }

        CommandSettings backSettings = commandSettings.get("back");
        if (backSettings != null && backSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> BackCommand.register(dispatcher, registryAccess, environment, backSettings)
            );
        }

        CommandSettings backpackSettings = commandSettings.get("backpack");
        if (backpackSettings != null && backpackSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> BackpackCommand.register(dispatcher, registryAccess, environment, backpackSettings)
            );
        }

        CommandSettings backpackSeeSettings = commandSettings.get("backpacksee");
        if (backpackSeeSettings != null && backpackSeeSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> BackpackSeeCommand.register(dispatcher, registryAccess, environment, backpackSeeSettings)
            );
        }

        CommandSettings enderChestSeeSettings = commandSettings.get("enderchestsee");
        if (enderChestSeeSettings != null && enderChestSeeSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> EnderChestSeeCommand.register(dispatcher, registryAccess, environment, enderChestSeeSettings)
            );
        }

        CommandSettings inventorySeeSettings = commandSettings.get("inventorysee");
        if (inventorySeeSettings != null && inventorySeeSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> InventorySeeCommand.register(dispatcher, registryAccess, environment, inventorySeeSettings)
            );
        }
    }

    private void registerLifecycleEvents() {
        // Initialize backpack manager when server starts
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            BackpackManager.initialize(server);
        });

        // Save all backpacks when server stops
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            BackpackSeeCommand.finishAll();
            BackpackManager.saveAll(server);
            f1nal.essentials.command.OfflinePlayerDataManager.finishAll();
        });

        // Save and drop a player's cached backpack when they disconnect.
        // Saving here matters: vanilla does not close open menus on
        // disconnect, so a backpack still open at that moment never gets
        // its menu-close save.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            java.util.UUID playerId = handler.getPlayer().getUUID();
            BackpackSeeCommand.finishForViewer(playerId);
            if (BackpackSeeCommand.isTargetBeingViewed(playerId)) {
                BackpackManager.saveBackpack(playerId,
                        BackpackManager.getOrCreateBackpack(playerId, server), server);
            } else {
                BackpackManager.saveAndUnloadPlayer(playerId, server);
            }
            f1nal.essentials.command.OfflinePlayerDataManager.finishForViewer(playerId);
        });
    }
}
