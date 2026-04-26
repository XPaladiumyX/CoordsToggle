# CoordsToggle

A Minecraft Paper plugin to toggle Bedrock Edition coordinates display for streamers - prevents stream sniping by hiding player coordinates on Bedrock clients.

## How It Works

When using GeyserMC through Velocity proxy:
1. **Velocity Proxy**: Has Geyser-Velocity and Floodgate pre-installed
2. **Backend Paper Servers**: Run the game server with just Floodgate (Geyser isn't needed here)
3. **CoordsToggle**: Installed on each backend Paper server

When a Bedrock player uses `/coordinates`, their coordinates display is hidden on their client.

## Requirements

### Backend Paper Server
- **Paper 1.21.4+**
- **Java 21**
- **Floodgate** must be installed

### Velocity Proxy
- **Velocity 3.x**
- **Geyser-Velocity** (has Floodgate built-in)

> Note: GeyserMC runs on the Velocity proxy, not on the backend servers. The backend servers only need Floodgate for player authentication.

## Installation

### 1. Velocity Proxy
1. Install [Geyser-Velocity](https://geysermc.org/download) on your Velocity proxy
2. That's it! Floodgate is included with Geyser-Velocity
3. Configure Geyser to point to your backend Paper servers

### 2. Backend Paper Servers
1. Install [Floodgate](https://geysermc.org/download) on each Paper server
2. Configure Floodgate to connect to your Velocity proxy
3. Place `CoordsToggle.jar` in the `plugins/` folder
4. Restart each backend server

## Commands

| Command | Alias | Description | Permission |
|---------|-------|-------------|------------|
| `/coordinates` | `/coords` | Toggle your coordinates display | `coords.toggle.use` |
| `/coordstoggle` | `/ctreload` | Reload plugin configuration | `coords.toggle.reload` |

## Configuration

Edit `config.yml` in the plugin folder:
```yaml
Prefix: "§dSky X §9Network §eCoordsToggle §8●⏺ "
```

The prefix is used in messages sent to players.

## Features

- Toggle coordinates display ON/OFF via command
- State saved per player in `playerdata/` folder
- Persists across player disconnect/reconnect
- Persists across server restarts
- Works for Bedrock Edition clients (via GeyserMC)
- Java Edition players get a message to use F3

## Building from Source

### Prerequisites
- Java 21
- Maven

### Build Commands
```bash
# Windows (double-click build.bat)
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