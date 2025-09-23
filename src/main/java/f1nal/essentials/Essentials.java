package f1nal.essentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import f1nal.essentials.command.DisposalCommand;
import f1nal.essentials.command.FeedCommand;
import f1nal.essentials.command.FlightCommand;
import f1nal.essentials.command.HealCommand;
import f1nal.essentials.command.RepairCommand;

public class Essentials implements ModInitializer {

    public static final String MOD_ID = "essentials";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        registerCommands();
        LOGGER.info("Essentials initialized");
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register(RepairCommand::register);
        CommandRegistrationCallback.EVENT.register(HealCommand::register);
        CommandRegistrationCallback.EVENT.register(FeedCommand::register);
        CommandRegistrationCallback.EVENT.register(FlightCommand::register);
        CommandRegistrationCallback.EVENT.register(DisposalCommand::register);
    }
}
