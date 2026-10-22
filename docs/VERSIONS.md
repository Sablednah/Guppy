# Versions

| Branch | Minecraft | Loader | MDG | JDK | Source changes from `main` |
|---|---|---|---|---|---|
| `main` | 1.21.11 | NeoForge 21.11.42 | 2.0.141 | 21 | none |
| `mc1.20.1` | 1.20.1 | **Forge** 47.4.23 | legacyforge 2.0.147 | 17 | see below |
| `mc1.21.1` | 1.21.1 | NeoForge 21.1.251 | 2.0.141 | 21 | `Item.inventoryTick(…, int, boolean)` (runs client-side too, so guarded); `ResourceKey.location`; `GameProfile.getName`; op level as an int; `EntityType.create(Level)` |
| `mc26.1` | 26.1.2 | NeoForge 26.1.2.95 | 2.0.144 | 25 | toolchain and `JAVA_25` mixin level only |
| `mc26.2` | 26.2 | NeoForge 26.2.0.59 | 2.0.144 | 25 | toolchain only |
| `mc26.3` | 26.3 | NeoForge 26.3.0.58-beta | 2.0.147 | 25 | config registered as `LOCAL` with the file name kept (FML 12) |

Ported 2026-10-21. The hooked call sites (see CLAUDE.md) have the same shape on every line.

## Self-test results (2026-10-21)

| Line | Checks | Live, against the real mods in `run/mods` |
|---|---|---|
| 1.21.11 | 27 PASS | none: no accelerator mod is built for 1.21.11 |
| 1.21.1 | 33 PASS | Time in a Bottle 6.5.4, GAG Temporal Pouch 5.2.0 (+ Placebo), ProjectE 1.1.0 Watch of Flowing Time in a hotbar: all caught and named |
| 26.1 / 26.2 | 29 PASS | Time in a Bottle 8.0.0 |
| 26.3 | 27 PASS | none: TiaB 8.0.0 does not load on NeoForge 26.3.0.58 (it uses `ModConfig.Type.SERVER`, which FML 12 removed) |
| 1.20.1 | 27 PASS (dev) | none: the dev runtime is mojmap and released 1.20.1 jars are SRG |

## 1.20.1 (Forge)

- `mods.toml` (not `neoforge.mods.toml`) with `mandatory=true`; mixins are found through the jar
  manifest's `MixinConfigs`, not a `[[mixins]]` block.
- A refmap is written by the Mixin annotation processor (`legacyForge.mixin`). **Check the shipped
  jar's `guppy.refmap.json`**: an empty one compiles and runs in dev, then fails in production.
- No MixinExtras, so `@Redirect` replaces `@WrapOperation` (same bodies, calling the target directly).
- Forge doesn't merge mod `texts/splashes.txt`, so `SplashManagerMixin` (client) adds the line to
  the list `prepare` returns. It copies the list rather than shadowing a field, to stay clear of
  reobfuscation.
- `ForgeConfigSpec.defineListAllowEmpty` has no element-supplier overload.
- Every mod jar needs a `pack.mcmeta` (`pack_format` 15). Without one, Forge shows "failed to load a
  valid ResourcePackInfo" on the loading screen. The NeoForge lines don't need one.
- The production jar has only been checked by its refmap. It still needs a run in the
  `1.20.1  Forge` instance.
