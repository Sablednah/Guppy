#!/usr/bin/env bash
# Build Guppy and copy the jar into a test instance's mods/ folder, then launch that instance from
# CurseForge to see it live.
#
# Usage:   ./deploy.sh
# Override the target instance:
#          GUPPY_INSTANCE="/path/to/instance" ./deploy.sh
#
# One instance per Minecraft line, so the branch you are on decides where the jar goes. The
# calendar-versioned instances are named for their Minecraft version and shared with the other mods
# in this family; 1.21.11 is the Standards instance and 1.20.1 is the Forge one.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"

MC_VERSION="$(sed -n 's/^minecraft_version=//p' "$ROOT/gradle.properties" | head -1)"
if [ -z "$MC_VERSION" ]; then
    echo "!! Could not read minecraft_version from gradle.properties" >&2
    exit 1
fi

# The JDK tracks Minecraft: 17 for 1.20.x, 21 for the 1.21 lines, 25 for 26.x. A JAVA_HOME set by
# the caller always wins. tools/ is gitignored; CityWorld's copy is used when this repo has none
# (one level up from main, two from a worktree).
if [ -z "${JAVA_HOME:-}" ]; then
    case "$MC_VERSION" in
        1.20.*) WANT_JDK="jdk17" ;;
        1.*)    WANT_JDK="jdk21" ;;
        *)      WANT_JDK="jdk25" ;;
    esac
    for dir in "$ROOT/tools/$WANT_JDK" "$ROOT/../CityWorld-ReForged/tools/$WANT_JDK" "$ROOT/../../CityWorld-ReForged/tools/$WANT_JDK"; do
        if [ -x "$dir/bin/java" ]; then export JAVA_HOME="$(cd "$dir" && pwd)"; break; fi
    done
    if [ -z "${JAVA_HOME:-}" ]; then
        echo "!! Minecraft $MC_VERSION needs $WANT_JDK: put one in tools/$WANT_JDK, or set JAVA_HOME." >&2
        exit 1
    fi
fi
export PATH="$JAVA_HOME/bin:$PATH"

case "$MC_VERSION" in
    1.20.1)  DEFAULT_INSTANCE="1.20.1  Forge" ;;
    1.21.11) DEFAULT_INSTANCE="Standards" ;;
    *)       DEFAULT_INSTANCE="$MC_VERSION" ;;
esac
INSTANCE="${GUPPY_INSTANCE:-/mnt/c/Users/darre/curseforge/minecraft/Instances/$DEFAULT_INSTANCE}"
MODS="$INSTANCE/mods"

# Family convention: an instance whose root holds .sablecraft-no-deploy never takes a dev jar, not
# even when named explicitly. A modpack that must ship only released jars is what it is for.
if [ -e "$INSTANCE/.sablecraft-no-deploy" ]; then
    echo "!! Refusing to deploy: $INSTANCE/.sablecraft-no-deploy" >&2
    exit 3
fi

echo ">> Building Guppy for Minecraft $MC_VERSION (JDK: $(basename "$JAVA_HOME"))..."
"$ROOT/gradlew" build --console=plain

if [ ! -d "$MODS" ]; then
    echo "!! Instance mods folder not found: $MODS" >&2
    echo "!! Create an instance named '$DEFAULT_INSTANCE', or run: GUPPY_INSTANCE=\"/path\" ./deploy.sh" >&2
    exit 1
fi

# Name the jar exactly: build/libs keeps whatever other branches built here, and "newest" deploys
# another line's jar silently the moment a build is up to date.
MOD_VERSION="$(sed -n 's/^mod_version=//p' "$ROOT/gradle.properties" | head -1)"
JAR="$ROOT/build/libs/guppy-${MOD_VERSION}+mc${MC_VERSION}.jar"
if [ ! -f "$JAR" ]; then
    echo "!! Expected jar not found: $JAR" >&2
    exit 1
fi

instance_locked() {
    echo "!! Could not $1 the jar in the instance's mods folder." >&2
    echo "!! Is the '$(basename "$INSTANCE")' instance still running? Close Minecraft and retry." >&2
    exit 1
}

rm -f "$MODS"/guppy-*.jar || instance_locked "remove"
cp "$JAR" "$MODS/" || instance_locked "copy"
if ! cmp -s "$JAR" "$MODS/$(basename "$JAR")"; then
    echo "!! The deployed jar does not match the one just built." >&2
    exit 1
fi

echo ">> Deployed: $(basename "$JAR") ($(stat -c%s "$JAR") bytes) to $(basename "$INSTANCE")"
