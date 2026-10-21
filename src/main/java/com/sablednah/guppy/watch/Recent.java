package com.sablednah.guppy.watch;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Who last touched a block: the clue for an accelerator that does not say who started it.
 *
 * <p>A Time in a Bottle or a Temporal Pouch spawns an entity with no owner on the block the player
 * clicked; a Watch of Flowing Time goes into a pedestal the player clicked; a Torcherino is placed.
 * So right-clicks are remembered briefly and placements for longer, and both are capped.</p>
 */
public final class Recent {

    public record Touch(UUID player, String name, String item, long tick) {}

    private record Key(ResourceKey<Level> level, long pos) {}

    /** A click starts an accelerator at once; ten seconds is generous. */
    private static final long CLICK_TICKS = 20L * 10;
    private static final int CAP = 4096;

    private static final Map<Key, Touch> CLICKS = new LinkedHashMap<>();
    private static final Map<Key, Touch> PLACED = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Key, Touch> eldest) {
            return size() > CAP;
        }
    };

    private Recent() {}

    public static void clicked(ResourceKey<Level> level, BlockPos pos, Touch touch) {
        Key key = new Key(level, pos.asLong());
        CLICKS.remove(key);
        CLICKS.put(key, touch);
        if (CLICKS.size() > CAP) {
            Iterator<Key> it = CLICKS.keySet().iterator();
            it.next();
            it.remove();
        }
    }

    public static void placed(ResourceKey<Level> level, BlockPos pos, Touch touch) {
        PLACED.put(new Key(level, pos.asLong()), touch);
    }

    /** The latest click on this block, or on one beside it, within the last few seconds. */
    static Touch clickNear(ResourceKey<Level> level, BlockPos pos, long now) {
        Touch best = null;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Touch t = CLICKS.get(new Key(level, BlockPos.asLong(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz)));
                    if (t != null && now - t.tick() <= CLICK_TICKS && (best == null || t.tick() > best.tick())) best = t;
                }
            }
        }
        return best;
    }

    static Touch placedAt(ResourceKey<Level> level, BlockPos pos) {
        return PLACED.get(new Key(level, pos.asLong()));
    }

    /** Drops clicks too old to matter. Placements are only ever capped. */
    public static void expire(long now) {
        CLICKS.values().removeIf(t -> now - t.tick() > CLICK_TICKS);
    }

    public static void clear() {
        CLICKS.clear();
        PLACED.clear();
    }
}
