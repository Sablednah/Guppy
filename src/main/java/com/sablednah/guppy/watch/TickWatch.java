package com.sablednah.guppy.watch;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Tells vanilla's own ticks apart from everybody else's.
 *
 * <p>Every tick accelerator we have looked at works the same way, whatever it looks like in game:</p>
 * <ul>
 *     <li><b>Time in a Bottle</b> and <b>Gadgets Against Grind's Temporal Pouch</b> spawn an entity on
 *         the block, which each tick calls {@code state.getTicker(level, type).tick(...)} several times
 *         and {@code state.randomTick(...)} now and then.</li>
 *     <li><b>ProjectE's Watch of Flowing Time</b> (from a player's inventory, or in a DM Pedestal)
 *         reaches into the chunk's bound ticker and calls its {@code BlockEntityTicker} directly, and
 *         calls {@code randomTick} on the blocks around it.</li>
 *     <li>Torcherino-style blocks do the same from their own block entity's tick.</li>
 * </ul>
 *
 * <p>So we do not look for items at all. Vanilla calls a block-entity ticker from exactly one place
 * ({@code Level.tickBlockEntities}, through the chunk's bound ticker) and {@code randomTick} from
 * exactly one place ({@code ServerLevel.tickChunk}). The mixins mark those two passes; any tick that
 * arrives outside them is an extra tick, and {@link Sessions} works out who and what.</p>
 *
 * <p><b>Block entities.</b> {@code getTicker} is wrapped ({@link GuppyTicker}) for server levels, so
 * the chunk's bound ticker is ours and so is the one an accelerator asks for itself. Vanilla's pass
 * sets {@link #vanillaSlot} around each {@code TickingBlockEntity.tick()}; the first wrapped ticker
 * to run inside it takes the slot and is the vanilla tick. Anything else, including a ticker called
 * from <em>inside</em> another block entity's vanilla tick (a pedestal, a Torcherino), is extra.</p>
 *
 * <p>Everything here runs on the server thread and is plain static state: the hot path is a field
 * read per tick. A call from any other thread is ignored rather than trusted.</p>
 *
 * <p><b>If a mixin fails to apply</b> (another mod rewrote the same method) the matching pass is
 * never seen, and {@link #blockEntityPassSeen} / {@link #randomPassSeen} keep detection of that kind
 * switched off rather than reporting every vanilla tick as extra. The server log says so.</p>
 */
public final class TickWatch {

    /** Set by vanilla's block-entity pass around each ticker it runs; taken by the first {@link GuppyTicker}. */
    public static boolean vanillaSlot;
    /** The block entity vanilla is ticking right now, or null: the source of anything it ticks in turn. */
    public static BlockEntity vanillaTicking;
    /** How deep we are inside vanilla's random-tick pass. */
    public static int vanillaRandomDepth;

    public static boolean blockEntityPassSeen;
    public static boolean randomPassSeen;

    /** From the config's detection section. */
    private static boolean watchBlockEntities = true;
    private static boolean watchRandomTicks = true;

    /** The entity whose own tick is running, if any. */
    public static Entity tickingEntity;
    /** The item stack whose inventory tick is running, if any, and who holds it. */
    public static ItemStack tickingStack;
    public static Entity stackHolder;

    private static volatile Thread serverThread;

    private TickWatch() {}

    public static void serverStarting(Thread thread) {
        serverThread = thread;
        reset();
    }

    public static void serverStopped() {
        serverThread = null;
        reset();
    }

    private static void reset() {
        vanillaSlot = false;
        vanillaTicking = null;
        vanillaRandomDepth = 0;
        blockEntityPassSeen = false;
        randomPassSeen = false;
        tickingEntity = null;
        tickingStack = null;
        stackHolder = null;
    }

    public static void configure(boolean blockEntities, boolean randomTicks) {
        watchBlockEntities = blockEntities;
        watchRandomTicks = randomTicks;
    }

    static boolean onServerThread() {
        return Thread.currentThread() == serverThread;
    }

    /** A block entity was ticked outside vanilla's pass. */
    static void extraBlockEntityTick(Level level, BlockPos pos) {
        if (!blockEntityPassSeen || !watchBlockEntities || !(level instanceof ServerLevel server) || !onServerThread()) return;
        Sessions.record(Sessions.Kind.BLOCK_ENTITY, server, pos);
    }

    /** Called at the head of every {@code BlockState.randomTick}. */
    public static void randomTick(ServerLevel level, BlockPos pos) {
        if (vanillaRandomDepth > 0 || !randomPassSeen || !watchRandomTicks || !onServerThread()) return;
        Sessions.record(Sessions.Kind.RANDOM, level, pos);
    }

    /** What the extra tick is coming from, for caching: the innermost thing being ticked. */
    static Object contextObject() {
        if (vanillaTicking != null) return vanillaTicking;
        if (tickingStack != null) return tickingStack;
        return tickingEntity;
    }
}
