package f1nal.essentials.update;

import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import f1nal.essentials.Essentials;
import f1nal.essentials.Messages;
import f1nal.essentials.config.UpdateConfig;
import f1nal.essentials.permission.EssentialsPermissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Owns one update-check session per server start. */
public final class UpdateManager {

    private static Session current;

    private UpdateManager() {
    }

    public static synchronized void start(MinecraftServer server) {
        stop();
        UpdateConfig config = UpdateConfig.load();
        if (!config.enabled()) {
            Essentials.LOGGER.info("Essentials update checks are disabled");
            return;
        }
        current = new Session(server, config);
        current.schedule();
    }

    public static synchronized void stop() {
        if (current != null) {
            current.close();
            current = null;
        }
    }

    public static synchronized void onPlayerJoin(ServerPlayer player) {
        if (current != null) {
            current.notifyIfEligible(player);
        }
    }

    private static synchronized boolean isCurrent(Session session) {
        return current == session;
    }

    private static final class Session {

        private final MinecraftServer server;
        private final UpdateConfig config;
        private final ScheduledExecutorService executor;
        private final AtomicBoolean failedLogged = new AtomicBoolean();
        private final Set<UUID> notifiedPlayers = new HashSet<>();
        private UpdateRelease pendingRelease;
        private String installedVersion;

        private Session(MinecraftServer server, UpdateConfig config) {
            this.server = server;
            this.config = config;
            ThreadFactory factory = task -> {
                Thread thread = new Thread(task, "Essentials update checker");
                thread.setDaemon(true);
                return thread;
            };
            this.executor = Executors.newSingleThreadScheduledExecutor(factory);
        }

        private void schedule() {
            executor.schedule(this::check, config.startupDelaySeconds(), TimeUnit.SECONDS);
        }

        private void check() {
            try {
                installedVersion = metadataVersion(Essentials.MOD_ID);
                String minecraftVersion = metadataVersion("minecraft");
                ModrinthUpdateClient client = new ModrinthUpdateClient(
                        Duration.ofSeconds(config.requestTimeoutSeconds()));
                Optional<UpdateRelease> release = client.check(
                        installedVersion, minecraftVersion, config.channel());
                release.ifPresent(update -> server.execute(() -> publish(update)));
            } catch (UpdateCheckException | RuntimeException e) {
                logFailure(e);
            }
        }

        private void publish(UpdateRelease release) {
            if (!isCurrent(this)) {
                return;
            }
            pendingRelease = release;
            if (config.consoleNotifications()) {
                Essentials.LOGGER.warn(
                        "Essentials update available: installed {}, latest {}, channel {}, download {}",
                        installedVersion, release.version(), release.versionType(), release.downloadUrl());
            }
            if (config.playerNotifications()) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    notifyIfEligible(player);
                }
            }
        }

        private void notifyIfEligible(ServerPlayer player) {
            if (pendingRelease == null || !config.playerNotifications()
                    || notifiedPlayers.contains(player.getUUID())
                    || !EssentialsPermissions.canReceiveUpdateNotification(player, config.opFallback())) {
                return;
            }
            player.sendSystemMessage(notificationMessage(pendingRelease));
            notifiedPlayers.add(player.getUUID());
        }

        private Component notificationMessage(UpdateRelease release) {
            String template = config.notificationText()
                    .replace("{installed_version}", installedVersion)
                    .replace("{latest_version}", release.version())
                    .replace("{channel}", release.versionType());
            if (!config.clickableLink() || !template.contains("{download_link}")) {
                return Messages.warning(template.replace("{download_link}", release.downloadUrl()));
            }

            String[] parts = template.split("\\{download_link}", -1);
            MutableComponent message = Messages.prefix();
            for (int i = 0; i < parts.length; i++) {
                message.append(Component.literal(parts[i]).withStyle(ChatFormatting.YELLOW));
                if (i < parts.length - 1) {
                    message.append(Component.literal(release.downloadUrl())
                            .withStyle(style -> style
                                    .withColor(ChatFormatting.AQUA)
                                    .withUnderlined(true)
                                    .withClickEvent(new ClickEvent.OpenUrl(URI.create(release.downloadUrl())))
                                    .withHoverEvent(new HoverEvent.ShowText(
                                            Component.literal("Open the Modrinth release page")))));
                }
            }
            return message;
        }

        private void logFailure(Exception error) {
            if (failedLogged.compareAndSet(false, true) && isCurrent(this)) {
                Essentials.LOGGER.warn("Essentials update check failed: {}", error.getMessage());
            }
        }

        private void close() {
            executor.shutdownNow();
            pendingRelease = null;
            notifiedPlayers.clear();
        }

        private static String metadataVersion(String modId) throws UpdateCheckException {
            return FabricLoader.getInstance().getModContainer(modId)
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElseThrow(() -> new UpdateCheckException("missing mod metadata for " + modId));
        }
    }
}
