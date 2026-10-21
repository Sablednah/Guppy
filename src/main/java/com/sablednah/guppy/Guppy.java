package com.sablednah.guppy;

import com.sablednah.guppy.neoforge.Announcer;
import com.sablednah.guppy.neoforge.GuppyEvents;
import com.sablednah.guppy.watch.Exclusions;
import com.sablednah.guppy.watch.Sessions;
import com.sablednah.guppy.watch.TickWatch;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Guppy: for players with the patience of a guppy.
 *
 * <p>After X33n's rant in SkyFactory ("you have the patience of a guppy"). Whenever something hands
 * out extra ticks (a Time in a Bottle, a Watch of Flowing Time, a Temporal Pouch, a Torcherino or
 * anything else built the same way) the player is told so, and the server log records who, what,
 * where and when, which is the part an owner actually wants after a lag spike.</p>
 *
 * <p>Detection is of the ticks themselves, not of any particular item: see
 * {@link com.sablednah.guppy.watch.TickWatch}. Server-side; a client only needs it for the splash.</p>
 */
@Mod(Guppy.MODID)
public final class Guppy {

    public static final String MODID = "guppy";

    public Guppy() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GuppyConfig.SPEC);
        modEventBus.addListener(Guppy::onConfigLoad);
        modEventBus.addListener(Guppy::onConfigReload);
        MinecraftForge.EVENT_BUS.register(GuppyEvents.class);
        Sessions.onStart(Announcer::announce);

        // Which build, not just which release. Same format across the family of mods.
        Sessions.LOG.info("Guppy {}", BuildInfo.describe());
    }

    private static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == GuppyConfig.SPEC) apply();
    }

    private static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == GuppyConfig.SPEC) apply();
    }

    private static void apply() {
        TickWatch.configure(GuppyConfig.BLOCK_ENTITIES.get(), GuppyConfig.RANDOM_TICKS.get());
        Sessions.configure(new Sessions.Settings(GuppyConfig.LOG.get(), GuppyConfig.QUIET_SECONDS.get(),
                GuppyConfig.ONGOING_MINUTES.get(), GuppyConfig.NEARBY_RADIUS.get(),
                Exclusions.of(GuppyConfig.EXCLUDE.get())));
        GuppyEvents.configure();
    }
}
