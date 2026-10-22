# Guppy

> *"...just knowing that you have the patience of a guppy..."*
> X33n, [SkyFactory](https://youtu.be/AbYfcSAw-tM?t=1361)

**Use a Time in a Bottle and Guppy tells you that you have the patience of a guppy. It tells
everyone else too.**

That's the joke. The useful part is that every tick acceleration also goes in the server log, with
who, what, where and when. The next time the server lags, you can check whether somebody had
three bottles stacked on a furnace array.

---

## What it catches

- **Time in a Bottle**
- **Watch of Flowing Time** (ProjectE), held in an inventory or set in a DM pedestal
- **Temporal Pouch** (Gadgets Against Grind)
- **Torcherino**, and anything else that speeds blocks up the same way

Guppy doesn't look for any of those items. It watches the ticks themselves. Vanilla ticks a block
from exactly one place, so a tick that arrives from anywhere else is an extra one, whichever mod
sent it. A mod nobody has heard of yet gets caught the same way.

## The joke

The player using it sees **Patience of a Guppy** or **Captain Guppy!** in chat and on the action
bar. Everyone else on the server sees **Sablednah is Captain Guppy!** in chat.

- Only someone **actually using it** gets called out: the player holding the watch, or the one who
  just used a bottle or pouch. A pedestal set up an hour ago goes in the log and tells nobody.
  Guppy never accuses a player just for standing nearby.
- **Once a minute at most** per player, however many bottles they use.
- **Sprinklers, watering cans, twerking and growth accelerators** hand out extra ticks too, but
  nobody would call them impatient, so they're excluded by default.
- Install it on a client as well and **Patience of a Guppy** joins the title-screen splashes.

## The useful part

```
[Guppy] Tick acceleration #12 started | who: Sablednah | what: entity tiab:time_accelerator (mod tiab: ...) | where: minecraft:overworld 120 64 -30, minecraft:furnace | when: 2026-10-21T22:04:11+01:00
[Guppy] Tick acceleration #12 still going after 5m 0s | ... | so far: 96,000 extra block-entity ticks on 1 block
[Guppy] Tick acceleration #12 stopped after 7m 30s | ... | 144,000 extra block-entity ticks on 1 block (about 320 a second)
```

- **who**: the player using it. For something placed earlier, it's the player who placed it. If
  nobody can be named, the line says so and gives the nearest player.
- **what**: the item, entity or block doing it, plus the mod and the code responsible
- **where** and **when**: the dimension, position and block, and the start and end times
- **how much**: the extra ticks, how many blocks got them, and the rate
- `/tick` and `/gamerule randomTickSpeed` are logged too
- `/guppy` (ops) lists what is speeding time up right now

## Configuration

`config/guppy-common.toml`, and changes apply when you save the file.

- **Messages**: your own pool to pick from at random, with `{player}`, `{what}` and `&` colour codes
- **Who sees it**: the player, everyone nearby, the whole server, or nobody (log only)
- **Where**: chat and/or action bar
- **The call-out to everyone else**: its own pool, and it can be switched off
- **Cooldown**: 60 seconds by default
- **Exclusions**: glob patterns matched against the mod id, item/entity/block id and class
- **Logging**: on or off, how quickly an acceleration counts as stopped, how often a progress line
  is written, and which commands are logged

## Versions

| Minecraft | Loader |
|---|---|
| 1.20.1 | Forge 47 |
| 1.21.1 | NeoForge |
| 1.21.11 | NeoForge |
| 26.1 / 26.2 / 26.3 | NeoForge |

It's a server-side mod, and unmodified clients can join. Installing it on a client only adds the
splash. Open source (MIT): [GitHub](https://github.com/Sablednah/Guppy).
