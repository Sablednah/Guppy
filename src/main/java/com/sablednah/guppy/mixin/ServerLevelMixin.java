package com.sablednah.guppy.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.sablednah.guppy.watch.TickWatch;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vanilla's random-tick pass, and which entity is ticking (so a Time in a Bottle or a Temporal
 * Pouch is named by its own entity). See {@link TickWatch}. Plain {@code @Redirect}s on this line.
 */
@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {

    @Redirect(method = "tickChunk", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void guppy$vanillaRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        TickWatch.randomPassSeen = true;
        TickWatch.vanillaRandomDepth++;
        try {
            state.randomTick(level, pos, random);
        } finally {
            TickWatch.vanillaRandomDepth--;
        }
    }

    @Redirect(method = "tickNonPassenger",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void guppy$entityTick(Entity entity) {
        Entity previous = TickWatch.tickingEntity;
        TickWatch.tickingEntity = entity;
        try {
            entity.tick();
        } finally {
            TickWatch.tickingEntity = previous;
        }
    }

    @Redirect(method = "tickPassenger",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private void guppy$passengerTick(Entity entity) {
        Entity previous = TickWatch.tickingEntity;
        TickWatch.tickingEntity = entity;
        try {
            entity.rideTick();
        } finally {
            TickWatch.tickingEntity = previous;
        }
    }
}
