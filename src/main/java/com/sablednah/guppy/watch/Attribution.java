package com.sablednah.guppy.watch;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;

/**
 * Which mod's code asked for an extra tick: the first frame on the stack that is not Minecraft,
 * the loader, a mixin library or us.
 *
 * <p>Walked once, when an acceleration starts, never per tick. A stack with no such frame is
 * vanilla's own business (bone meal on a fully grown stem random-ticks it, a game test does too),
 * and is not reported.</p>
 */
public final class Attribution {

    /** Who did it in code, or {@link #VANILLA}. */
    public record Caller(String modId, String className) {}

    public static final Caller VANILLA = new Caller("minecraft", "");

    private static final String[] SKIP = {
            "java.", "jdk.", "sun.", "com.sun.",
            "net.minecraft.", "com.mojang.", "net.neoforged.", "net.minecraftforge.", "cpw.mods.",
            "org.spongepowered.", "com.llamalad7.", "it.unimi.", "com.google.",
            "com.sablednah.guppy.",
    };
    /** The self-test plays an accelerator, so it must not be skipped as one of ours. */
    private static final String SELF_TEST = "com.sablednah.guppy.neoforge.SelfTest";

    private static final Map<Class<?>, String> MOD_OF = new HashMap<>();
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private Attribution() {}

    public static Caller caller() {
        Optional<Class<?>> found = WALKER.walk(frames -> frames
                .map(StackWalker.StackFrame::getDeclaringClass)
                .filter(Attribution::foreign)
                .findFirst());
        if (found.isEmpty()) return VANILLA;
        Class<?> c = found.get();
        String mod = modOf(c);
        if (isVanillaMod(mod)) return VANILLA;
        return new Caller(mod, c.getName());
    }

    private static boolean foreign(Class<?> c) {
        String name = c.getName();
        if (name.startsWith(SELF_TEST)) return true;
        for (String prefix : SKIP) {
            if (name.startsWith(prefix)) return false;
        }
        return !isVanillaMod(modOf(c));
    }

    private static boolean isVanillaMod(String mod) {
        return "minecraft".equals(mod) || "neoforge".equals(mod) || "forge".equals(mod);
    }

    /**
     * The mod a class came from, by its package. Built once, on the first acceleration, from the
     * loader's scan of every mod jar, which every line has; how a class maps to its jar at run time
     * is the part that keeps changing between loader versions.
     */
    static String modOf(Class<?> c) {
        return MOD_OF.computeIfAbsent(c, Attribution::lookUp);
    }

    private static Map<String, String> packages;

    private static String lookUp(Class<?> c) {
        if (c.getName().startsWith(SELF_TEST)) return "guppy-selftest";
        if (packages == null) packages = scanPackages();
        String pkg = c.getPackageName();
        while (!pkg.isEmpty()) {
            String mod = packages.get(pkg);
            if (mod != null) return mod;
            int dot = pkg.lastIndexOf('.');
            pkg = dot < 0 ? "" : pkg.substring(0, dot);
        }
        return "unknown";
    }

    private static Map<String, String> scanPackages() {
        Map<String, String> map = new HashMap<>();
        for (ModFileScanData scan : ModList.get().getAllScanData()) {
            String mod = null;
            for (IModFileInfo file : scan.getIModInfoData()) {
                if (!file.getMods().isEmpty()) {
                    mod = file.getMods().get(0).getModId();
                    break;
                }
            }
            if (mod == null) continue;
            for (ModFileScanData.ClassData data : scan.getClasses()) {
                String name = data.clazz().getClassName();
                int dot = name.lastIndexOf('.');
                if (dot > 0) map.putIfAbsent(name.substring(0, dot), mod);
            }
        }
        return map;
    }
}
