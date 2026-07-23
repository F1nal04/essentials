# Essentials

A lightweight, server-side Fabric toolkit for survival servers and SMPs. Essentials adds the everyday player commands and moderation tools most servers need without requiring a client-side mod.

> **Server-side only:** players can join with an unmodified vanilla client. Fabric API is required on the server.

## Features

### Player quality of life

- Teleport requests with `/tpa`, `/tpahere`, accept, deny, cancel, and `/tpahere all`
- `/back` after a completed teleport request
- 9x3 backpacks with per-player, shared-server, or vanilla ender chest modes
- A disposable 9x3 inventory with `/disposal`, `/trash`, or `/trashcan`

### Server administration

- Repair held items, heal or feed players, and toggle survival flight
- Inspect and edit inventories, ender chests, and per-player backpacks
- Manage previously joined players even while they are offline
- Changes stay synchronized when multiple admins inspect the same live storage
- Hide staff with `/vanish`, including entities, the tab list, commands, announcements, mobs, and collision
- Check server-reported latency with `/ping [player]`, including configurable color thresholds
- Monitor rolling server TPS with `/tps` across 5-second through 15-minute windows

### Moderation

- Timed or permanent player bans, including offline players
- Timed or permanent IPv4 and IPv6 bans
- Ban an online player's account and current IP together by using their name with `/ban-ip`
- Kick players with a required reason
- Pardon player accounts and IP addresses independently
- View paginated kick and ban history with `/history` or `/audit`, including active bans

Ban durations support values such as `30m`, `2h`, `7d`, and `1d12h`. Use `permanent` or `perm` for a permanent ban. IPv6 addresses must be quoted in commands.

## Command overview

| Area | Commands | Default access |
| --- | --- | --- |
| Teleport requests | `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpcancel`, `/back` | Everyone |
| Player storage | `/backpack` (`/bp`), `/disposal` (`/trash`, `/trashcan`) | Everyone |
| Player utilities | `/repair [target]`, `/heal [target]`, `/feed [target]`, `/flight [target]` | Operators |
| Server status | `/ping [player]`, `/tps` | Everyone |
| Inventory inspection | `/inventorysee` (`/isee`), `/enderchestsee` (`/esee`), `/backpacksee` (`/bpsee`) | Operators |
| Player moderation | `/ban`, `/pardon` (`/unban`), `/kick`, `/history` (`/audit`), `/vanish` | Operators |
| IP moderation | `/ban-ip` (`/banip`), `/pardon-ip` (`/unban-ip`) | Operators |

Every command group can be disabled or made available to either operators or all players. With a compatible
Fabric permission provider such as LuckPerms, stable `essentials.<command>` nodes provide granular access;
aliases share the primary command node. See the README for the complete permission-node table. Without a
provider, `commands.<name>.access` remains the only access check and behavior is unchanged.

## Configuration

The configuration is generated at `config/essentials/essentials.yaml`. Changes take effect after restarting the server.

| Option | Default | Purpose |
| --- | --- | --- |
| `tag.text` | `Essentials` | Text shown in the chat prefix |
| `tag.color` | `DARK_PURPLE` | Prefix text color using a Minecraft formatting name |
| `tag.bracketColor` | `DARK_GRAY` | Prefix bracket color |
| `tag.bold` | `true` | Makes the prefix text bold |
| `back.window_seconds` | `120` | How long `/back` remains available after a TPA teleport |
| `backpack.mode` | `per_player` | Backpack type: `per_player`, `serverwide`, or `ender_chest` |
| `tpa.timeout_seconds` | `60` | Time before a teleport request expires |
| `tpa.cooldown_seconds` | `10` | Cooldown after cancelling a teleport request |
| `tps.healthy.minimum_tps` | `18.0` | Lower bound for healthy TPS coloring |
| `tps.degraded.minimum_tps` | `15.0` | Lower bound for degraded TPS coloring |
| `vanish.persist_state` | `true` | Keeps UUID-based vanish state across reconnects and restarts |
| `vanish.chat_behavior` | `block` | Blocks public chat or routes it to staff-only visibility |
| `moderation.ban_message` | Included in generated config | Disconnect message used for player and IP bans |
| `moderation.kick_message` | Included in generated config | Disconnect message used for kicks |
| `updates.enabled` | `true` | Runs one asynchronous compatible-release check after startup |
| `updates.channel` | `stable_only` | Accepts stable releases only or includes prereleases |
| `updates.console_notifications` | `true` | Logs an available update to the server console |
| `updates.player_notifications` | `true` | Notifies authorized online players once per session |
| `updates.request_timeout_seconds` | `5` | Network timeout, clamped to 2-30 seconds |
| `updates.startup_delay_seconds` | `10` | Post-start delay, clamped to 0-300 seconds |
| `commands.<name>.enabled` | `true` | Enables or disables a command group |
| `commands.<name>.access` | `op` or `all` | Restricts the command group to operators or allows everyone |

Moderation messages support Minecraft ampersand formatting codes such as `&c` and `&l`.

- `ban_message`: `{player}`, `{reason}`, `{moderator}`, `{time}`, `{expires_at}`
- `kick_message`: `{player}`, `{reason}`, `{moderator}`

Available command configuration names include `repair`, `heal`, `feed`, `flight`, `disposal`, `tpa`, `back`,
`backpack`, `backpacksee`, `enderchestsee`, `inventorysee`, `ban`, `pardon`, `banip`, `pardonip`,
`kick`, `history`, `ping`, and `tps`.

## Installation

1. Install Fabric Loader and Fabric API on the server.
2. Place the Essentials JAR in the server's `mods` folder.
3. Start the server and adjust `config/essentials/essentials.yaml` as needed.

Requires Java 25 or newer. No client installation is required.
