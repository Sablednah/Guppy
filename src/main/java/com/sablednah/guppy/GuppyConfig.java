package com.sablednah.guppy;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Everything an owner can change, in {@code config/guppy-common.toml}. Edits apply on save.
 */
public final class GuppyConfig {

    /** Who sees the message when somebody speeds time up. */
    public enum Audience {
        /** The player responsible, or the nearest player when nobody can be named. */
        PLAYER,
        /** Every player within {@code nearbyRadius} blocks. */
        NEARBY,
        /** The whole server. */
        EVERYONE,
        /** Nobody: the server log only. */
        NONE
    }

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<List<? extends String>> MESSAGES;
    public static final ModConfigSpec.BooleanValue RANDOM;
    public static final ModConfigSpec.EnumValue<Audience> AUDIENCE;
    public static final ModConfigSpec.BooleanValue CHAT;
    public static final ModConfigSpec.BooleanValue ACTION_BAR;
    public static final ModConfigSpec.IntValue NEARBY_RADIUS;
    public static final ModConfigSpec.IntValue COOLDOWN;

    public static final ModConfigSpec.BooleanValue LOG;
    public static final ModConfigSpec.IntValue QUIET_SECONDS;
    public static final ModConfigSpec.IntValue ONGOING_MINUTES;
    public static final ModConfigSpec.BooleanValue LOG_COMMANDS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> COMMANDS;

    public static final ModConfigSpec.BooleanValue BLOCK_ENTITIES;
    public static final ModConfigSpec.BooleanValue RANDOM_TICKS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> IGNORED_MODS;

    public static final ModConfigSpec SPEC;

    static {
        B.comment("What the player is told when they speed time up.").push("messages");
        MESSAGES = B.comment(
                        "The message, or a pool of messages to pick from.",
                        "  {player} - the player's name    {what} - the item, entity or block doing it",
                        "  &-codes colour it (&6 gold, &b aqua, &o italic, ...). Without any, it is shown in aqua.")
                .defineListAllowEmpty("messages", List.of("Patience of a Guppy", "Captain Guppy!"),
                        () -> "Patience of a Guppy", o -> o instanceof String);
        RANDOM = B.comment("Pick a message from the pool at random. When false, the first one is always used.")
                .define("random", true);
        AUDIENCE = B.comment(
                        "Who sees it.",
                        "  PLAYER   - the player responsible (the nearest player when nobody can be named)",
                        "  NEARBY   - every player within nearbyRadius blocks",
                        "  EVERYONE - the whole server",
                        "  NONE     - nobody: the server log only")
                .defineEnum("audience", Audience.PLAYER);
        CHAT = B.comment("Show it in chat.").define("chat", true);
        ACTION_BAR = B.comment("Show it on the action bar, above the hotbar.").define("actionBar", true);
        NEARBY_RADIUS = B.comment("How far 'nearby' and 'nearest player' reach, in blocks.")
                .defineInRange("nearbyRadius", 32, 1, 512);
        COOLDOWN = B.comment("Seconds before the same player is told again.")
                .defineInRange("cooldownSeconds", 60, 0, 86400);
        B.pop();

        B.comment("The server log: the useful bit.").push("log");
        LOG = B.comment("Log every tick acceleration: who, what, where and when, and a summary when it stops.")
                .define("enabled", true);
        QUIET_SECONDS = B.comment("Seconds without an extra tick before an acceleration counts as stopped.")
                .defineInRange("quietSeconds", 5, 1, 3600);
        ONGOING_MINUTES = B.comment("While one keeps going, log a progress line this often, in minutes. 0 = never.")
                .defineInRange("ongoingMinutes", 5, 0, 1440);
        LOG_COMMANDS = B.comment("Also log the commands below, which change the tick rate for everyone.")
                .define("logCommands", true);
        COMMANDS = B.comment("Command prefixes to log (without the slash, any case).")
                .defineListAllowEmpty("commands",
                        List.of("tick rate", "tick sprint", "tick step", "tick freeze", "tick unfreeze",
                                "gamerule randomTickSpeed", "gamerule random_tick_speed"),
                        () -> "tick rate", o -> o instanceof String);
        B.pop();

        B.comment("What counts.").push("detection");
        BLOCK_ENTITIES = B.comment("Extra ticks given to block entities (furnaces, machines, spawners...).")
                .define("blockEntities", true);
        RANDOM_TICKS = B.comment("Extra random ticks given to blocks (crops, saplings, sugar cane...).")
                .define("randomTicks", true);
        IGNORED_MODS = B.comment("Mod ids whose extra ticks are not reported at all, e.g. [\"torcherino\"].")
                .defineListAllowEmpty("ignoredMods", List.of(), () -> "modid", o -> o instanceof String);
        B.pop();

        SPEC = B.build();
    }

    private GuppyConfig() {}
}
