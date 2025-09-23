# Essentials (Fabric) (WIP)

Essentials is an SMP toolkit that, in my opinion, provides the essential admin and quality of life improvements for a survival server.

## Commands & Features

### Available Commands

- `/repair [target]` – Repairs the main-hand item if it is damageable. Op-level permission by default.
- `/heal [target]` – Restores health, hunger, and saturation to full. Op-level permission by default.
- `/feed [target]` – Tops off hunger and saturation. Op-level permission by default.
- `/flight [target]` – Toggles creative-style flight for survival players. Op-level permission by default.
- `/disposal`, `/trash`, `/trashcan` – Opens a temporary 9x3 inventory for throwing items away. Available to all players by default. The contents are deleted when the screen closes, and the player gets a reminder in chat.

`[target]` is optional. Defaults to executor.

### Features

- **Configurable Access Control**: Commands can be restricted to operators only or made available to all players via configuration
- **Customizable Chat Tags**: All messages include a configurable tag with custom text, colors, and styling
- **Smart Feedback**: All targeted commands send feedback to both the executor and the affected player, so nobody is surprised by a sudden heal or flight toggle
- **Configuration System**: YAML-based configuration system allows server administrators to customize command availability and access levels

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

Essentials uses a YAML configuration file located at `.minecraft/config/essentials.yaml` (or in your server's config directory). If no configuration file exists, default settings will be used.

## Build from source

```bash
./gradlew build
```

The remapped JAR will be in `build/libs/`.

## License

- MIT (see LICENSE)
