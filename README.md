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
| `/ban <player> <duration\|permanent> <reason>` | None | Bans an online or previously known offline player. Use `permanent` or `perm` for no expiry. | Operators |
| `/pardon <player>` | `/unban` | Revokes an active player-account ban for an online or previously known offline player. | Operators |
| `/ban-ip <address-or-player> <duration\|permanent> <reason>` | `/banip` | Bans an IPv4/IPv6 address temporarily or permanently. IPv6 addresses must be quoted. An online player target bans both their account and current address. | Operators |
| `/pardon-ip <address>` | `/unban-ip` | Revokes an active IP ban. IPv6 addresses must be quoted. | Operators |
| `/kick <player> <reason>` | None | Disconnects an online player and records the moderation action. | Operators |
| `/warn <player> <reason>` | None | Records a warning for an online or previously known offline player. | Operators |
| `/mute <player> <duration\|permanent> <reason>` | None | Temporarily or permanently blocks a player's configured communication channels. | Operators |
| `/unmute <player>` | None | Revokes a player's active mute. | Operators |
| `/note <player> <text>` | None | Adds a private, staff-only note without notifying the player. | Operators |
| `/history <player> [all\|bans\|kicks\|warnings\|mutes\|notes] [page]` | `/audit` | Shows filtered, paginated moderation history and active moderation state. | Operators |

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
- **Persistent Moderation**: Bans, kicks, warnings, temporary/permanent mutes, revocations, and private staff notes are stored in SQLite, survive restarts, and can be reviewed with `/history` or `/audit`
- **Update Notifications**: After startup, one asynchronous Modrinth check can notify the console and authorized online operators about newer compatible releases

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

- `ban_message` controls the message shown to a banned player. It supports `{player}`, `{reason}`, `{moderator}`, `{time}`, and `{expires_at}`. Permanent bans render these last two placeholders as `Permanent` and `Never`.
- Ban durations accept values such as `30m`, `2h`, `7d`, and `1d12h`; use `permanent` or `perm` for a ban that never expires.
- The same `ban_message` is used for timed and permanent IP bans. `/ban-ip` accepts a literal address or the name of an online player; IPv6 addresses must be quoted (for example, `/ban-ip "2001:db8::10" permanent Proxy`). A player target atomically bans both their account and current address. `/banip` is an alias.
- `kick_message` controls the message shown to a kicked player. It supports `{player}`, `{reason}`, and `{moderator}`.
- Warning and mute feedback is controlled by `warning_message`, `mute_message`, `unmute_message`, `mute_blocked_message`, and `mute_expired_message`.
- `mute_blocks_private_messages` controls whether mutes also block `/msg`, `/tell`, `/w`, and team messages. Public chat is always blocked while muted.
- `warning_rolling_period` and `warning_alert_threshold` control the informational escalation count shown to staff. Essentials never applies an automatic punishment.
- Minecraft ampersand formatting codes such as `&c` and `&l` are supported.
- Kick audit logging is always enabled and has no configuration switch.
- `/history <player>` shows moderation entries and active state; use `bans`, `kicks`, `warnings`, `mutes`, or `notes` to filter and a page number to navigate older entries (10 per page). Staff notes require their dedicated history permission even when `all` is selected. `/audit` is an alias. Dates use `DD/MM/YYYY` in the server's timezone.
- `/pardon <player>` (alias `/unban`) revokes only the active player-account ban. It does not revoke a separate IP ban.
- `/pardon-ip <address>` (alias `/unban-ip`) revokes only the active IP ban. IPv6 addresses must be quoted.

### Command Access

Every command can be enabled/disabled and have its access level configured (`op`/`all`) in the `commands` section.

### Update Checks

The `updates` section controls the once-per-start Modrinth update check. It is enabled by default, accepts stable
releases only, waits 10 seconds after startup, and uses a 5-second request timeout. Set `channel` to
`include_prereleases` to consider beta and alpha versions. Console and player notifications can be disabled
independently, and `notification_text` supports `{installed_version}`, `{latest_version}`, `{channel}`, and
`{download_link}`. The link can be made non-clickable with `clickable_link: false`.

Players require `essentials.update.notify`; when no permission provider supplies a value, `op_fallback: true`
grants the notification to operators. Each eligible player is notified at most once per server session, including
when they join after the check finishes. The checker never downloads or installs files.

When a supported Fabric permission provider such as LuckPerms is installed, Essentials automatically uses the
nodes below. A provider grant or denial takes precedence over `access`. If no provider supplies a result, the
existing `access: op|all` check remains the fallback, so current servers keep the same behavior and configuration.
Aliases always use their primary command's node. Console execution is unchanged.

| Command | Permission node |
| --- | --- |
| `/repair` | `essentials.repair` |
| `/heal` | `essentials.heal` |
| `/feed` | `essentials.feed` |
| `/flight` | `essentials.flight` |
| `/disposal` (`/trash`, `/trashcan`) | `essentials.disposal` |
| `/tpa` | `essentials.tpa` |
| `/tpahere` | `essentials.tpahere` |
| `/tpaccept` | `essentials.tpaccept` |
| `/tpdeny` | `essentials.tpdeny` |
| `/tpcancel` | `essentials.tpcancel` |
| `/back` | `essentials.back` |
| `/backpack` (`/bp`) | `essentials.backpack` |
| `/backpacksee` (`/bpsee`) | `essentials.backpacksee` |
| `/enderchestsee` (`/esee`) | `essentials.enderchestsee` |
| `/inventorysee` (`/isee`) | `essentials.inventorysee` |
| `/ban` | `essentials.ban` |
| `/pardon` (`/unban`) | `essentials.pardon` |
| `/ban-ip` (`/banip`) | `essentials.banip` |
| `/pardon-ip` (`/unban-ip`) | `essentials.pardonip` |
| `/kick` | `essentials.kick` |
| `/warn` | `essentials.warn` |
| `/mute` | `essentials.mute` |
| `/unmute` | `essentials.unmute` |
| `/note` | `essentials.note` |
| `/history` (`/audit`) | `essentials.history` |
| Update availability notifications | `essentials.update.notify` |

Granular capabilities use these sub-permissions:

| Capability | Permission node |
| --- | --- |
| Use `/repair <target>` | `essentials.repair.others` |
| Use `/heal <target>` | `essentials.heal.others` |
| Use `/feed <target>` | `essentials.feed.others` |
| Use `/flight <target>` | `essentials.flight.others` |
| Use `/tpahere all` | `essentials.tpahere.all` |
| View warning history and rolling counts | `essentials.history.warnings` |
| View mute and revocation history | `essentials.history.mutes` |
| View private staff notes | `essentials.history.notes` |

Without a permission provider, each sub-permission uses its owning command's existing `access` result; it does
not add a new access tier or require any permission configuration.

## Build from source

```bash
./gradlew build
```

The remapped JAR will be in `build/libs/`. Run the unit tests with `./gradlew test`.

## License

- MIT (see LICENSE)
