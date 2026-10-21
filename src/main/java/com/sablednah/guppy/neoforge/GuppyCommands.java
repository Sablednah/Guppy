package com.sablednah.guppy.neoforge;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.sablednah.guppy.watch.Sessions;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** {@code /guppy}: what is speeding time up right now. Ops only (level 2). */
final class GuppyCommands {

    private GuppyCommands() {}

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("guppy")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> list(ctx.getSource())));
    }

    private static int list(CommandSourceStack source) {
        List<Sessions.Session> active = Sessions.active();
        if (active.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Nothing is speeding time up right now.")
                    .withStyle(ChatFormatting.AQUA), false);
            return 0;
        }
        long now = Sessions.now();
        source.sendSuccess(() -> Component.literal(active.size() + (active.size() == 1 ? " tick accelerator" : " tick accelerators")
                + " running:").withStyle(ChatFormatting.AQUA), false);
        for (Sessions.Session s : active) {
            String line = "#" + s.id + " " + Sessions.duration(s.ageSeconds(now)) + " | " + s.who + " | " + s.what
                    + " | " + s.where + " | " + s.tally();
            source.sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.GRAY), false);
        }
        return active.size();
    }
}
