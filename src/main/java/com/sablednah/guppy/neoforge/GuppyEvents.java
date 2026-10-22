package com.sablednah.guppy.neoforge;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.sablednah.guppy.GuppyConfig;
import com.sablednah.guppy.watch.Compat;
import com.sablednah.guppy.watch.Recent;
import com.sablednah.guppy.watch.Sessions;
import com.sablednah.guppy.watch.TickWatch;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** The game-bus side: server lifecycle, the once-a-second sweep, who touched what, and commands. */
public final class GuppyEvents {

    /** A minute of play with somebody online, then check both vanilla passes have been seen. */
    private static final int HEALTH_CHECK_TICKS = 20 * 60;

    private static List<String> commandPrefixes = List.of();
    private static int ticksWithPlayers;
    private static boolean healthChecked;

    private GuppyEvents() {}

    public static void configure() {
        List<String> prefixes = new ArrayList<>();
        for (String c : GuppyConfig.COMMANDS.get()) {
            String p = normalise(c);
            if (!p.isEmpty()) prefixes.add(p);
        }
        commandPrefixes = List.copyOf(prefixes);
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        TickWatch.serverStarting(event.getServer().getRunningThread());
        ticksWithPlayers = 0;
        healthChecked = false;
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        Sessions.serverStarted(event.getServer());
        if (SelfTest.requested()) SelfTest.start(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        Sessions.serverStopping();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TickWatch.serverStopped();
        Recent.clear();
        Announcer.clear();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 == 0) Sessions.sweep();
        if (!healthChecked && server.getPlayerCount() > 0 && ++ticksWithPlayers >= HEALTH_CHECK_TICKS) {
            healthChecked = true;
            healthCheck();
        }
        SelfTest.tick(server);
    }

    /**
     * A mixin another mod has beaten to the same method leaves that pass unseen, and detection of
     * that kind off. Say so once, rather than leave an owner trusting a silent log.
     */
    private static void healthCheck() {
        if (!TickWatch.blockEntityPassSeen) {
            Sessions.LOG.warn("Guppy has not seen vanilla's block-entity pass after a minute of play, so it is not "
                    + "watching block-entity acceleration. Another mod may have changed Level.tickBlockEntities.");
        }
        if (!TickWatch.randomPassSeen) {
            Sessions.LOG.warn("Guppy has not seen vanilla's random-tick pass after a minute of play, so it is not "
                    + "watching random-tick acceleration. Is randomTickSpeed 0, or has another mod changed ServerLevel.tickChunk?");
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack held = event.getItemStack();
        String item = held.isEmpty() ? Recent.EMPTY_HAND : BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
        Recent.clicked(player.level().dimension(), event.getPos(),
                new Recent.Touch(player.getUUID(), Compat.name(player), item, Sessions.now()));
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        String block = BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock()).toString();
        Recent.placed(level.dimension(), event.getPos(),
                new Recent.Touch(player.getUUID(), Compat.name(player), block, Sessions.now()));
    }

    /** {@code /tick rate 100} and friends speed up the whole server; that belongs in the same log. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCommand(CommandEvent event) {
        if (event.isCanceled() || !GuppyConfig.LOG_COMMANDS.get() || commandPrefixes.isEmpty()) return;
        String command = normalise(event.getParseResults().getReader().getString());
        boolean watched = false;
        for (String prefix : commandPrefixes) {
            if (command.equals(prefix) || command.startsWith(prefix + " ")) {
                watched = true;
                break;
            }
        }
        if (!watched) return;
        CommandSourceStack source = event.getParseResults().getContext().getSource();
        Vec3 at = source.getPosition();
        Sessions.LOG.info("Tick command | who: {} | command: /{} | where: {} {} {} {} | when: {}",
                source.getTextName(), event.getParseResults().getReader().getString().replaceFirst("^/", ""),
                Compat.dimension(source.getLevel().dimension()), (int) Math.floor(at.x), (int) Math.floor(at.y),
                (int) Math.floor(at.z), Sessions.timestamp());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        GuppyCommands.register(event.getDispatcher());
    }

    private static String normalise(String command) {
        String c = command.trim();
        if (c.startsWith("/")) c = c.substring(1);
        return c.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
