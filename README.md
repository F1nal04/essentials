# Essentials (Fabric)

A lightweight set of server-friendly utility commands for Minecraft. Provides quick actions like repair, heal, feed, and a disposable trash GUI. Works in singleplayer and on dedicated servers. Clients do not need the mod to join a modded server running this.

## What it does

- /repair: Fully repairs the item in a player's main hand if it is damageable.
- /heal: Restores a player's health to full and also fills hunger and saturation.
- /feed: Fills a player's hunger and saturation to maximum.
- /disposal, /trash, /trashcan: Opens a 27-slot temporary container to throw items away; items are not kept.

## Commands

- /repair [target]
  - Permissions: Requires permission level 2 (operators by default).
  - If no target is given, acts on the command executor.
  - Only works on damageable items in the main hand.

- /heal [target]
  - Permissions: Requires permission level 2.
  - Heals to max health and fills hunger + saturation.

- /feed [target]
  - Permissions: Requires permission level 2.
  - Fills hunger and saturation to max.

- /disposal (aliases: /trash, /trashcan)
  - Permissions: None required by default; can be executed by any player.
  - Opens a temporary 9x3 container for discarding items.

Notes:
- When targeting another player with /repair, /heal, or /feed, appropriate feedback messages are sent to both the executor and the target.
- Command permission levels follow standard Minecraft server rules. Adjust via your permissions/ops setup as needed.

## Installation (Fabric)

### Dedicated server

1. Install Fabric Loader for your server's Minecraft version.
2. Download and place the built JAR of this mod into the `mods/` folder along with Fabric API.
3. Start the server. Vanilla clients can connect.

### Singleplayer

1. Install Fabric Loader and Fabric API on your client.
2. Place the mod JAR into your client `mods/` folder.
3. Launch the game. The behavior applies in singleplayer because it runs an integrated server.

## Build from source

```bash
./gradlew build
```

The remapped JAR will be in `build/libs/`.

## License

- MIT (see LICENSE)
