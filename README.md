# Guppy

*"...just knowing that you have the patience of a guppy..."*
— X33n, [SkyFactory](https://youtu.be/AbYfcSAw-tM?t=1361)

Guppy notices when somebody speeds time up, and tells them they have the patience of a guppy.

It works with a **Time in a Bottle**, a **Watch of Flowing Time** (in an inventory or a DM pedestal), a
**Temporal Pouch** from Gadgets Against Grind, a **Torcherino**, and anything else built the same
way. That's because Guppy doesn't look for any of those items. It watches the ticks themselves.

## The useful bit

Every acceleration goes in the server log, so after a lag spike an owner can check who was doing
what, where and when:

```
[Guppy] Tick acceleration #12 started | who: Sablednah | what: entity tiab:time_accelerator (mod tiab: org.mangorage.tiab.common.entities.TimeAcceleratorEntity) | where: minecraft:overworld 120 64 -30, minecraft:furnace | when: 2026-10-21T22:04:11+01:00
[Guppy] Tick acceleration #12 still going after 5m 0s | ... | so far: 96,000 extra block-entity ticks on 1 block
[Guppy] Tick acceleration #12 stopped after 7m 30s | ... | 144,000 extra block-entity ticks on 1 block (about 320 a second) | ended: ...
```

- **who**: the player holding the item, or the player who just used the block. For something
  placed earlier, it's the player who placed it. If nobody can be named, the line says so and gives
  the nearest player.
- **what**: the item, entity or block handing out the ticks, plus the mod and class that did it.
- `/tick rate`, `/tick sprint` and `/gamerule randomTickSpeed` are logged too.
- `/guppy` (ops) lists what is speeding time up right now.

## The joke bit

The player is told *Patience of a Guppy* (or *Captain Guppy!*) in chat and on the action bar.
That only happens while they are actually using the thing: holding the watch, or having just
used the bottle or pouch on a block. A pedestal or Torcherino placed an hour ago, or a guess at
the nearest player, goes in the log and tells nobody.

Sprinklers, watering cans, twerking and growth accelerators also hand out extra ticks, but nobody
would call them impatient, so they're excluded by default. The exclusion list is in the config.

If the mod is installed on a client, *Patience of a Guppy* also joins the title-screen splashes.

## Config

`config/guppy-common.toml`. Changes apply when you save the file.

| Setting | Default | |
|---|---|---|
| `messages.messages` | `["Patience of a Guppy", "Captain Guppy!"]` | the pool. `{player}`, `{what}` and `&` colour codes work |
| `messages.random` | `true` | pick a random message; `false` always uses the first |
| `messages.audience` | `PLAYER` | `PLAYER`, `NEARBY`, `EVERYONE`, or `NONE` (server log only) |
| `messages.chat` / `actionBar` | `true` / `true` | where the message is shown |
| `messages.cooldownSeconds` | `60` | how long before the same player is told again |
| `log.enabled` | `true` | the who/what/where/when lines |
| `log.quietSeconds` | `5` | how long without extra ticks before an acceleration counts as stopped |
| `log.ongoingMinutes` | `5` | how often to log a progress line while one keeps going |
| `log.logCommands` / `commands` | `true` / tick and gamerule | which tick-rate commands are logged |
| `detection.blockEntities` / `randomTicks` | `true` / `true` | which kinds of extra tick count |
| `detection.exclude` | sprinklers, watering cans, twerking, growth accelerators | globs matched against the mod id, the item/entity/block id and the class; never reported |

## How it knows

Vanilla ticks a block entity from exactly one place, and random-ticks a block from exactly one
place. Guppy marks those two passes, so any tick that arrives from anywhere else is an extra one.
Time in a Bottle and the Temporal Pouch spawn an entity that ticks the block. The Watch of
Flowing Time calls the chunk's own ticker. A Torcherino ticks its neighbours from its own tick. All
of them come in from outside the two passes.

Guppy is server-side. Unmodified clients can join. Installing it on a client only adds the splash.

## Versions

| Minecraft | Loader | Java |
|---|---|---|
| 1.20.1 | Forge 47 | 17 |
| 1.21.1 | NeoForge 21.1 | 21 |
| 1.21.11 | NeoForge 21.11 | 21 |
| 26.1 / 26.2 / 26.3 | NeoForge 26.x | 25 |

MIT licence.
