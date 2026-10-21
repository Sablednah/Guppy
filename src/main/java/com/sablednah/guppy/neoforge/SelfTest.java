package com.sablednah.guppy.neoforge;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.sablednah.guppy.watch.Exclusions;
import com.sablednah.guppy.watch.GuppyTicker;
import com.sablednah.guppy.watch.Sessions;
import com.sablednah.guppy.watch.TickWatch;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@code ./gradlew runServer -Pselftest}: the only headless proof that vanilla's own ticks are left
 * alone and an accelerator's are caught. It plays the accelerators itself, the three ways the real
 * ones work, then waits for the sessions to close, logs a pass/fail block and stops the server.
 *
 * <p>This class is the one in Guppy that {@code Attribution} does not skip, so what it does is
 * reported as mod {@code guppy-selftest}, the way Time in a Bottle's entity is reported as its mod.</p>
 */
final class SelfTest {

    private static final BlockPos FURNACE = new BlockPos(2, 200, 2);
    private static final BlockPos WHEAT = new BlockPos(5, 200, 5);
    /** Vanilla gets this long to tick the furnace (and must not be reported) before we accelerate. */
    private static final int SETTLE_TICKS = 40;
    /** How long the real accelerators get before their sessions are read. */
    private static final int LIVE_TICKS = 60;

    private static boolean running;
    private static int stage;
    private static long stageAt;
    private static int startedBefore;
    private static int endedBefore;
    private static final List<String> FAILED = new ArrayList<>();
    private static int passed;

    private SelfTest() {}

    static boolean requested() {
        return Boolean.getBoolean("guppy.selftest");
    }

    static void start(MinecraftServer server) {
        ServerLevel level = server.overworld();
        level.setChunkForced(0, 0, true);
        level.setBlock(FURNACE, Blocks.FURNACE.defaultBlockState(), 3);
        level.setBlock(WHEAT.below(), Blocks.FARMLAND.defaultBlockState(), 3);
        level.setBlock(WHEAT, Blocks.WHEAT.defaultBlockState(), 3);
        running = true;
        stage = 0;
        stageAt = server.getTickCount();
        Sessions.LOG.info("=== Guppy self-test started ===");
    }

    static void tick(MinecraftServer server) {
        if (!running) return;
        long now = server.getTickCount();
        try {
            if (stage == 0 && now - stageAt >= SETTLE_TICKS) {
                vanillaLeftAlone();
                accelerate(server.overworld());
                stage = 1;
                stageAt = now;
            } else if (stage == 1 && now - stageAt >= 20L * (com.sablednah.guppy.GuppyConfig.QUIET_SECONDS.get() + 3)) {
                closed();
                exclusions();
                messages();
                if (LiveMods.start(server)) {
                    stage = 2;
                    stageAt = now;
                } else {
                    Sessions.LOG.info("Self-test: no accelerator mods in run/mods, so no live check");
                    finish(server);
                }
            } else if (stage == 2) {
                LiveMods.tick();
                if (now - stageAt >= LIVE_TICKS) {
                    live();
                    LiveMods.stop();
                    finish(server);
                }
            }
        } catch (Throwable t) {
            fail("self-test threw " + t);
            Sessions.LOG.error("Guppy self-test threw", t);
            finish(server);
        }
    }

    private static void vanillaLeftAlone() {
        check("vanilla's block-entity pass was seen (the furnace ticked through our ticker)", TickWatch.blockEntityPassSeen);
        check("vanilla's own ticks opened no session", Sessions.startedCount() == 0 && Sessions.active().isEmpty());
        if (!TickWatch.randomPassSeen) {
            // Random ticks only run in chunks near a player, and a headless server has none. Say so,
            // and switch it on by hand: the randomTick check itself is what is under test.
            Sessions.LOG.info("Self-test: no random-tick pass yet (no player online); enabling it for the test");
            TickWatch.randomPassSeen = true;
        }
        startedBefore = Sessions.startedCount();
        endedBefore = Sessions.endedCount();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void accelerate(ServerLevel level) throws Exception {
        BlockState furnace = level.getBlockState(FURNACE);
        BlockEntity be = level.getBlockEntity(FURNACE);
        check("the furnace has a block entity", be != null);
        if (be == null) return;

        // 1. Time in a Bottle / Temporal Pouch: ask getTicker yourself and call it.
        BlockEntityTicker ticker = furnace.getTicker(level, be.getType());
        check("getTicker hands out our ticker on a server level", ticker instanceof GuppyTicker);
        for (int i = 0; i < 10; i++) ticker.tick(level, FURNACE, furnace, be);

        // 2. ProjectE: read the ticker the chunk bound, and call that.
        BlockEntityTicker bound = boundTicker(level, FURNACE);
        check("the chunk's bound ticker is ours (ProjectE's route)", bound instanceof GuppyTicker);
        if (bound != null) for (int i = 0; i < 5; i++) bound.tick(level, FURNACE, furnace, be);

        // 3. Extra random ticks on a crop.
        BlockState wheat = level.getBlockState(WHEAT);
        for (int i = 0; i < 3; i++) wheat.randomTick(level, WHEAT, level.random);

        List<Sessions.Session> active = Sessions.active();
        check("one session for the unattributed extra ticks", active.size() == 1);
        if (active.size() == 1) {
            Sessions.Session s = active.get(0);
            check("15 extra block-entity ticks counted (got " + s.blockEntityTicks() + ")", s.blockEntityTicks() == 15);
            check("3 extra random ticks counted (got " + s.randomTicks() + ")", s.randomTicks() == 3);
            check("2 blocks touched (got " + s.blocks() + ")", s.blocks() == 2);
            check("named the mod that did it (" + s.what + ")", s.what.contains("mod guppy-selftest"));
            check("said nobody could be named (" + s.who + ")", s.who.startsWith("unknown"));
            check("nobody is messaged when nobody is using it", s.target == null);
        }

        // 4. From an entity's tick, the way the bottle's entity does it: its own session, named by the entity.
        ArmorStand stand = new ArmorStand(level, 2.5, 201, 2.5);
        TickWatch.tickingEntity = stand;
        try {
            for (int i = 0; i < 4; i++) ticker.tick(level, FURNACE, furnace, be);
        } finally {
            TickWatch.tickingEntity = null;
        }
        active = Sessions.active();
        check("an entity's extra ticks get a session of their own", active.size() == 2);
        if (active.size() == 2) {
            Sessions.Session s = active.get(1);
            check("named by the entity (" + s.what + ")", s.what.startsWith("entity minecraft:armor_stand"));
            check("4 ticks on the entity's session (got " + s.blockEntityTicks() + ")", s.blockEntityTicks() == 4);
        }
        check("two sessions started", Sessions.startedCount() - startedBefore == 2);
    }

    /** What ProjectE does: Level.blockEntityTickers, then the wrapper's and the bound ticker's own field. */
    @SuppressWarnings("rawtypes")
    private static BlockEntityTicker boundTicker(ServerLevel level, BlockPos pos) throws Exception {
        Field list = Level.class.getDeclaredField("blockEntityTickers");
        list.setAccessible(true);
        for (Object wrapper : (List<?>) list.get(level)) {
            if (!(wrapper instanceof TickingBlockEntity ticking) || !pos.equals(ticking.getPos())) continue;
            Object bound = field(wrapper, "ticker");
            Object ticker = field(bound, "ticker");
            return ticker instanceof BlockEntityTicker t ? t : null;
        }
        return null;
    }

    private static Object field(Object owner, String name) throws Exception {
        Field f = owner.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(owner);
    }

    private static void closed() {
        check("every session closed once it went quiet", Sessions.active().isEmpty());
        check("both sessions logged an end (got " + (Sessions.endedCount() - endedBefore) + ")",
                Sessions.endedCount() - endedBefore == 2);
    }

    private static void exclusions() {
        Exclusions ex = Exclusions.of(List.of("*sprinkler*", "*twerk*", "torcherino", "*watering_can*"));
        check("exclude: a sprinkler block by id", ex.excludes("cyclic", "com.lothrazar.cyclic.X", "cyclic:sprinkler"));
        check("exclude: a twerk mod by its class", ex.excludes("twerkitmeal", "com.x.TwerkHandler", ""));
        check("exclude: a bare id is that mod only", ex.excludes("torcherino", "a.B", "") && !ex.excludes("torcherinoplus", "a.B", ""));
        check("exclude: a watering can item", ex.excludes("justdirethings", "a.B", "justdirethings:watering_can"));
        check("exclude: leaves the Watch of Flowing Time alone",
                !ex.excludes("projecte", "moze_intel.projecte.gameObjs.items.rings.TimeWatch", "projecte:time_watch"));
        check("exclude: leaves Time in a Bottle alone",
                !ex.excludes("tiab", "org.mangorage.tiab.common.entities.TimeAcceleratorEntity", "tiab:time_accelerator"));
    }

    private static void messages() {
        String coloured = Announcer.format("&6Patience of a {player}", "Guppy", "x").getString();
        check("& codes become colour codes (" + coloured + ")", coloured.equals("§6Patience of a Guppy"));
        check("{what} is the short name", Announcer.thing("item projecte:time_watch (mod projecte: moze_intel.X)")
                .equals("projecte:time_watch"));
        check("{what} of a block drops the position", Announcer.thing("block projecte:dm_pedestal at 1 2 3 (mod projecte: x)")
                .equals("projecte:dm_pedestal"));
    }

    /** Every real accelerator found has a session of its own, named by it, that has counted ticks. */
    private static void live() {
        List<Sessions.Session> active = Sessions.active();
        for (Sessions.Session s : active) {
            Sessions.LOG.info("Self-test (live): #{} who: {} | what: {} | {}", s.id, s.who, s.what, s.tally());
        }
        for (LiveMods.Expectation e : LiveMods.expected()) {
            Sessions.Session found = null;
            for (Sessions.Session s : active) if (s.what.startsWith(e.whatContains())) found = s;
            check("live: " + e.label() + " was caught and named", found != null);
            if (found != null) check("live: " + e.label() + " counted extra ticks (" + found.tally() + ")",
                    found.blockEntityTicks() + found.randomTicks() > 0);
        }
    }

    private static void check(String what, boolean ok) {
        if (ok) {
            passed++;
            Sessions.LOG.info("  PASS {}", what);
        } else {
            fail(what);
        }
    }

    private static void fail(String what) {
        FAILED.add(what);
        Sessions.LOG.error("  FAIL {}", what);
    }

    private static void finish(MinecraftServer server) {
        running = false;
        if (FAILED.isEmpty()) {
            Sessions.LOG.info("=== Guppy self-test PASSED ({} checks) ===", passed);
        } else {
            Sessions.LOG.error("=== Guppy self-test FAILED: {} of {} ===", FAILED.size(), passed + FAILED.size());
            for (String f : FAILED) Sessions.LOG.error("  - {}", f);
        }
        server.halt(false);
    }
}
