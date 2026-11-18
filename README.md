# Banlist • See and manage every banned player in a menu
Made by Mineconed

A clean menu with every banned player on your server. See who banned them, why, when, and when the ban ends. Unban with one click.
Works on **1.8 to 1.21** (Spigot and Paper). No other plugins needed.

## Features
- 📋 **Menu with all bans**, newest first, with player heads
- 🔍 Reason, banned by, date, type (tempban / permanent) and time left
- ✅ **Click to unban** with a Yes/No confirm screen
- 🔀 Filter: all bans, only tempbans, only permanent
- 📄 Pages when you have many bans
- 💬 `/banlist <name>` shows info in chat (with tab complete)
- 🌍 English and Danish built in, make your own `messages_xx.yml`
- ⚙️ Works with vanilla `/ban`, Essentials `/ban` and `/tempban`

## Commands
| Command | |
|---|---|
| `/banlist` | Open the menu (also `/bans`, `/banliste`) |
| `/banlist <name>` | Ban info in chat |
| `/banlist temp` / `perm` | Open the menu with a filter |
| `/banlist reload` | Reload config and messages |

## Permissions
| Permission | Default | |
|---|---|---|
| `banliste.se` | op | See the banlist |
| `banliste.unban` | op | Unban from the menu and reload |

## Config
```yaml
language: "en"            # en or da
unban_kommando: ""        # empty = server unban. Essentials: "essentials:unban {navn}"
dato_format: "dd/MM/yyyy HH:mm"
```
