# ChatMC

An independent, lightweight chat layer for Fabric servers that runs alongside vanilla Minecraft chat.

---

## Motivation

Recent legislation in several jurisdictions — including the UK Online Safety Act and the EU Digital Services Act — has introduced requirements around chat reporting, content moderation, and age verification that affect how vanilla Minecraft chat operates. These measures are well-intentioned, but they can create friction for small community servers that already manage their own moderation.

ChatMC provides an alternative communication channel that is entirely server-controlled. It does not replace, modify, or interfere with vanilla chat in any way. Both systems coexist: players can freely switch between them at any time. Server operators who install ChatMC retain full control over their own chat logs, moderation tools, and data retention policies.

ChatMC is a quality-of-life tool for server communities. It is not designed to circumvent any legal obligation. Server operators remain solely responsible for compliance with all applicable laws in their jurisdiction.

---

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.21.5 |
| Fabric Loader | >= 0.16.0 |
| Fabric API | 0.124.0+1.21.5 or later |
| Java | >= 21 |

ChatMC must be installed on **both** the server and the client. Players without the mod installed will not see ChatMC messages and will continue using vanilla chat normally.

---

## Installation

### Server

1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21.5.
2. Place [Fabric API](https://modrinth.com/mod/fabric-api) in the server's `mods/` directory.
3. Place the `chatmc-x.x.x.jar` file in the server's `mods/` directory.
4. Start the server. ChatMC will generate its default configuration files on first launch.

### Client

1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21.5.
2. Place [Fabric API](https://modrinth.com/mod/fabric-api) in your `mods/` directory.
3. Place the same `chatmc-x.x.x.jar` file in your `mods/` directory.
4. Launch the game. When you join a server that has ChatMC installed, you will see a confirmation message in chat.

---

## Features

- **Independent chat channel** — ChatMC messages are sent through custom network packets, completely separate from vanilla Minecraft chat. Both systems run side by side.
- **Toggle keybind** — Press **Right Shift** to switch between ChatMC mode and vanilla chat. A confirmation message is shown each time you toggle.
- **HUD indicator** — When ChatMC mode is active, a `[ChatMC Active]` badge is displayed at the top center of the screen.
- **Chat bar indicator** — When the chat screen is open in ChatMC mode, a teal `[ChatMC]` label is rendered inside the chat input field.
- **Chat history on join** — When a player joins the server, the last 20 messages from the current day's log are sent to the client, displayed in italic grey with a `--- ChatMC History ---` header.
- **Chat background opacity** — The transparency of the ChatMC overlay background is configurable via the client config file.
- **Server-side mute system** — Operators can silently mute players. Muted players' messages are dropped server-side without notification to the sender. The mute list persists across server restarts.
- **Flat-file chat logging** — All ChatMC messages, joins, and leaves are logged to daily flat files with configurable retention and automatic cleanup on server start.
- **Graceful fallback** — If the server does not have ChatMC installed, pressing Right Shift displays a notice: *"This server does not support ChatMC."* If ChatMC is installed but disabled on the server, a separate notice is shown.

---

## Commands

All `/chatmc` subcommands require **operator permission level 3**.

| Command | Description |
|---|---|
| `/chatmc mute <player>` | Mutes the specified player. Their ChatMC messages are silently dropped server-side. |
| `/chatmc unmute <player>` | Removes the mute from the specified player. |
| `/chatmc toggle` | Toggles ChatMC on or off for the entire server. The new state is persisted to the config file. |
| `/chatmc log <player>` | Displays the last 10 ChatMC messages from the specified player in today's log. |

---

## Configuration

### Server — `config/chatmc-server.toml`

| Key | Type | Default | Description |
|---|---|---|---|
| `enabled` | boolean | `true` | Whether ChatMC is active on the server. Can also be toggled at runtime via `/chatmc toggle`. |
| `log-retention-days` | integer | `30` | Number of days to retain chat log files. Set to `0` to disable logging entirely. Old logs are deleted on server start. |

### Client — `config/chatmc-client.toml`

| Key | Type | Default | Description |
|---|---|---|---|
| `chat-background-opacity` | float | `0.5` | Alpha value for the chat overlay background. `0.0` is fully transparent, `1.0` is fully opaque. |

### Mute List — `config/chatmc/muted-players.json`

Managed automatically via `/chatmc mute` and `/chatmc unmute`. Stored as a JSON array of UUID strings. This file can be edited manually if needed, but changes require a server restart to take effect.

---

## Logging and Data

### Log Storage

Chat logs are written to `config/chatmc/logs/` on the server, one file per day in the format `YYYY-MM-DD.log`. Each line follows one of these formats:

```
[2026-06-27 16:45:12] [GLOBAL] [player-uuid] PlayerName: message text
[2026-06-27 16:45:12] [JOIN]   [player-uuid] PlayerName joined ChatMC
[2026-06-27 16:45:12] [LEAVE]  [player-uuid] PlayerName left ChatMC
```

Logs are append-only. A new file is created automatically each day. Old log files are deleted on server start based on the `log-retention-days` configuration value.

### Server Operator Responsibility

ChatMC stores chat data locally on the server machine. No data is transmitted to any third party. The server operator is the **data controller** and bears sole responsibility for:

- **Moderation** of chat content within the ChatMC channel.
- **Retention and deletion** of chat log files in accordance with applicable data protection laws (e.g., GDPR, UK Data Protection Act 2018).
- **Informing players** about what data is collected, how long it is stored, and how to request its deletion.
- **Legal compliance** in their jurisdiction, including but not limited to obligations under the UK Online Safety Act, the EU Digital Services Act, and any other relevant legislation.

The authors of ChatMC provide this software as-is and accept no liability for how it is deployed or operated.

---

## Known Limitations

- ChatMC requires the mod to be installed on both the server and the client. There is no way for unmodded clients to participate in ChatMC chat.
- Players without the mod will not receive ChatMC messages, nor will ChatMC users see vanilla chat messages sent by unmodded players (vanilla chat continues to function normally for all players regardless).
- The mute system is silent by design — muted players are not notified that their messages are being dropped.
- There is currently no private messaging or per-channel system. All ChatMC messages are broadcast to every connected player who has the mod installed.
- Chat history on join is limited to the current day's log file.

---

## Building from Source

```bash
./gradlew build
```

The compiled mod JAR will be placed in `build/libs/`.

---

## License

ChatMC is released under the [MIT License](LICENSE).

Copyright (c) 2026 Punith P.

---

## Contributing

Contributions are welcome. Please open an issue to discuss proposed changes before submitting a pull request. Keep PRs focused on a single feature or fix.
