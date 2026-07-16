# Essentials

A lightweight, server-side Fabric mod that adds practical administration and quality-of-life commands for survival servers—without requiring players to install anything.

Essentials focuses on the features most small SMPs need: teleport requests, persistent backpacks, inventory inspection, moderation utilities, and configurable command access.

## Features

- **Teleport requests** – `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, and `/tpcancel`
- **Return teleport** – `/back` returns players to their previous position after a TPA teleport
- **Persistent backpacks** – Per-player, serverwide, or vanilla ender-chest-backed storage
- **Offline inventory management** – Operators can inspect and edit inventories, ender chests, and per-player backpacks even after a player logs out
- **Live inventory views** – Changes are synchronized while the inspected player or other viewers have the same storage open
- **Administration utilities** – Repair items, heal or feed players, and toggle survival flight
- **Timed and permanent bans** – `/ban <player> <duration|permanent> <reason>` persists ban state in SQLite and blocks reconnects until expiry or pardon
- **Persisted pardons** – `/pardon <player>` (alias `/unban`) revokes active account bans for online or previously known offline players and records the revocation in audit history
- **Timed and permanent IP bans** – `/ban-ip <address-or-player> <duration|permanent> <reason>` (alias `/banip`) bans a literal address, or both the account and current address when targeting an online player; IPv6 addresses must be quoted
- **Persisted IP pardons** – `/pardon-ip <address>` (alias `/unban-ip`) revokes an active IP ban while preserving its audit record; IPv6 addresses must be quoted
- **Audited kicks** – `/kick <player> <reason>` uses a configurable disconnect message and always records the moderator action
- **Moderation history** – `/history <player> [all|bans|kicks] [page]` (alias `/audit`) gives operators a paginated audit view for online or offline players, including any active ban
- **Disposal inventory** – Safely delete unwanted items using `/disposal` or its aliases `/trash` and `/trashcan`
- **Configurable permissions** – Enable or disable individual commands and choose whether they are available to everyone or operators only
- **Automatic configuration migration** – New settings are merged into existing configuration files while preserving customized values

## Administrative inventory commands

- `/inventorysee <player>` — Alias: `/isee`
- `/enderchestsee <player>` — Alias: `/esee`
- `/backpacksee <player>` — Alias: `/bpsee`

Inventory views are editable and support previously joined offline players. The backpack lookup command is available when backpacks use `per_player` mode.

## Backpack modes

- **Per-player** – Every player receives their own persistent backpack
- **Serverwide** – Everyone shares one synchronized backpack
- **Ender chest** – `/backpack` opens the player's vanilla ender chest

Open your backpack with `/backpack` or `/bp`.

## Server-side only

Essentials runs entirely on the server. Players can connect using an unmodified vanilla client.

Fabric API is required on the server.
