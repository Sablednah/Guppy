package com.sablednah.guppy.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sablednah.guppy.watch.TickWatch;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Which item is ticking in whose inventory, so a Watch of Flowing Time is named by the item and its
 * holder rather than by whoever happens to be nearest. See {@link TickWatch}.
 */
@Mixin(ItemStack.class)
abstract class ItemStackMixin {

    @WrapOperation(method = "inventoryTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;inventoryTick(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EquipmentSlot;)V"))
    private void guppy$itemTick(Item item, ItemStack stack, ServerLevel level, Entity holder, EquipmentSlot slot,
            Operation<Void> original) {
        ItemStack previousStack = TickWatch.tickingStack;
        Entity previousHolder = TickWatch.stackHolder;
        TickWatch.tickingStack = stack;
        TickWatch.stackHolder = holder;
        try {
            original.call(item, stack, level, holder, slot);
        } finally {
            TickWatch.tickingStack = previousStack;
            TickWatch.stackHolder = previousHolder;
        }
    }
}
