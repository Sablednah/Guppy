package com.sablednah.guppy.watch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * What is never reported: sprinklers, watering cans, twerking, growth accelerators and the like.
 * They hand out extra ticks too, but nobody calls a watering can impatient.
 *
 * <p>Each pattern is a glob ({@code *} for anything, case ignored) tried against the mod id, the id
 * of the item, entity or block doing it, and the class that asked for the tick. So
 * {@code *sprinkler*} catches {@code cyclic:sprinkler} and a {@code SprinklerBlockEntity} alike, and
 * {@code torcherino} (no star) is that mod and nothing else.</p>
 */
public final class Exclusions {

    public static final Exclusions NONE = new Exclusions(List.of());

    private final List<Pattern> patterns;

    private Exclusions(List<Pattern> patterns) {
        this.patterns = patterns;
    }

    public static Exclusions of(List<? extends String> globs) {
        List<Pattern> list = new ArrayList<>();
        for (String g : globs) {
            String glob = g.trim().toLowerCase(Locale.ROOT);
            if (glob.isEmpty()) continue;
            StringBuilder re = new StringBuilder();
            for (String part : glob.split("\\*", -1)) {
                if (!re.isEmpty()) re.append(".*");
                re.append(Pattern.quote(part));
            }
            list.add(Pattern.compile(re.toString()));
        }
        return new Exclusions(List.copyOf(list));
    }

    public boolean excludes(String modId, String className, String thing) {
        for (Pattern p : patterns) {
            if (matches(p, modId) || matches(p, className) || matches(p, thing)) return true;
        }
        return false;
    }

    private static boolean matches(Pattern p, String s) {
        return s != null && !s.isEmpty() && p.matcher(s.toLowerCase(Locale.ROOT)).matches();
    }
}
