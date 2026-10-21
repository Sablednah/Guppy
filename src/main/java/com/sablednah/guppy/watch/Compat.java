package com.sablednah.guppy.watch;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The few names that move between Minecraft lines, in one place, so a port touches this file and
 * the mixins rather than everything that logs a dimension or a player.
 */
public final class Compat {

    private Compat() {}

    public static String dimension(ResourceKey<Level> level) {
        return level.location().toString();
    }

    public static String name(Player player) {
        return player.getGameProfile().getName();
    }
}
