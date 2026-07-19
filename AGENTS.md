## Project

Essentials is a server-side Fabric mod (Minecraft 26.2, Java 25) providing admin and QoL commands for SMP servers: teleport requests, backpacks, moderation (bans/mutes/warnings backed by SQLite), private messaging, and Modrinth update checks. Vanilla clients can connect; the mod also works in singleplayer via the integrated server.

## Commands

```bash
./gradlew build        # full build; remapped JAR lands in build/libs/
./gradlew test         # unit tests (JUnit 5, pure JVM — no Minecraft bootstrap)
./gradlew test --tests "f1nal.essentials.config.ConfigMergerTest"   # single test class
./gradlew runServer    # Loom dev server (working dir: run/)
./gradlew runClient    # Loom dev client
```

Java 25 is required (`options.release = 25`).

## Versioning and commits

- Releases are automated by release-please. Use conventional commit messages (`feat:`, `fix:`, `chore:`, ...) — they determine the next version.
- `mod_version` in `gradle.properties` is managed by release-please via the `x-release-please-*` markers. Never bump it manually.
- Do not add `Co-Authored-By` trailers to commits.
- Spec/plan documents may be written to disk while working, but must never be committed.

## Architecture

Entry point is `f1nal.essentials.Essentials` (ModInitializer). `onInitialize()` runs the config migrator, registers commands (each gated by its `CommandConfig` settings), and hooks server lifecycle events: `SERVER_STARTED` initializes `ModerationManager`, `BackpackManager`, `MessagingManager`, and `UpdateManager`; `SERVER_STOPPING` tears them down; `BEFORE_CONFIGURE` enforces account and IP bans at connection time (runs on Netty's event loop, so lookups there must be cache-only); `DISCONNECT` saves and unloads backpacks.

### Config system

- `src/main/resources/essentials.default.yaml` is the schema source of truth, deployed to `config/essentials/essentials.yaml` on first run.
- On mod updates, `ConfigMigrator` (file I/O + logging) uses `ConfigMerger` (pure merge logic, unit-tested) to merge new keys into the user's file, backing the old one up to `essentials.yaml.bak`.
- Adding a config option means editing the default YAML **and** the matching `*Config` class in `config/`. Config classes parse leniently and fall back to defaults on any error — a broken user config must never crash startup.

### Commands and permissions

- Each command is a final class in `command/` with a static `register(dispatcher, registryAccess, environment, settings)` method; registration in `Essentials.registerCommands()` is skipped when the command is disabled in config.
- Every command has two access paths combined by `EssentialsPermissions.require()`: a Fabric permission API node (`essentials.<path>`, used by providers like LuckPerms) and the legacy `access: op|all` config check. `PermissionDecision.resolve()` gives the provider result precedence; non-player sources (console) always use the legacy path.
- `PermissionCatalog.path()` maps command aliases to their canonical permission node — new aliases must be added there.
- All user-facing chat output goes through `Messages` (or `ModerationMessageFormatter`/`MessageFormatter` for templated text), which applies the configurable `[Essentials]` tag prefix.

### Moderation

`ModerationManager` is a static lifecycle holder around `ModerationService`, which owns the SQLite database (`config/essentials/essentials.db`, driver nested in the mod JAR via `include`). Bans, IP bans, kicks, warnings, mutes, and staff notes are all audit records queried by `/history`.

### Mixins

Mixins in `mixin/` intercept vanilla commands so they route through Essentials instead: ban/pardon/kick command mixins record moderation actions, `MsgCommandMixin` cancels vanilla `/msg` registration (Essentials registers its own) and blocks muted senders, `TeamMsgCommandMixin` enforces mutes on team chat. New mixins must be listed in `essentials.mixins.json`.

### Testing convention

Tests are pure JVM — no Minecraft or Fabric classes are bootstrapped. Logic meant to be tested is deliberately factored into Minecraft-free classes (e.g. `ConfigMerger`, `DurationParser`, `PermissionDecision`, `TpaRequests`, `BackPositions`, `ModerationService`). Keep new business logic out of command/mixin classes so it stays testable this way.

## Docs

`README.md` documents every command, permission node, and config option — keep it in sync when adding or changing any of these. `MODRINTH.md` is the Modrinth listing page.
