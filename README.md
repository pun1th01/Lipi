# Lipi

An independent, server-controlled chat channel for Fabric servers that runs alongside vanilla Minecraft chat.

## About

Lipi provides a secondary, opt-in communication channel that operates independently of the vanilla chat system. It uses custom network packets for messaging, meaning Lipi messages are entirely separate from vanilla chat. Both systems can coexist, allowing players to switch between them as needed.

Server operators who install Lipi have full control over this channel, including local chat logging and moderation tools.

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.21.5 |
| Fabric Loader | >= 0.16.0 |
| Fabric API | 0.124.0+1.21.5 or later |
| Java | >= 21 |

Lipi must be installed on **both** the server and the client. Players without the mod installed will not see Lipi messages and will continue using vanilla chat normally.

## Installation

### Server

1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21.5.
2. Place [Fabric API](https://modrinth.com/mod/fabric-api) in the server's `mods/` directory.
3. Place the `lipi-x.x.x.jar` file in the server's `mods/` directory.
4. Start the server. Default configuration files will be generated.

### Client

1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21.5.
2. Place [Fabric API](https://modrinth.com/mod/fabric-api) in your `mods/` directory.
3. Place the `lipi-x.x.x.jar` file in your `mods/` directory.
4. Launch the game. A confirmation message will appear when joining a supported server.

## Features

- **Independent Channel** — Messages are sent via custom packets, keeping them separate from vanilla chat.
- **Toggle Keybind** — Press **Right Shift** to switch between Lipi mode and vanilla chat.
- **HUD Indicator** — A `[Lipi Active]` badge appears at the top center of the screen when active.
- **Chat Bar Indicator** — A teal `[Lipi]` label appears in the chat input field when typing in Lipi mode.
- **Chat History** — On join, the last 20 messages from the current day's log are displayed in italic grey with a `--- Lipi History ---` header.
- **Background Opacity** — Configurable transparency for the Lipi overlay background.
- **Server-Side Mutes** — Operators can silently mute players, dropping their Lipi messages server-side.
- **Flat-File Logging** — Messages, joins, and leaves are logged to daily local files.
- **Graceful Fallback** — Displays a notice if the server does not support Lipi or if it is disabled.

## Commands

All `/lipi` subcommands require **operator permission level 3**.

| Command | Description |
|---|---|
| `/lipi mute <player>` | Silently drops the specified player's Lipi messages server-side. |
| `/lipi unmute <player>` | Removes the mute from the specified player. |
| `/lipi toggle` | Toggles Lipi on or off for the server and updates the config file. |
| `/lipi log <player>` | Displays the last 10 Lipi messages from the specified player in today's log. |

## Configuration

### Server — `config/lipi-server.toml`

| Key | Type | Default | Description |
|---|---|---|---|
| `enabled` | boolean | `true` | Whether Lipi is active on the server. |
| `log-retention-days` | integer | `30` | Number of days to retain log files. `0` disables logging. |

### Client — `config/lipi-client.toml`

| Key | Type | Default | Description |
|---|---|---|---|
| `chat-background-opacity` | float | `0.5` | Alpha value for the chat overlay background (0.0 to 1.0). |

### Mute List — `config/lipi/muted-players.json`

Managed via `/lipi mute` and `/lipi unmute`. Stores a JSON array of UUID strings. Manual edits require a server restart.

## Logging and Data

Chat logs are saved locally on the server in `config/lipi/logs/` as daily flat files (`YYYY-MM-DD.log`). Old logs are automatically deleted on server start based on the `log-retention-days` setting.

```
[2026-06-27 16:45:12] [GLOBAL] [player-uuid] PlayerName: message text
[2026-06-27 16:45:12] [JOIN]   [player-uuid] PlayerName joined Lipi
[2026-06-27 16:45:12] [LEAVE]  [player-uuid] PlayerName left Lipi
```

Server operators have full control over this data. Operators are solely responsible for moderation, log storage, retention policies, and managing their server data.

## Known Limitations

- Requires installation on both the client and the server.
- Unmodded clients cannot send or receive Lipi messages.
- The mute system is silent; muted players are not notified.
- All messages are global; there is no private messaging or channel system.
- Chat history on join only includes messages from the current day's log file.

## License

Lipi is released under the [MIT License](LICENSE).

Copyright (c) 2026 Punith P.

## Credits

Developed by the Lipi Team.
