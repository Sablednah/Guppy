package com.sablednah.guppy.neoforge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import com.sablednah.guppy.GuppyConfig;
import com.sablednah.guppy.watch.Sessions;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Tells the player. The joke half of the mod: a line from the configured pool, in chat and on the
 * action bar, to the audience the owner chose, and a call-out to everyone else. The cooldown is per
 * impatient player, so one bottle does not become a chat flood.
 */
public final class Announcer {

    private static final Pattern CODE = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    private static final Map<UUID, Long> LAST_TOLD = new HashMap<>();

    private Announcer() {}

    public static void announce(Sessions.Session s) {
        // Only ever about somebody using it right now (see Session.target): never a guess.
        if (s.target == null) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        long now = Sessions.now();
        Long last = LAST_TOLD.get(s.target);
        if (last != null && now - last < GuppyConfig.COOLDOWN.get() * 20L) return;

        Set<UUID> told = new HashSet<>();
        tell(server, s, told);
        callOut(server, s, told);
        if (!told.isEmpty()) LAST_TOLD.put(s.target, now);
    }

    /** The message itself, to the configured audience. */
    private static void tell(MinecraftServer server, Sessions.Session s, Set<UUID> told) {
        GuppyConfig.Audience audience = GuppyConfig.AUDIENCE.get();
        boolean chat = GuppyConfig.CHAT.get();
        boolean bar = GuppyConfig.ACTION_BAR.get();
        if (audience == GuppyConfig.Audience.NONE || (!chat && !bar)) return;
        String line = pick(GuppyConfig.MESSAGES.get());
        if (line == null) return;
        Component message = format(line, s.targetName, thing(s.what));
        for (ServerPlayer p : audience(server, s, audience)) {
            if (chat) p.sendSystemMessage(message);
            if (bar) p.sendSystemMessage(message, true);
            told.add(p.getUUID());
        }
    }

    /** The call-out, in chat, to every player who was not already told and is not the one using it. */
    private static void callOut(MinecraftServer server, Sessions.Session s, Set<UUID> told) {
        if (!GuppyConfig.BROADCAST.get()) return;
        String line = pick(GuppyConfig.BROADCAST_MESSAGES.get());
        if (line == null) return;
        Component message = format(line, s.targetName, thing(s.what));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.getUUID().equals(s.target) || told.contains(p.getUUID())) continue;
            p.sendSystemMessage(message);
            told.add(p.getUUID());
        }
    }

    private static List<ServerPlayer> audience(MinecraftServer server, Sessions.Session s, GuppyConfig.Audience audience) {
        List<ServerPlayer> to = new ArrayList<>();
        switch (audience) {
            case PLAYER -> {
                ServerPlayer p = server.getPlayerList().getPlayer(s.target);
                if (p != null) to.add(p);
            }
            case NEARBY -> {
                ServerLevel level = server.getLevel(s.level);
                if (level == null) break;
                double r = GuppyConfig.NEARBY_RADIUS.get();
                for (ServerPlayer p : level.players()) {
                    if (p.distanceToSqr(s.firstPos.getX() + 0.5, s.firstPos.getY() + 0.5, s.firstPos.getZ() + 0.5) <= r * r) to.add(p);
                }
            }
            case EVERYONE -> to.addAll(server.getPlayerList().getPlayers());
            case NONE -> { }
        }
        return to;
    }

    private static String pick(List<? extends String> configured) {
        List<? extends String> pool = configured.stream().filter(m -> !m.isBlank()).toList();
        if (pool.isEmpty()) return null;
        if (!GuppyConfig.RANDOM.get()) return pool.get(0);
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    /** {@code item projecte:watch_of_flowing_time (mod ...)} -> {@code projecte:watch_of_flowing_time}. */
    static String thing(String what) {
        String t = what;
        int mod = t.indexOf(" (mod ");
        if (mod >= 0) t = t.substring(0, mod);
        int space = t.indexOf(' ');
        if (space >= 0 && (t.startsWith("item ") || t.startsWith("entity ") || t.startsWith("block "))) t = t.substring(space + 1);
        int at = t.indexOf(" at ");
        if (at >= 0) t = t.substring(0, at);
        return t;
    }

    /** Fills the placeholders and turns {@code &} codes into colour; plain text is shown in aqua. */
    public static Component format(String line, String player, String what) {
        String text = line.replace("{player}", player).replace("{what}", what);
        if (CODE.matcher(text).find()) {
            return Component.literal(CODE.matcher(text).replaceAll("§$1"));
        }
        MutableComponent plain = Component.literal(text);
        return plain.withStyle(ChatFormatting.AQUA);
    }

    public static void clear() {
        LAST_TOLD.clear();
    }
}
