package com.sablednah.guppy.watch;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The ticker a server level hands out for every ticking block entity: the real one, plus a check
 * of whether this call is vanilla's own (see {@link TickWatch}).
 *
 * <p>Vanilla binds it into the chunk, so an accelerator that reads the chunk's ticker (ProjectE) gets
 * this; one that asks {@code getTicker} itself (Time in a Bottle, Temporal Pouch) gets a fresh one.
 * Either way the call comes through here.</p>
 */
public final class GuppyTicker<T extends BlockEntity> implements BlockEntityTicker<T> {

    private final BlockEntityTicker<T> delegate;

    private GuppyTicker(BlockEntityTicker<T> delegate) {
        this.delegate = delegate;
    }

    public static <T extends BlockEntity> BlockEntityTicker<T> wrap(BlockEntityTicker<T> ticker) {
        return ticker instanceof GuppyTicker<T> ? ticker : new GuppyTicker<>(ticker);
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, T blockEntity) {
        if (TickWatch.vanillaSlot) {
            // Vanilla's pass: take the slot, so a ticker this block entity calls in turn is extra.
            TickWatch.vanillaSlot = false;
            TickWatch.blockEntityPassSeen = true;
            BlockEntity previous = TickWatch.vanillaTicking;
            TickWatch.vanillaTicking = blockEntity;
            try {
                delegate.tick(level, pos, state, blockEntity);
            } finally {
                TickWatch.vanillaTicking = previous;
            }
            return;
        }
        TickWatch.extraBlockEntityTick(level, pos);
        delegate.tick(level, pos, state, blockEntity);
    }
}
