package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig.CommandSettings;
import f1nal.essentials.config.TpsConfig;
import f1nal.essentials.tps.TpsDisplay;
import f1nal.essentials.tps.TpsManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class TpsCommand {

    private static final TpsConfig CONFIG = TpsConfig.loadOrDefaults();

    private TpsCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandSettings settings) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("tps")
                .requires(settings.getPermissionRequirement("tps"))
                .executes(context -> showTps(context.getSource()));
        dispatcher.register(root);
    }

    private static int showTps(CommandSourceStack source) {
        var snapshot = TpsManager.snapshot();
        if (snapshot.isEmpty()) {
            source.sendFailure(Messages.error("No usable tick samples are available yet."));
            return 0;
        }

        double targetTps = source.getServer().tickRateManager().tickrate();
        source.sendSuccess(() -> Messages.info(TpsDisplay.HEADER), false);

        MutableComponent values = Component.empty();
        double[] rawValues = snapshot.get().values();
        for (int i = 0; i < rawValues.length; i++) {
            if (i > 0) {
                values.append(Component.literal(", ").withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            TpsDisplay.Reading reading = TpsDisplay.reading(
                    rawValues[i], targetTps,
                    CONFIG.healthy.minimumTps(), CONFIG.degraded.minimumTps());
            values.append(Component.literal(reading.text()).withStyle(CONFIG.color(reading.health())));
        }
        source.sendSuccess(() -> Messages.custom(values), false);
        return 1;
    }
}
