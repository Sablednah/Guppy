package com.sablednah.guppy;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Everything an owner can change, in {@code config/guppy-common.toml}. Edits apply on save.
 */
public final class GuppyConfig {

    /** Who sees the message when somebody speeds time up. */
    public enum Audience {
        /** The player using it. */
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

    public static final ModConfigSpec.BooleanValue BROADCAST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BROADCAST_MESSAGES;

    public static final ModConfigSpec.BooleanValue LOG;
    public static final ModConfigSpec.IntValue QUIET_SECONDS;
    public static final ModConfigSpec.IntValue ONGOING_MINUTES;
    public static final ModConfigSpec.BooleanValue LOG_COMMANDS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> COMMANDS;

    public static final ModConfigSpec.BooleanValue BLOCK_ENTITIES;
    public static final ModConfigSpec.BooleanValue RANDOM_TICKS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXCLUDE;

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
                        "Who sees it. A message is only ever sent while somebody is using the thing: holding the item,",
                        "or having just clicked the block. A pedestal or Torcherino placed earlier, or one nobody can",
                        "be named for, goes in the log and tells nobody.",
                        "  PLAYER   - the player using it",
                        "  NEARBY   - every player within nearbyRadius blocks of it",
                        "  EVERYONE - the whole server",
                        "  NONE     - nobody: the server log only")
                .defineEnum("audience", Audience.PLAYER);
        CHAT = B.comment("Show it in chat.").define("chat", true);
        ACTION_BAR = B.comment("Show it on the action bar, above the hotbar.").define("actionBar", true);
        NEARBY_RADIUS = B.comment("How far NEARBY reaches, and how far the log looks for a nearest player, in blocks.")
                .defineInRange("nearbyRadius", 32, 1, 512);
        COOLDOWN = B.comment("Seconds before the same player is called out again, by either message.")
                .defineInRange("cooldownSeconds", 60, 0, 86400);
        B.pop();

        B.comment("Everyone else is told too: the fun is being called out for being impatient.").push("broadcast");
        BROADCAST = B.comment("Tell every other player on the server, in chat.",
                        "Only ever about somebody using it right now, like the message above.")
                .define("enabled", true);
        BROADCAST_MESSAGES = B.comment(
                        "The call-out, or a pool to pick from. {player}, {what} and &-codes work as above.",
                        "The random setting above applies here too.")
                .defineListAllowEmpty("messages", List.of("{player} has the patience of a guppy", "{player} is Captain Guppy!"),
                        () -> "{player} has the patience of a guppy",
                        o -> o instanceof String);
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
        EXCLUDE = B.comment(
                        "Never reported, logged or messaged: things that speed up growth but nobody would call impatient.",
                        "Globs (* = anything, any case), each tried against the mod id, the id of the item, entity or",
                        "block doing it, and the class asking for the tick. \"torcherino\" with no star is that mod only.")
                .defineListAllowEmpty("exclude", List.of(
                        "*sprinkler*", "*watering_can*", "*wateringcan*", "*twerk*",
                        "*growth_accelerator*", "*growthaccelerator*", "*growth_crystal*", "*fertiliz*"),
                        () -> "*sprinkler*", o -> o instanceof String);
        B.pop();

        SPEC = B.build();
    }

    private GuppyConfig() {}
}
