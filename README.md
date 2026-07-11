# Lipi

**An independent, server-controlled chat platform for Fabric Minecraft servers.**

![Minecraft 1.21.5](https://img.shields.io/badge/Minecraft-1.21.5-brightgreen)
![Fabric](https://img.shields.io/badge/Mod%20Loader-Fabric-blue)
![Alpha](https://img.shields.io/badge/Status-Alpha-orange)
![MIT License](https://img.shields.io/badge/License-MIT-yellow)

---

## What is Lipi

Lipi adds a completely independent chat channel that runs alongside vanilla Minecraft chat. It uses its own network packets — Lipi messages never touch the vanilla chat system.

**What it does:**

- Adds a second chat channel that operates independently of vanilla chat
- Both systems coexist — players can use either one at any time
- Messages are sent through custom network packets, not vanilla chat packets
- The server owner has full control over Lipi: logging, moderation, and configuration

**What it does NOT do:**

- It does not modify, replace, or interfere with vanilla Minecraft chat
- It does not alter any vanilla game behavior
- It does not send messages through vanilla chat pathways

Lipi is designed for small-scale private SMPs and friend group servers. It is not built for large public servers.

---

## Who is this for

- **Small SMP server owners** who want a server-controlled chat layer independent of Minecraft's default systems
- **Friend groups** running private Fabric servers
- **Players whose Minecraft chat has been disabled** due to account verification requirements — Lipi provides an alternative communication channel for servers where the owner has chosen to install it
- **Server owners** who want proper chat logging, moderation tools, and admin controls built in

**Not suitable for:** Large public servers. Lipi requires the mod installed on both the client and the server, which makes it impractical for open servers with many unmodded players.

---

## Important limitation

Both the client **and** the server must have Lipi installed. Players without the mod cannot send or receive Lipi messages. If a player presses the Lipi keybind on a server without the mod, they will see a `"This server does not support Lipi"` message.

This is a known design constraint, not a bug. Lipi uses custom network packets, so both sides need to understand them.

---

## Features

Every feature listed below is verified against the current source code.

- **Dedicated chat screen** — Press Right Shift to open a standalone Lipi chat input screen. This is a custom screen, not a modification of the vanilla chat screen.
- **Custom network packets** — All messages use Lipi's own packet system (`ChatMessagePayload`, `ChatBroadcastPayload`, `ChatHistoryPayload`, `ServerStatusPayload`), completely separate from vanilla.
- **HUD indicator** — A `[Lipi]` badge renders at the top center of the screen when the Lipi chat screen is open.
- **Input label** — A teal `[Lipi]` label appears above the chat input field on the Lipi chat screen.
- **Chat history on join** — When joining a server, the last 20 messages from today's log are displayed in italic grey with `--- Lipi History ---` and `--- End History ---` markers.
- **Server-side mute system** — Operators can silently mute players. Muted players' messages are dropped server-side without notification. Mute list is persisted to `config/lipi/muted-players.json`.
- **Flat-file chat logging** — Messages, joins, and leaves are logged to daily local files in `config/lipi/logs/`. Old logs are automatically cleaned based on the configured retention period.
- **Server toggle** — Operators can enable/disable Lipi at runtime. The state is persisted to the config file.
- **Configurable background opacity** — The Lipi chat screen background transparency is configurable on the client side.
- **Graceful fallback** — If the server doesn't support Lipi or has it disabled, players see a clear notice instead of errors.
- **Command passthrough** — The Lipi chat screen routes `/` commands through Minecraft's command system, so server commands still work normally while the Lipi screen is open.
- **Connection status** — On joining a supported server, players see a `[Lipi] Connected! Press Right Shift to open Lipi chat.` message.

---

## How to use

### For server owners

1. Download `lipi-mc1.21.5-2.0.0.jar`
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) for Minecraft 1.21.5
3. Place both jars in your server's `mods/` folder
4. Start the server — config files generate automatically at `config/lipi-server.toml`

### For players

1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21.5
2. Download `lipi-mc1.21.5-2.0.0.jar`
3. Download [Fabric API](https://modrinth.com/mod/fabric-api) for Minecraft 1.21.5
4. Place both jars in your `.minecraft/mods/` folder
5. Join a server that has Lipi installed
6. Press **Right Shift** to open the Lipi chat screen
7. Type your message and press **Enter**
8. Press **ESC** to close

---

## Commands

All `/lipi` subcommands require **operator permission level 3**.

| Command | Who can use it | What it does |
|---|---|---|
| `/lipi mute <player>` | Operators (level 3) | Silently drops the specified player's Lipi messages server-side. The player is not notified. |
| `/lipi unmute <player>` | Operators (level 3) | Removes the mute from the specified player. |
| `/lipi toggle` | Operators (level 3) | Toggles Lipi on or off for the entire server. Updates the config file. |
| `/lipi log <player>` | Operators (level 3) | Displays the last 10 Lipi messages from the specified player in today's log. |

---

## Server configuration

### Server — `config/lipi-server.toml`

| Config key | Default | What it does |
|---|---|---|
| `enabled` | `true` | Whether Lipi is active on the server. Can also be toggled at runtime with `/lipi toggle`. |
| `log-retention-days` | `30` | Number of days to retain log files. Set to `0` to disable logging entirely. |

### Client — `config/lipi-client.toml`

| Config key | Default | What it does |
|---|---|---|
| `chat-background-opacity` | `0.5` | Alpha value for the Lipi chat screen background (0.0 = fully transparent, 1.0 = fully opaque). |

### Mute list — `config/lipi/muted-players.json`

Managed via `/lipi mute` and `/lipi unmute`. Stores a JSON array of UUID strings. Manual edits require a server restart.

---

## Logging

### Where logs are stored

`config/lipi/logs/YYYY-MM-DD.log` — one file per day.

### Log format

```
[2026-06-27 16:45:12] [GLOBAL] [player-uuid] PlayerName: message text
[2026-06-27 16:45:12] [JOIN] [player-uuid] PlayerName joined Lipi
[2026-06-27 16:45:12] [LEAVE] [player-uuid] PlayerName left Lipi
```

### Log retention

Configure `log-retention-days` in `config/lipi-server.toml`. Logs older than the configured number of days are automatically deleted on server start. Set to `0` to disable logging entirely.

Server owners are responsible for their own logs and any applicable data policies.

---

## Known issues

Being honest about the current state:

- **Incoming messages render in vanilla chat HUD** — Lipi messages from other players currently appear in the vanilla chat HUD, not in a dedicated Lipi window. This is the top-priority UI fix for Alpha V3.
- **Chat history shows raw UUIDs** — The history lines sent on join include raw UUID strings. Cleanup is planned for Alpha V3.
- **Timestamps show server time** — Message timestamps use the server's timezone, not the player's local time.
- **No cross-version support** — 1.21.5 only. Backports are planned but not yet available.
- **Silent mute system** — Muted players are not notified that they are muted. Their messages are silently dropped.
- **Global-only messaging** — All messages go to a single global channel. No private messaging or multi-channel support yet.

---

## Roadmap

### Alpha V3 (next)

- Dedicated Lipi chat window (incoming messages no longer rendered in vanilla chat HUD)
- Timestamp format fix (HH:MM only)
- UUID cleanup in chat history

### Beta V1 (when UI is stable)

- Multi-channel support
- Media and GIF sharing
- Improved moderation tools

### Future

- 1.21.1 backport
- Forge/NeoForge port
- Additional Minecraft version support

---

## Building from source

### Requirements

- JDK 21+
- Gradle 8.14+

### Build

```bash
# Windows
.\gradlew.bat build

# Linux / macOS
./gradlew build
```

### Output

`build/libs/lipi-mc1.21.5-2.0.0.jar`

---

## License

Lipi is released under the [MIT License](LICENSE).

Copyright (c) 2026 Punith P.

---

## Contributing

Open issues for bugs. PRs welcome. This is a solo student project under active development.
