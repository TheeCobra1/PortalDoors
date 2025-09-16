# PortalDoors

A Spigot plugin that allows players to link doors together for instant teleportation and create portal networks across your server.

## Features

- **Door Linking**: Connect any two doors for instant teleportation
- **Portal Networks**: Create multi-door networks with GUI selection
- **Visual Effects**: Particle effects and sounds
- **Access Control**: Public/private networks with player permissions
- **One-Way Networks**: Create entry-only portal systems
- **Door Naming**: Custom names for organized portal systems
- **Cooldown System**: Configurable teleport cooldowns
- **Data Persistence**: Automatic saving of all portal configurations

## Installation

1. Download the latest release
2. Place `PortalDoors.jar` in your server's `plugins` folder
3. Restart your server
4. Configure settings in `plugins/PortalDoors/config.yml` (optional)

## Usage

### Basic Door Linking
```
/door              # Start linking - look at first door, then second door
/door unlink       # Remove door link (look at door)
/door cancel       # Cancel pending link
/door info         # View door information
```

### Portal Networks
```
/door network create <n>           # Create a new network
/door network add <n>              # Add door to network (look at door)
/door network list                    # List all networks
/door network public <n>           # Toggle public access
/door network oneway <n>           # Toggle one-way mode
/door network entry <n>            # Set entry door (one-way only)
/door network access <n> <player>  # Grant/revoke player access
```

### Door Naming
```
/door name <n>      # Name a door (supports color codes with &)
/door name clear       # Remove door name
/door name             # View current name
```

## Requirements

- **Minecraft**: 1.20.6+
- **Java**: 21+
- **Server**: Spigot/Paper

## Configuration

Key config options in `config.yml`:
- Teleport cooldowns and sounds
- Particle effects settings
- Network limitations
- Permission requirements
- Custom messages

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `portaldoors.use` | Basic plugin usage | true |
| `portaldoors.link` | Link doors together | true |
| `portaldoors.unlink.others` | Unlink others' doors | op |
| `portaldoors.bypass.cooldown` | Skip teleport cooldown | op |

## How It Works

1. **Simple Linking**: Look at a door and run `/door` twice to link two doors
2. **Networks**: Create networks for multiple interconnected doors
3. **Teleportation**: Walk through open linked doors to teleport instantly
4. **Access Control**: Control who can use your portal networks

## Effects

- **Portal Particles**: Particle effects when doors are opened
- **Teleport Effects**: Particles and sounds during teleportation
- **Link Creation**: Visual feedback when creating door connections
