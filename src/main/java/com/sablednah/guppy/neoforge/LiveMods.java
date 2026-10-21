package com.sablednah.guppy.neoforge;

import java.util.ArrayList;
import java.util.List;

import com.sablednah.guppy.watch.Sessions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;

/**
 * The self-test's second half, run only when real accelerator mods are in {@code run/mods}: their
 * own entities and items, doing what they do, against a furnace each. Nothing here is compiled
 * against them; everything is found by registry id, so a line without them simply skips it.
 *
 * <ul>
 *     <li>Any entity type whose id has "accelerat" in it (Time in a Bottle, Temporal Pouch) is
 *         summoned on a furnace with every NBT key either mod reads.</li>
 *     <li>ProjectE's {@code watch_of_flowing_time} goes, switched on, into a creative fake player's hotbar
 *         beside a furnace, and that inventory is ticked the way a player's is.</li>
 * </ul>
 */
final class LiveMods {

    /** What we expect to see in a session's "what", one per accelerator found. */
    record Expectation(String label, String whatContains) {}

    private static final BlockPos WATCH_FURNACE = new BlockPos(20, 200, 4);

    private static final List<Expectation> EXPECTED = new ArrayList<>();
    private static FakePlayer watcher;

    private LiveMods() {}

    /** Sets everything up; returns false if there is nothing to test. */
    static boolean start(MinecraftServer server) {
        EXPECTED.clear();
        watcher = null;
        ServerLevel level = server.overworld();
        level.setChunkForced(1, 0, true);
        int x = 8;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            if (id.startsWith("minecraft:") || !id.contains("accelerat")) continue;
            BlockPos furnace = new BlockPos(x, 200, 10);
            x += 3;
            level.setBlock(furnace, Blocks.FURNACE.defaultBlockState(), 3);
            String nbt = "{timesAccelerated:2,ticksRemaining:400,timeRate:4,remainingTime:400,position:[I;"
                    + furnace.getX() + "," + furnace.getY() + "," + furnace.getZ() + "]}";
            String command = "summon " + id + " " + (furnace.getX() + 0.5) + " " + furnace.getY() + " "
                    + (furnace.getZ() + 0.5) + " " + nbt;
            Sessions.LOG.info("Self-test (live): /{}", command);
            int before = level.getEntitiesOfClass(Entity.class, new AABB(furnace)).size();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
            if (level.getEntitiesOfClass(Entity.class, new AABB(furnace)).size() == before) {
                // GAG's is not summonable: make it the way its pouch does.
                if (!spawn(level, type, furnace)) {
                    Sessions.LOG.warn("Self-test (live): could not spawn {}", id);
                    continue;
                }
            }
            EXPECTED.add(new Expectation(id, "entity " + id));
        }

        // ProjectE's watch is not tried on this line: 1.20.1 has no item components to switch it on
        // with, and its NBT form is ProjectE's own business. Its route (the chunk's bound ticker)
        // is covered by the self-test's first half.
        return !EXPECTED.isEmpty();
    }

    /**
     * Creates the entity directly and sets it going through its own setters, by name, the way its
     * item would. {@code EntityType.create} is the part that moves between lines.
     */
    private static boolean spawn(ServerLevel level, EntityType<?> type, BlockPos at) {
        Entity entity = type.create(level);
        if (entity == null) return false;
        entity.setPos(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        for (String setter : new String[] {"setTimesAccelerated:2", "setTicksRemaining:400", "setTimeRate:4", "setRemainingTime:400"}) {
            String[] nv = setter.split(":");
            try {
                entity.getClass().getMethod(nv[0], int.class).invoke(entity, Integer.parseInt(nv[1]));
            } catch (ReflectiveOperationException ignored) {
                // Not this mod's setter.
            }
        }
        return level.addFreshEntity(entity);
    }

    /** A player's inventory ticks from its connection, which a fake player has none of. */
    static void tick() {
        if (watcher != null) watcher.getInventory().tick();
    }

    static List<Expectation> expected() {
        return EXPECTED;
    }

    static void stop() {
        if (watcher != null) watcher.getInventory().clearContent();
        watcher = null;
    }
}
