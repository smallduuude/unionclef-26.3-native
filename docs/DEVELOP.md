# Development guide

## Prerequisites

- **JDK 25** — the build targets Java 25 and the game runs on it.
- A **Fabric 26.3** profile if you want to run the built jar in a real launcher (optional;
  `runClient` launches a dev client for you).

## Get the code and build

```bash
git clone https://github.com/smallduuude/unionclef-26.3-native
cd unionclef-26.3-native
./gradlew build            # Windows: gradlew.bat build
```

The first build downloads the Gradle distribution plus Minecraft and Fabric (~1 GB) and caches it.
Every build after that is much faster.

Output — the distributable, remapped mod jar:

```
build/libs/unionclef-<mod_version>+26.3.jar
```

## Run a dev client

```bash
./gradlew runClient
```

This compiles and launches a Minecraft client with UnionClef loaded, from the `run/` directory.

## Useful Gradle tasks

```bash
./gradlew compileJava      # compile only (altoclef + tungsten)
./gradlew build            # full build, jars + checks
./gradlew clean build      # reproducible build from scratch
```

## Project layout

```
build.gradle            Root build; bundles the :tungsten subproject into the mod jar
gradle.properties       mod_version, minecraft_version, dependency versions
settings.gradle.kts     Root project + :tungsten
src/main/java/          altoclef — the bot: tasks, commands, trackers, GUI, py4j + MCP bridges
src/main/resources/     fabric.mod.json, mixins, assets
tungsten/               tungsten — the fallback pathfinder, plus aim and combat (a sibling Gradle project)
docs/                   this documentation
```

## Runtime configuration

Settings live under the game directory:

```
<game dir>/altoclef/altoclef_settings.json
```

The in-game settings screen (keybind **UnionClef Settings**, default **K**) writes the same file.
See the project README for the options.

## Troubleshooting

### Stale Loom cache

If the IDE reports "references non-existing library" or the build cannot resolve Minecraft after
switching JDK or Loom version:

1. Close the IDE and any running dev client.
2. Delete `.gradle/loom-cache`.
3. Rebuild: `./gradlew compileJava`.

### The client will not launch

- Confirm `java -version` reports 25 and that `JAVA_HOME` points at a JDK 25.
- Give Gradle enough memory (see `org.gradle.jvmargs` in `gradle.properties`).

### Out of memory when launching the real game

Minecraft 26.3 plus a modpack wants a few GB. If the launcher reports low memory, close other
applications or lower the instance's allocated RAM.

## Releasing

See [RELEASE.md](RELEASE.md).
