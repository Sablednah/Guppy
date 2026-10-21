package com.sablednah.guppy.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sablednah.guppy.watch.TickWatch;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;

/** Marks vanilla's block-entity pass: each ticker it runs is vanilla's own. See {@link TickWatch}. */
@Mixin(Level.class)
abstract class LevelMixin {

    @WrapOperation(method = "tickBlockEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private void guppy$vanillaBlockEntityTick(TickingBlockEntity ticking, Operation<Void> original) {
        if (((Level) (Object) this).isClientSide()) {
            original.call(ticking);
            return;
        }
        TickWatch.vanillaSlot = true;
        try {
            original.call(ticking);
        } finally {
            TickWatch.vanillaSlot = false;
        }
    }
}
