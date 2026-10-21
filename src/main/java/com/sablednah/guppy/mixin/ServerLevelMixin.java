package com.sablednah.guppy.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sablednah.guppy.watch.TickWatch;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vanilla's random-tick pass, and which entity is ticking (so a Time in a Bottle or a Temporal
 * Pouch is named by its own entity). See {@link TickWatch}.
 */
@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {

    @WrapOperation(method = "tickChunk", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void guppy$vanillaRandomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
            Operation<Void> original) {
        TickWatch.randomPassSeen = true;
        TickWatch.vanillaRandomDepth++;
        try {
            original.call(state, level, pos, random);
        } finally {
            TickWatch.vanillaRandomDepth--;
        }
    }

    @WrapOperation(method = "tickNonPassenger",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void guppy$entityTick(Entity entity, Operation<Void> original) {
        Entity previous = TickWatch.tickingEntity;
        TickWatch.tickingEntity = entity;
        try {
            original.call(entity);
        } finally {
            TickWatch.tickingEntity = previous;
        }
    }

    @WrapOperation(method = "tickPassenger",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private void guppy$passengerTick(Entity entity, Operation<Void> original) {
        Entity previous = TickWatch.tickingEntity;
        TickWatch.tickingEntity = entity;
        try {
            original.call(entity);
        } finally {
            TickWatch.tickingEntity = previous;
        }
    }
}
