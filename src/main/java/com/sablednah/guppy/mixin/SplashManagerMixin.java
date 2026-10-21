package com.sablednah.guppy.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.resources.SplashManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * The title-screen splash. NeoForge merges every mod's {@code texts/splashes.txt}; Forge 1.20.1
 * reads vanilla's alone, so the line is added to the list vanilla loads. The returned list is
 * copied rather than added to, because a failed read returns an immutable empty one, and there is
 * no {@code @Shadow} field to go wrong under reobfuscation.
 */
@Mixin(SplashManager.class)
abstract class SplashManagerMixin {

    @Inject(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private void guppy$addSplash(ResourceManager manager, ProfilerFiller profiler, CallbackInfoReturnable<List<String>> cir) {
        List<String> splashes = new ArrayList<>(cir.getReturnValue());
        splashes.add("Patience of a Guppy");
        cir.setReturnValue(splashes);
    }
}
