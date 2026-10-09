# DeadChest on Pl3xMap

A Paper plugin for Minecraft **26.2** that puts a marker on [Pl3xMap](https://modrinth.com/plugin/pl3xmap) for every [DeadChest](https://modrinth.com/plugin/dead-chest) grave.

* When a player dies and DeadChest places a chest, a chest-with-skull icon shows up at that spot on the map.
* When the chest is **claimed** (DeadChest's `DeadchestPickUpEvent`), the marker is removed right away.
* When the DeadChest **timer runs out**, or an admin removes the chest or gives it back (`/dc remove`, `/dc giveback` …), the marker is removed on the next layer refresh.
* Hover over a marker to see the owner. Click it to see the coordinates, item count, XP, how long ago the player died, and how much time is left.

## How it works

DeadChest has no "chest created" or "chest expired" event, so the plugin does not try to track chests itself. It adds one Pl3xMap layer per world, and Pl3xMap asks that layer for its markers every `layer.update-interval` seconds (2 by default). Each time, the plugin reads DeadChest's current list of chests. Because DeadChest's own list is used every time, the map can't fall out of sync with it, even after a restart, a `/dc` command or a Pl3xMap reload. With `live-update: true`, open browser tabs get the changes pushed to them without reloading the page.

## Requirements

| | Version |
|---|---|
| Server | Paper (or a fork) 26.2, Java 25 |
| DeadChest | 4.30.0+ (older versions work in a limited mode: only chests of online players are shown) |
| Pl3xMap | 26.2-555+ |

## Build

```bash
./gradlew build
```

The jar ends up in `build/libs/DeadChestPl3xMap-<version>.jar`. If Java 25 isn't installed, Gradle downloads it.

## Install

1. Put the jar in `plugins/` next to DeadChest and Pl3xMap.
2. Restart the server.
3. Edit `plugins/DeadChestPl3xMap/config.yml` if you want, then run `/deadchestmap reload`.

To use your own icon, replace `plugins/DeadChestPl3xMap/icon.png` and reload.

## Commands

| Command | Permission | |
|---|---|---|
| `/deadchestmap status` (alias `/dcmap`) | `deadchestmap.admin` | Shows how many dead chests are on the map |
| `/deadchestmap reload` | `deadchestmap.admin` | Reloads the config and icon |

## Config

The layer label, refresh interval, icon size, tooltip and popup templates (HTML, with `{player} {world} {x} {y} {z} {items} {xp} {died} {time_left} {status}`), whether to show never-expiring chests, and which worlds to skip are all set in `config.yml`.
