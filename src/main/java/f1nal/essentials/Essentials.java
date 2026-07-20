package f1nal.essentials;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import f1nal.essentials.backpack.BackpackManager;
import f1nal.essentials.command.BackCommand;
import f1nal.essentials.command.BanCommand;
import f1nal.essentials.command.BanIpCommand;
import f1nal.essentials.command.BackpackCommand;
import f1nal.essentials.command.BackpackSeeCommand;
import f1nal.essentials.command.DisposalCommand;
import f1nal.essentials.command.EnderChestSeeCommand;
import f1nal.essentials.command.FeedCommand;
import f1nal.essentials.command.FlightCommand;
import f1nal.essentials.command.HealCommand;
import f1nal.essentials.command.HistoryCommand;
import f1nal.essentials.command.InventorySeeCommand;
import f1nal.essentials.command.KickCommand;
import f1nal.essentials.command.PardonCommand;
import f1nal.essentials.command.PardonIpCommand;
import f1nal.essentials.command.RepairCommand;
import f1nal.essentials.command.TpaCommands;
import f1nal.essentials.command.WarnCommand;
import f1nal.essentials.command.VanishCommand;
import f1nal.essentials.command.MuteCommand;
import f1nal.essentials.command.MessageCommands;
import f1nal.essentials.command.UnmuteCommand;
import f1nal.essentials.command.NoteCommand;
import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.config.CommandConfig.CommandSettings;
import f1nal.essentials.config.ConfigMigrator;
import f1nal.essentials.moderation.ModerationManager;
import f1nal.essentials.moderation.MuteEnforcement;
import f1nal.essentials.messaging.MessagingManager;
import f1nal.essentials.mixin.ServerCommonPacketListenerAccessor;
import f1nal.essentials.moderation.IpAddressUtil;
import f1nal.essentials.permission.EssentialsPermissions;
import f1nal.essentials.update.UpdateManager;
import f1nal.essentials.vanish.VanishChatEnforcement;
import f1nal.essentials.vanish.VanishManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;

public class Essentials implements ModInitializer {

    public static final String MOD_ID = "essentials";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ConfigMigrator.run();
        EssentialsPermissions.logDetectedProvider();
        registerCommands();
        registerLifecycleEvents();
        MuteEnforcement.register();
        VanishChatEnforcement.register();
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

        CommandSettings banSettings = commandSettings.get("ban");
        if (banSettings != null && banSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> BanCommand.register(dispatcher, registryAccess, environment, banSettings)
            );
        }

        CommandSettings pardonSettings = commandSettings.get("pardon");
        if (pardonSettings != null && pardonSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> PardonCommand.register(dispatcher, registryAccess, environment, pardonSettings)
            );
        }

        CommandSettings banIpSettings = commandSettings.get("banip");
        if (banIpSettings != null && banIpSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> BanIpCommand.register(dispatcher, registryAccess, environment, banIpSettings)
            );
        }

        CommandSettings pardonIpSettings = commandSettings.get("pardonip");
        if (pardonIpSettings != null && pardonIpSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> PardonIpCommand.register(
                            dispatcher, registryAccess, environment, pardonIpSettings)
            );
        }

        CommandSettings kickSettings = commandSettings.get("kick");
        if (kickSettings != null && kickSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> KickCommand.register(dispatcher, registryAccess, environment, kickSettings)
            );
        }

        CommandSettings historySettings = commandSettings.get("history");
        if (historySettings != null && historySettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> HistoryCommand.register(dispatcher, registryAccess, environment, historySettings)
            );
        }

        CommandSettings warnSettings = commandSettings.get("warn");
        if (warnSettings != null && warnSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> WarnCommand.register(dispatcher, registryAccess, environment, warnSettings));
        }
        CommandSettings muteSettings = commandSettings.get("mute");
        if (muteSettings != null && muteSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> MuteCommand.register(dispatcher, registryAccess, environment, muteSettings));
        }
        CommandSettings unmuteSettings = commandSettings.get("unmute");
        if (unmuteSettings != null && unmuteSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> UnmuteCommand.register(dispatcher, registryAccess, environment, unmuteSettings));
        }
        CommandSettings noteSettings = commandSettings.get("note");
        if (noteSettings != null && noteSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> NoteCommand.register(dispatcher, registryAccess, environment, noteSettings));
        }

        CommandSettings msgSettings = commandSettings.get("msg");
        CommandSettings replySettings = commandSettings.get("reply");
        CommandSettings ignoreSettings = commandSettings.get("ignore");
        CommandSettings msgSpySettings = commandSettings.get("msgspy");
        CommandSettings msgAllSettings = commandSettings.get("msgall");
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                -> MessageCommands.register(dispatcher, msgSettings, replySettings,
                        ignoreSettings, msgSpySettings, msgAllSettings));

        CommandSettings vanishSettings = commandSettings.get("vanish");
        VanishManager.configurePermissions(vanishSettings);
        if (vanishSettings != null && vanishSettings.enabled()) {
            CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)
                    -> VanishCommand.register(dispatcher, registryAccess, environment, vanishSettings));
        }
    }

    private void registerLifecycleEvents() {
        // Initialize backpack manager when server starts
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                ModerationManager.initialize();
            } catch (Exception e) {
                LOGGER.error("Failed to initialize moderation database", e);
                throw new IllegalStateException("Essentials moderation database could not be initialized", e);
            }
            BackpackManager.initialize(server);
            MessagingManager.initialize();
            VanishManager.initialize(server);
            UpdateManager.start(server);
        });

        // Save all backpacks when server stops
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            UpdateManager.stop();
            BackpackSeeCommand.finishAll();
            BackpackManager.saveAll(server);
            f1nal.essentials.command.OfflinePlayerDataManager.finishAll();
            MessagingManager.close();
            VanishManager.close();
            try {
                ModerationManager.close();
            } catch (java.sql.SQLException e) {
                LOGGER.error("Failed to close moderation database", e);
            }
        });

        // Authentication is complete here, but the player has not been placed
        // into the world. The lookup is cache-only because this event runs on
        // Netty's event loop.
        ServerConfigurationConnectionEvents.BEFORE_CONFIGURE.register((handler, server) -> {
            java.util.UUID playerId = handler.getOwner().id();
            if (playerId != null) {
                var accountBan = ModerationManager.activeBan(playerId);
                if (accountBan.isPresent()) {
                    handler.disconnect(f1nal.essentials.moderation.ModerationMessages.banDisconnect(
                            accountBan.get(), ModerationManager.get().nowMs()));
                    return;
                }
            }
            var connection = ((ServerCommonPacketListenerAccessor) handler)
                    .essentials$getConnection();
            IpAddressUtil.fromSocketAddress(connection.getRemoteAddress())
                    .flatMap(ModerationManager::activeIpBan)
                    .ifPresent(ban -> handler.disconnect(
                            f1nal.essentials.moderation.ModerationMessages.ipBanDisconnect(
                                    ban, ModerationManager.get().nowMs())));
        });

        // Save and drop a player's cached backpack when they disconnect.
        // Saving here matters: vanilla does not close open menus on
        // disconnect, so a backpack still open at that moment never gets
        // its menu-close save.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            java.util.UUID playerId = handler.getPlayer().getUUID();
            VanishManager.onDisconnect(handler.getPlayer());
            BackpackSeeCommand.finishForViewer(playerId);
            if (BackpackSeeCommand.isTargetBeingViewed(playerId)) {
                BackpackManager.saveBackpack(playerId,
                        BackpackManager.getOrCreateBackpack(playerId, server), server);
            } else {
                BackpackManager.saveAndUnloadPlayer(playerId, server);
            }
            f1nal.essentials.command.OfflinePlayerDataManager.finishForViewer(playerId);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            VanishManager.onJoin(handler.getPlayer());
            UpdateManager.onPlayerJoin(handler.getPlayer());
        });
    }
}
