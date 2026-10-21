package com.sablednah.guppy.neoforge;

import java.util.ArrayList;
import java.util.List;

import com.sablednah.guppy.watch.Sessions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * The self-test's second half, run only when real accelerator mods are in {@code run/mods}: their
 * own entities and items, doing what they do, against a furnace each. Nothing here is compiled
 * against them; everything is found by registry id, so a line without them simply skips it.
 *
 * <ul>
 *     <li>Any entity type whose id has "accelerat" in it (Time in a Bottle, Temporal Pouch) is
 *         summoned on a furnace with every NBT key either mod reads.</li>
 *     <li>ProjectE's {@code time_watch} goes, switched on, into a creative fake player's hotbar
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
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
            EXPECTED.add(new Expectation(id, "entity " + id));
        }

        Item watch = item("projecte:time_watch");
        DataComponentType<?> active = component("projecte:active");
        if (watch != null && active != null) {
            level.setBlock(WATCH_FURNACE, Blocks.FURNACE.defaultBlockState(), 3);
            watcher = FakePlayerFactory.getMinecraft(level);
            watcher.setGameMode(GameType.CREATIVE);
            watcher.setPos(WATCH_FURNACE.getX() + 1.5, WATCH_FURNACE.getY(), WATCH_FURNACE.getZ() + 0.5);
            ItemStack stack = new ItemStack(watch);
            set(stack, active, Boolean.TRUE);
            watcher.getInventory().setItem(0, stack);
            EXPECTED.add(new Expectation("projecte:time_watch in a player's hotbar", "item projecte:time_watch"));
        }
        return !EXPECTED.isEmpty();
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

    @SuppressWarnings("unchecked")
    private static <T> void set(ItemStack stack, DataComponentType<?> type, T value) {
        stack.set((DataComponentType<T>) type, value);
    }

    private static Item item(String id) {
        for (Item i : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(i).toString().equals(id)) return i;
        }
        return null;
    }

    private static DataComponentType<?> component(String id) {
        for (DataComponentType<?> t : BuiltInRegistries.DATA_COMPONENT_TYPE) {
            if (id.equals(String.valueOf(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(t)))) return t;
        }
        return null;
    }
}
