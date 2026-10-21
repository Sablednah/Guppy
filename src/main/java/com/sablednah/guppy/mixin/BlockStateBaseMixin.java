package com.sablednah.guppy.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.sablednah.guppy.watch.GuppyTicker;
import com.sablednah.guppy.watch.TickWatch;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The two doors every accelerator goes through: {@code getTicker} (wrapped, server levels only) and
 * {@code randomTick} (checked against vanilla's pass). See {@link TickWatch}.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateBaseMixin {

    @Inject(method = "getTicker", at = @At("RETURN"), cancellable = true)
    private <T extends BlockEntity> void guppy$watchTicker(Level level, BlockEntityType<T> type,
            CallbackInfoReturnable<BlockEntityTicker<T>> cir) {
        BlockEntityTicker<T> ticker = cir.getReturnValue();
        if (ticker != null && !level.isClientSide()) cir.setReturnValue(GuppyTicker.wrap(ticker));
    }

    @Inject(method = "randomTick", at = @At("HEAD"))
    private void guppy$randomTick(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        TickWatch.randomTick(level, pos);
    }
}
