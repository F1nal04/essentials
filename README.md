# Essentials (Fabric) (WIP)

Essentials is an SMP toolkit that, in my opinion, provides the essential admin and quality of life improvements for a survival server.

## Commands & Features

- `/repair [target]` – Repairs the main-hand item if it is damageable. Permission level 2.
- `/heal [target]` – Restores health, hunger, and saturation to full. Permission level 2.
- `/feed [target]` – Tops off hunger and saturation. Permission level 2.
- `/flight [target]` – Toggles creative-style flight for survival players. Permission level 2.
- `/disposal`, `/trash`, `/trashcan` – Opens a temporary 9x3 inventory for throwing items away. Anyone can run it. The contents are deleted when the screen closes, and the player gets a reminder in chat.

`[target]` is optional. Defaults to executor.

All targeted commands send feedback to both the executor and the affected player, so nobody is surprised by a sudden heal or flight toggle.

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
