package com.sablednah.guppy.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.sablednah.guppy.watch.TickWatch;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;

/**
 * Marks vanilla's block-entity pass: each ticker it runs is vanilla's own. See {@link TickWatch}.
 * A plain {@code @Redirect} on this line: Forge 1.20.1 does not ship MixinExtras.
 */
@Mixin(Level.class)
abstract class LevelMixin {

    @Redirect(method = "tickBlockEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private void guppy$vanillaBlockEntityTick(TickingBlockEntity ticking) {
        if (((Level) (Object) this).isClientSide()) {
            ticking.tick();
            return;
        }
        TickWatch.vanillaSlot = true;
        try {
            ticking.tick();
        } finally {
            TickWatch.vanillaSlot = false;
        }
    }
}
