# CLAUDE.md

Working notes. `README.md` says what the mod does. This file covers the conventions it doesn't.

Guppy is a small joke mod with one useful half: it logs tick acceleration. The build, stamp,
deploy and publishing setup follows `../CityWorld-ReForged` (and `../ChatFilter-ReForged`, which
has the mixin and self-test conventions). It ships on the same six lines as CityWorld.

## Build & run

```bash
export JAVA_HOME=/mnt/d/Repos/sable/CityWorld-ReForged/tools/jdk21   # jdk17 on mc1.20.1, jdk25 on 26.x
./gradlew compileJava
./gradlew runServer -Pselftest     # headless self-test: logs PASS/FAIL and stops the server
./gradlew build                    # build/libs/guppy-<v>+mc<mc>.jar
./deploy.sh                        # build + copy into this line's CurseForge instance
```

- `run/server.properties` uses port **25590** (flat world, offline). It's gitignored, so set it by hand.
- **`SelfTest` is the only headless proof.** It plays the three accelerator styles itself:
  `getTicker` from outside, the chunk's bound ticker (ProjectE's route, by reflection), and an
  entity's tick. It also checks that vanilla's own ticks open no session. A headless server has no
  player, so no random-tick pass runs. The test says so and turns random-tick detection on by hand.
- Real mods can go in `run/mods` for a live check (GAG and ProjectE exist for 1.21.1; Time in a
  Bottle exists for 1.21.1 and 26.x).

## How detection works (read before changing anything)

`watch/TickWatch` holds the whole idea. In short:

- **Block entities**: `BlockStateBaseMixin` wraps every server-side `getTicker` result in
  `GuppyTicker`. `LevelMixin` sets `vanillaSlot` around each `TickingBlockEntity.tick()` in
  `Level.tickBlockEntities`. The first `GuppyTicker` to run inside that call takes the slot and is
  vanilla's tick. Every other call counts as extra. That includes a ticker called from *inside*
  another block entity's vanilla tick, which is how pedestals and Torcherinos work.
- **Random ticks**: `ServerLevelMixin` counts depth around the `randomTick` call in `tickChunk`. A
  `randomTick` at depth 0 is extra.
- **Context** (who/what): the entity being ticked (`tickNonPassenger` / `tickPassenger`) and the
  item stack being inventory-ticked (`ItemStack.inventoryTick`). `Recent` remembers clicks and
  placements as a fallback.
- **Attribution**: one stack walk when a session opens, never per tick. The first frame that
  isn't Minecraft, the loader, mixin or Guppy is the culprit. A stack with no such frame belongs to
  vanilla (bone meal on a grown stem calls `randomTick`) and its session is silent. Mod ids come
  from the loader's scan data by package, because the runtime class-to-jar mapping changes between
  FML versions (`IModFileInfo.moduleName` is gone on FML 10).

Mixins use `defaultRequire = 0`. If one fails to apply, its pass is never seen, and
`blockEntityPassSeen` / `randomPassSeen` keep that kind of detection off rather than reporting
every vanilla tick. `GuppyEvents.healthCheck` warns after a minute of play.

**When porting, check these call sites first:** `Level.tickBlockEntities` (the
`TickingBlockEntity.tick()` invoke), `ServerLevel.tickChunk` (the `BlockState.randomTick` invoke),
`ServerLevel.tickNonPassenger` / `tickPassenger`, `ItemStack.inventoryTick` (the `Item.inventoryTick`
descriptor changes between lines), and `BlockStateBase.getTicker` / `randomTick`. Names that move
between lines (dimension id, player name) live in `watch/Compat`.

## Branches

`main` = 1.21.11, plus `mc1.20.1` (Forge 47, `legacyforge` MDG), `mc1.21.1`, `mc26.1`, `mc26.2`,
`mc26.3`. Features land on `main` and get cherry-picked onto the others. Docs live on `main` only.
Porting notes go in `docs/VERSIONS.md`. CurseForge only.

## Tone

Player-facing strings are dry. The joke is the message pool, which is the owner's to change.
