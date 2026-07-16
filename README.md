# Essentials (Fabric) (WIP)

Essentials is an SMP toolkit that, in my opinion, provides the essential admin and quality of life improvements for a survival server.

## Commands & Features

### Available Commands

The primary command is the configured command name. Aliases are shorter alternatives with identical behavior and permissions.

| Primary command | Aliases | Description | Default access |
| --- | --- | --- | --- |
| `/repair [target]` | None | Repairs the main-hand item if it is damageable. | Operators |
| `/heal [target]` | None | Restores health, hunger, and saturation to full. | Operators |
| `/feed [target]` | None | Tops off hunger and saturation. | Operators |
| `/flight [target]` | None | Toggles creative-style flight for survival players. | Operators |
| `/disposal` | `/trash`, `/trashcan` | Opens a temporary 9x3 inventory whose contents are deleted when closed. | Everyone |
| `/backpack` | `/bp` | Opens your persistent 9x3 backpack. See the backpack modes below. | Everyone |
| `/backpacksee <player>` | `/bpsee` | Opens and edits an online or previously joined player's backpack in per-player mode. | Operators |
| `/tpa <player>` | None | Sends a teleport request to another player. | Everyone |
| `/tpahere <player>` | None | Requests that another player teleport to you. Use `/tpahere all` to request all online players. | Everyone |
| `/tpaccept [player]` | None | Accepts a teleport request, or the newest request when no player is given. | Everyone |
| `/tpdeny [player]` | None | Denies a teleport request, or the newest request when no player is given. | Everyone |
| `/tpcancel` | None | Cancels your outgoing teleport request. | Everyone |
| `/back` | None | Returns to your previous position after a TPA teleport. | Everyone |
| `/inventorysee <player>` | `/isee` | Opens and edits an online or previously joined player's inventory. | Operators |
| `/enderchestsee <player>` | `/esee` | Opens and edits an online or previously joined player's ender chest. | Operators |
| `/ban <player> <duration> <reason>` | None | Temporarily bans an online or previously known offline player. Durations accept values such as `30m`, `2h`, `7d`, and `1d12h`. | Operators |
| `/pardon <player>` | `/unban` | Revokes an active player-account ban for an online or previously known offline player. | Operators |
| `/ban-ip <address-or-player> <duration> <reason>` | `/banip` | Temporarily bans an IPv4/IPv6 address. IPv6 addresses must be quoted. An online player target bans both their account and current address. | Operators |
| `/pardon-ip <address>` | `/unban-ip` | Revokes an active IP ban. IPv6 addresses must be quoted. | Operators |
| `/kick <player> <reason>` | None | Disconnects an online player and records the moderation action. | Operators |
| `/history <player> [all\|bans\|kicks] [page]` | `/audit` | Shows paginated moderation history for online or previously known offline players, including any active ban. | Operators |

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
- **Persistent Moderation**: Timed player/IP bans and the always-on kick audit log are stored in SQLite, survive restarts, and can be reviewed with `/history` or `/audit`

## Installation (Fabric)

### Dedicated server

1. Install Fabric Loader for your server's Minecraft version.
2. Download and place the built JAR of this mod into the `mods/` folder along with Fabric API.
3. Start the server. Vanilla clients can connect. The mod will automatically generate `config/essentials/essentials.yaml` and `config/essentials/essentials.db`.

### Singleplayer

1. Install Fabric Loader and Fabric API on your client.
2. Place the mod JAR into your client `mods/` folder.
3. Launch the game. The behavior applies in singleplayer because it runs an integrated server. The mod will automatically generate its files under `.minecraft/config/essentials/`.

## Configuration

Essentials uses `config/essentials/essentials.yaml` and stores moderation data in `config/essentials/essentials.db`. Existing `config/essentials.yaml` files are moved into the new folder automatically. If no configuration file exists, defaults are generated. After a mod update, new options are merged into your existing file automatically (see Automatic Config Migration above).

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

### Moderation Configuration

- `ban_message` controls the message shown to a banned player. It supports `{player}`, `{reason}`, `{moderator}`, `{time}`, and `{expires_at}`.
- The same `ban_message` is used for timed IP bans. `/ban-ip` accepts a literal address or the name of an online player; IPv6 addresses must be quoted (for example, `/ban-ip "2001:db8::10" 1h Proxy`). A player target atomically bans both their account and current address. `/banip` is an alias.
- `kick_message` controls the message shown to a kicked player. It supports `{player}`, `{reason}`, and `{moderator}`.
- Minecraft ampersand formatting codes such as `&c` and `&l` are supported.
- Kick audit logging is always enabled and has no configuration switch.
- `/history <player>` shows all moderation entries and any active ban; add `bans` or `kicks` to filter them and a page number to navigate older entries (10 per page). `/audit` is an alias. Dates use `DD/MM/YYYY` in the server's timezone.
- `/pardon <player>` (alias `/unban`) revokes only the active player-account ban. It does not revoke a separate IP ban.
- `/pardon-ip <address>` (alias `/unban-ip`) revokes only the active IP ban. IPv6 addresses must be quoted.

### Command Access

Every command can be enabled/disabled and have its access level configured (`op`/`all`) in the `commands` section.

## Build from source

```bash
./gradlew build
```

The remapped JAR will be in `build/libs/`. Run the unit tests with `./gradlew test`.

## License

- MIT (see LICENSE)
