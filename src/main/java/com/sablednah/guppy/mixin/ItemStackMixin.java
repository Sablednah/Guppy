package com.sablednah.guppy.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sablednah.guppy.watch.TickWatch;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Which item is ticking in whose inventory, so a Watch of Flowing Time is named by the item and its
 * holder rather than by whoever happens to be nearest. See {@link TickWatch}.
 */
@Mixin(ItemStack.class)
abstract class ItemStackMixin {

    @WrapOperation(method = "inventoryTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;inventoryTick(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;IZ)V"))
    private void guppy$itemTick(Item item, ItemStack stack, Level level, Entity holder, int slot, boolean selected,
            Operation<Void> original) {
        if (level.isClientSide()) {
            original.call(item, stack, level, holder, slot, selected);
            return;
        }
        ItemStack previousStack = TickWatch.tickingStack;
        Entity previousHolder = TickWatch.stackHolder;
        TickWatch.tickingStack = stack;
        TickWatch.stackHolder = holder;
        try {
            original.call(item, stack, level, holder, slot, selected);
        } finally {
            TickWatch.tickingStack = previousStack;
            TickWatch.stackHolder = previousHolder;
        }
    }
}
