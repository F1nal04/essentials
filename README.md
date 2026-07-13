# Essentials (Fabric) (WIP)

Essentials is an SMP toolkit that, in my opinion, provides the essential admin and quality of life improvements for a survival server.

## Commands & Features

### Available Commands

- `/repair [target]` – Repairs the main-hand item if it is damageable. Op-level permission by default.
- `/heal [target]` – Restores health, hunger, and saturation to full. Op-level permission by default.
- `/feed [target]` – Tops off hunger and saturation. Op-level permission by default.
- `/flight [target]` – Toggles creative-style flight for survival players. Op-level permission by default.
- `/disposal`, `/trash`, `/trashcan` – Opens a temporary 9x3 inventory for throwing items away. Available to all players by default. The contents are deleted when the screen closes, and the player gets a reminder in chat.
- `/backpack`, `/bp` – Opens your backpack, a persistent 9x3 storage. Available to all players by default. See the backpack modes below.
- `/backpacksee <player>` (`/bpsee`) – Opens and edits a player's backpack in per-player mode. Op-level permission by default.
- `/tpa <player>` – Send a teleport request to another player. Available to all players by default.
- `/tpahere <player>` – Send a request for another player to teleport to you. Available to all players by default.
- `/tpahere all` – Send TPAHere requests to all online players. Available to all players by default.
- `/tpaccept [player]` – Accept a teleport request (the newest one if no player is given). Available to all players by default.
- `/tpdeny [player]` – Deny a teleport request (the newest one if no player is given). Available to all players by default.
- `/tpcancel` – Cancel your outgoing teleport requests. Available to all players by default.
- `/back` – Teleport back to your previous position (works after TPA teleports). Available to all players by default.
- `/inventorysee <player>` (`/isee`) – Open an online or previously joined player's full inventory (storage, hotbar, armor, offhand) in an editable view. Op-level permission by default.
- `/enderchestsee <player>` (`/esee`) – Open an online or previously joined player's ender chest in an editable view. Op-level permission by default.

`[target]` is optional. Defaults to executor.

### Backpack Modes

The backpack has three modes, set via `backpack.mode` in the config:

- `per_player` (default) – Each player has their own persistent backpack.
- `serverwide` – All players share a single backpack.
- `ender_chest` – The backpack opens the player's ender chest: same live contents as an ender chest block, persisted by vanilla.

### Features

- **Configurable Access Control**: Commands can be restricted to operators only or made available to all players via configuration
- **Customizable Chat Tags**: All messages include a configurable tag with custom text, colors, and styling
- **Smart Feedback**: All targeted commands send feedback to both the executor and the affected player, so nobody is surprised by a sudden heal or flight toggle
- **Configuration System**: YAML-based configuration system allows server administrators to customize command availability and access levels
- **Automatic Config Migration**: When a mod update changes the config schema, your `essentials.yaml` is migrated automatically — your settings are kept, new options are added with defaults, the old file is backed up to `essentials.yaml.bak`, and a startup log warning lists exactly what changed
- **TPA System**: Full teleport request system with configurable timeouts, cooldowns, and smart request management
- **Back Command**: Return to your previous position after TPA teleports with a configurable time window
- **Admin Inventory Views**: `/inventorysee` and `/enderchestsee` give operators editable views into online and offline players' inventories and ender chests

## Installation (Fabric)

### Dedicated server

1. Install Fabric Loader for your server's Minecraft version.
2. Download and place the built JAR of this mod into the `mods/` folder along with Fabric API.
3. Start the server. Vanilla clients can connect. The mod will automatically generate a default configuration file at `config/essentials.yaml`.

### Singleplayer

1. Install Fabric Loader and Fabric API on your client.
2. Place the mod JAR into your client `mods/` folder.
3. Launch the game. The behavior applies in singleplayer because it runs an integrated server. The mod will automatically generate a default configuration file at `.minecraft/config/essentials.yaml`.

## Configuration

Essentials uses a YAML configuration file located at `.minecraft/config/essentials.yaml` (or in your server's config directory). If no configuration file exists, default settings will be used. After a mod update, new options are merged into your existing file automatically (see Automatic Config Migration above).

### Chat Tag

The `tag` section controls the chat prefix on all mod messages:

- `text` (default: `Essentials`) - The tag text shown in brackets
- `color` / `bracketColor` - Colors from Minecraft's formatting names (e.g. `AQUA`, `DARK_GRAY`, `DARK_PURPLE`)
- `bold` (default: `true`) - Whether the tag text is bold

### Backpack Configuration

- `mode` (default: `per_player`) - One of `per_player`, `serverwide`, or `ender_chest` (see Backpack Modes above)

### TPA Configuration

The TPA system includes the following configurable options:

- `timeout_seconds` (default: 60) - How long teleport requests last before expiring
- `cooldown_seconds` (default: 10) - How long to wait after cancelling a request before sending another
- `window_seconds` for back command (default: 120) - Time window during which `/back` can be used after a TPA teleport

### Command Access

Every command can be enabled/disabled and have its access level configured (`op`/`all`) in the `commands` section.

## Build from source

```bash
./gradlew build
```

The remapped JAR will be in `build/libs/`. Run the unit tests with `./gradlew test`.

## License

- MIT (see LICENSE)
