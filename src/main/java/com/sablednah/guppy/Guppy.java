package com.sablednah.guppy;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.sablednah.guppy.neoforge.Announcer;
import com.sablednah.guppy.neoforge.GuppyEvents;
import com.sablednah.guppy.watch.Sessions;
import com.sablednah.guppy.watch.TickWatch;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;

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

    public Guppy(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, GuppyConfig.SPEC);
        modEventBus.addListener(Guppy::onConfigLoad);
        modEventBus.addListener(Guppy::onConfigReload);
        NeoForge.EVENT_BUS.register(GuppyEvents.class);
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
        Set<String> ignored = new HashSet<>();
        for (String mod : GuppyConfig.IGNORED_MODS.get()) ignored.add(mod.trim().toLowerCase(Locale.ROOT));
        Sessions.configure(new Sessions.Settings(GuppyConfig.LOG.get(), GuppyConfig.QUIET_SECONDS.get(),
                GuppyConfig.ONGOING_MINUTES.get(), GuppyConfig.NEARBY_RADIUS.get(), Set.copyOf(ignored)));
        GuppyEvents.configure();
    }
}
