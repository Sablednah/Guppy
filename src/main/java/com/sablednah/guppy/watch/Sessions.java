package com.sablednah.guppy.watch;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Extra ticks, grouped by what is giving them: one entry in the log per Time in a Bottle, per
 * Watch of Flowing Time in somebody's inventory, per pedestal or Torcherino, however many blocks
 * it reaches and however many ticks a second it hands out.
 *
 * <p>A session opens on the first extra tick (who, what, where, when: the line an owner greps for
 * after a lag spike), says how it is going every few minutes while it lasts, and closes with a
 * summary once nothing has come from that source for {@code quietSeconds}.</p>
 */
public final class Sessions {

    public enum Kind { BLOCK_ENTITY, RANDOM }

    /** Tuning from the config, set by the loader layer. */
    public record Settings(boolean log, int quietSeconds, int ongoingMinutes, int nearbyRadius, Set<String> ignoredMods) {}

    public static final Logger LOG = LoggerFactory.getLogger("Guppy");

    private static final int POSITION_CAP = 4096;

    public static final class Session {
        public final int id;
        public final boolean silent;
        public final String who;
        /** The player to tell, if any: the one responsible, or failing that the nearest. */
        public final UUID target;
        public final String targetName;
        public final String what;
        public final ResourceKey<Level> level;
        public final BlockPos firstPos;
        public final String where;
        public final String started;
        final long startTick;
        long lastTick;
        long nextOngoingTick;
        long blockEntityTicks;
        long randomTicks;
        final LongOpenHashSet positions = new LongOpenHashSet();
        boolean open = true;

        Session(int id, boolean silent, String who, UUID target, String targetName, String what,
                ResourceKey<Level> level, BlockPos firstPos, String where, long now) {
            this.id = id;
            this.silent = silent;
            this.who = who;
            this.target = target;
            this.targetName = targetName;
            this.what = what;
            this.level = level;
            this.firstPos = firstPos.immutable();
            this.where = where;
            this.started = timestamp();
            this.startTick = now;
            this.lastTick = now;
        }

        public long blockEntityTicks() {
            return blockEntityTicks;
        }

        public long randomTicks() {
            return randomTicks;
        }

        public int blocks() {
            return positions.size();
        }

        public long ageSeconds(long now) {
            return Math.max(0, (now - startTick) / 20);
        }

        public String tally() {
            StringBuilder b = new StringBuilder();
            if (blockEntityTicks > 0) b.append(String.format("%,d extra block-entity ticks", blockEntityTicks));
            if (randomTicks > 0) {
                if (!b.isEmpty()) b.append(", ");
                b.append(String.format("%,d extra random ticks", randomTicks));
            }
            int n = positions.size();
            b.append(" on ").append(n >= POSITION_CAP ? POSITION_CAP + "+" : n).append(n == 1 ? " block" : " blocks");
            return b.toString();
        }
    }

    private record Held(Entity holder, Item item) {}

    private record Unattributed(ResourceKey<Level> level, long chunk) {}

    private static final Map<Object, Session> ACTIVE = new HashMap<>();
    private static Object lastContext;
    private static Session lastSession;
    private static MinecraftServer server;
    private static Settings settings = new Settings(true, 5, 5, 32, Set.of());
    private static Consumer<Session> onStart = s -> {};
    private static int nextId = 1;
    private static int started;
    private static int ended;
    private static boolean failed;

    private Sessions() {}

    public static void configure(Settings s) {
        settings = s;
    }

    public static void onStart(Consumer<Session> listener) {
        onStart = listener;
    }

    public static void serverStarted(MinecraftServer s) {
        server = s;
    }

    /** Closes everything, so a session cut short by a shutdown still gets its summary line. */
    public static void serverStopping() {
        for (Session s : new ArrayList<>(ACTIVE.values())) close(s, "server stopping");
        ACTIVE.clear();
        lastContext = null;
        lastSession = null;
        server = null;
    }

    static void record(Kind kind, ServerLevel level, BlockPos pos) {
        if (server == null || failed) return;
        try {
            long now = server.getTickCount();
            Object context = TickWatch.contextObject();
            Session s;
            if (context != null && context == lastContext && lastSession != null && lastSession.open) {
                s = lastSession;
            } else {
                Object key = keyFor(context, level, pos);
                s = ACTIVE.get(key);
                if (s == null) {
                    s = open(context, level, pos, now);
                    ACTIVE.put(key, s);
                }
                lastContext = context;
                lastSession = s;
            }
            s.lastTick = now;
            if (kind == Kind.BLOCK_ENTITY) s.blockEntityTicks++;
            else s.randomTicks++;
            if (s.positions.size() < POSITION_CAP) s.positions.add(pos.asLong());
        } catch (Throwable t) {
            // Never take a server down over a joke: stop watching and say why, once.
            failed = true;
            LOG.error("Guppy stopped watching for tick acceleration after an error. Please report it.", t);
        }
    }

    private static Object keyFor(Object context, ServerLevel level, BlockPos pos) {
        if (context instanceof ItemStack stack) return new Held(TickWatch.stackHolder, stack.getItem());
        if (context != null) return context;
        return new Unattributed(level.dimension(), ((long) (pos.getX() >> 4) << 32) | ((pos.getZ() >> 4) & 0xFFFFFFFFL));
    }

    private static Session open(Object context, ServerLevel level, BlockPos pos, long now) {
        Attribution.Caller caller = Attribution.caller();
        boolean silent = caller == Attribution.VANILLA || settings.ignoredMods().contains(caller.modId());

        String what;
        ServerPlayer responsible = null;
        Recent.Touch touch = null;
        ResourceKey<Level> dim = level.dimension();
        if (context instanceof ItemStack stack) {
            Entity holder = TickWatch.stackHolder;
            what = "item " + BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (holder instanceof ServerPlayer p) responsible = p;
            else if (holder != null) what += " carried by " + BuiltInRegistries.ENTITY_TYPE.getKey(holder.getType());
        } else if (context instanceof Entity entity) {
            what = "entity " + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (entity instanceof ServerPlayer p) responsible = p;
            else {
                touch = Recent.clickNear(dim, entity.blockPosition(), now);
                if (touch == null) touch = Recent.clickNear(dim, pos, now);
            }
        } else if (context instanceof BlockEntity be) {
            BlockPos at = be.getBlockPos();
            what = "block " + BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock())
                    + " at " + at.getX() + " " + at.getY() + " " + at.getZ();
            touch = Recent.clickNear(dim, at, now);
            if (touch == null) touch = Recent.placedAt(dim, at);
        } else {
            what = "code";
            touch = Recent.clickNear(dim, pos, now);
        }
        if (caller != Attribution.VANILLA) {
            what += " (mod " + caller.modId() + ": " + caller.className() + ")";
        }

        String who;
        UUID target = null;
        String targetName = null;
        if (responsible != null) {
            who = Compat.name(responsible);
            target = responsible.getUUID();
            targetName = who;
        } else if (touch != null) {
            who = touch.name() + " (last to use or place that block, with " + touch.item() + ")";
            target = touch.player();
            targetName = touch.name();
        } else {
            ServerPlayer nearest = nearest(level, pos, settings.nearbyRadius());
            if (nearest != null) {
                int d = (int) Math.sqrt(nearest.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
                targetName = Compat.name(nearest);
                who = "unknown - nearest player " + targetName + ", " + d + " blocks away";
                target = nearest.getUUID();
            } else {
                who = "unknown - no player within " + settings.nearbyRadius() + " blocks";
            }
        }

        String where = Compat.dimension(dim) + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                + ", " + BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());

        Session s = new Session(nextId++, silent, who, target, targetName, what, dim, pos, where, now);
        s.nextOngoingTick = now + settings.ongoingMinutes() * 60L * 20L;
        if (!silent) {
            started++;
            if (settings.log()) {
                LOG.info("Tick acceleration #{} started | who: {} | what: {} | where: {} | when: {}",
                        s.id, s.who, s.what, s.where, s.started);
            }
            onStart.accept(s);
        }
        return s;
    }

    private static ServerPlayer nearest(ServerLevel level, BlockPos pos, int radius) {
        ServerPlayer best = null;
        double bestSq = (double) radius * radius;
        for (ServerPlayer p : level.players()) {
            double d = p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (d <= bestSq) {
                best = p;
                bestSq = d;
            }
        }
        return best;
    }

    /** Once a second: close what has gone quiet, report what is still going. */
    public static void sweep() {
        if (server == null) return;
        long now = server.getTickCount();
        Recent.expire(now);
        long quiet = settings.quietSeconds() * 20L;
        Iterator<Session> it = ACTIVE.values().iterator();
        while (it.hasNext()) {
            Session s = it.next();
            if (now - s.lastTick > quiet) {
                it.remove();
                close(s, null);
            } else if (settings.ongoingMinutes() > 0 && now >= s.nextOngoingTick) {
                s.nextOngoingTick = now + settings.ongoingMinutes() * 60L * 20L;
                if (!s.silent && settings.log()) {
                    LOG.info("Tick acceleration #{} still going after {} | who: {} | what: {} | where: {} | so far: {}",
                            s.id, duration(s.ageSeconds(now)), s.who, s.what, s.where, s.tally());
                }
            }
        }
        if (lastSession != null && !lastSession.open) {
            lastSession = null;
            lastContext = null;
        }
    }

    private static void close(Session s, String reason) {
        s.open = false;
        if (s.silent) return;
        ended++;
        if (!settings.log()) return;
        long seconds = Math.max(1, (s.lastTick - s.startTick) / 20 + 1);
        long total = s.blockEntityTicks + s.randomTicks;
        LOG.info("Tick acceleration #{} {} after {} | who: {} | what: {} | where: {} | {} (about {} a second) | ended: {}",
                s.id, reason == null ? "stopped" : "cut short by " + reason, duration(seconds), s.who, s.what,
                s.where, s.tally(), String.format("%,d", total / seconds), timestamp());
    }

    /** What is going on right now, for {@code /guppy} and the self-test. Silent ones are left out. */
    public static List<Session> active() {
        List<Session> list = new ArrayList<>();
        for (Session s : ACTIVE.values()) if (!s.silent) list.add(s);
        list.sort((a, b) -> Integer.compare(a.id, b.id));
        return list;
    }

    public static long now() {
        return server == null ? 0 : server.getTickCount();
    }

    public static int startedCount() {
        return started;
    }

    public static int endedCount() {
        return ended;
    }

    public static String duration(long seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + (seconds % 3600 / 60) + "m";
    }

    public static String timestamp() {
        return OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }
}
