# lhSkyBlock

A Minecraft SkyBlock plugin for Spigot/Paper servers that provides island management and shop functionality.

## Features

- **Island Management**: Create, delete, and manage personal islands
- **Shop System**: Create and manage shops for trading
- **Economy Integration**: Vault support for economic transactions
- **Permissions System**: LuckPerms integration for advanced permission management
- **Citizens Integration**: Optional Citizens plugin support

## Commands

### Island Commands
- `/island` or `/is` - Main island command
  - `/island create` - Create a new island
  - `/island delete` - Delete your island
  - `/island home` - Teleport to your island
  - `/island help` - Show help information

### Shop Commands
- `/shop create` - Create a new shop
- `/shop delete` - Delete a shop
- `/shop reload` - Reload shop configuration

## Permissions

- `lhskyblock.island` - Allows players to manage their islands (default: true)
- `lhskyblock.shop` - Allows players to manage npc shops (default: false)

## Dependencies

### Required
- **Vault** - For economy integration

### Optional
- **LuckPerms** - For advanced permission management
- **Citizens** - For NPC integration

## Configuration

The plugin will generate configuration files in the `plugins/lhSkyBlock/` directory after first run. Check these files to customize the plugin behavior.
