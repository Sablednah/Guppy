# Changelog

## 1.0.0

First release.

- Detects tick acceleration itself: extra block-entity ticks and extra random ticks, from whatever
  gives them. Covers Time in a Bottle, the Watch of Flowing Time (inventory and DM pedestal),
  Gadgets Against Grind's Temporal Pouch, Torcherino and anything else that works the same way.
- Server log: who, what, where and when when an acceleration starts, a progress line every few
  minutes, and a summary (extra ticks, blocks, rate) when it stops.
- Tells the player *Patience of a Guppy* in chat and on the action bar. The message pool,
  randomness, audience, cooldown and where it shows are all configurable, and the audience can be
  set to nobody (server log only).
- Logs `/tick` and `/gamerule randomTickSpeed`.
- `/guppy` lists the accelerations running right now.
- Adds *Patience of a Guppy* to the title-screen splashes on clients that have it installed.
