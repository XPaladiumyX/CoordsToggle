# CoordsToggle

A Minecraft Paper plugin to toggle Bedrock Edition coordinates display for streamers - prevents stream sniping by hiding player coordinates on Bedrock clients.

## Requirements

### Server Side
- **Paper 1.21.4+** server
- **Java 21**
- **Geyser-Spigot** - Must be installed on the Paper server
- **Floodgate** - Required for Bedrock connections

### Proxy Side (Velocity)
- **Velocity 3.x** proxy
- **Geyser-Velocity** - Installed on Velocity
- **CoordsToggle-Velocity** - This plugin (install on Velocity)

## Installation

### 1. Paper Server (Geyser + Floodgate)
1. Install [Geyser-Spigot](https://geysermc.org/download) on your Paper server
2. Install [Floodgate](https://geysermc.org/download) on your Paper server
3. Place `CoordsToggle.jar` in the `plugins/` folder
4. Restart the server

### 2. Velocity Proxy
1. Install [Geyser-Velocity](https://geysermc.org/download) on your Velocity proxy
2. Download **CoordsToggle-Velocity** from [Releases](https://github.com/XPaladiumyX/CoordsToggle/releases)
3. Place `CoordsToggle-Velocity.jar` in Velocity's `plugins/` folder
4. Restart the proxy

## How It Works

- When a Bedrock player uses `/coordinates` or `/coords`, their coordinates display is hidden
- The toggle state is saved per player and persists across:
  - Player disconnect/reconnect
  - Server restarts
  - Full proxy restarts
- Java Edition players see a message telling them to use F3 (coordinates cannot be hidden on Java)

## Commands

| Command | Alias | Description | Permission |
|---------|------|-------------|------------|
| `/coordinates` | `/coords` | Toggle your coordinates display | `coords.toggle.use` |
| `/coordstoggle` | `/ctreload` | Reload plugin configuration | `coords.toggle.reload` |

## Configuration

Edit `config.yml` in the plugin folder:
```yaml
Prefix: "§dSky X §9Network §eCoordsToggle §8●⏺ "
```

The prefix is used in messages sent to players.

## Building from Source

### Prerequisites
- Java 21
- Maven

### Build Commands
```bash
# Using build.bat (Windows)
build.bat

# Or manually
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.10
mvn clean package
```

The compiled JAR will be in `target/CoordsToggle-1.1.0.jar`

## Permissions

- `coords.toggle.use` - Use `/coordinates` command (default: true)
- `coords.toggle.reload` - Reload config (default: op)

## Support

For issues or questions, please open an issue on GitHub:
https://github.com/XPaladiumyX/CoordsToggle

## License

MIT License - See LICENSE file