# Release guide

UnionClef is a **single-project** build (`settings.gradle.kts` includes only `:tungsten`). Publishing
is a manual step; there is no Gradle release task.

## How the version works

The version comes from `mod_version` in `gradle.properties`:

```properties
mod_version=1.0.0
```

`build.gradle` composes the artifact version as `"${mod_version}+${minecraft_version}"` and
`processResources` expands `${version}` into `fabric.mod.json`. The output jar is therefore:

```
build/libs/unionclef-<mod_version>+26.2.jar
```

## Release checklist

1. **Write the release notes.** Create `docs/releases/<mod_version>.md`. For every shipped change,
   say what changed and how to see it (the `@command` or GUI action that exercises it). This file is
   the human-facing changelog.

2. **Bump the version.** Edit `mod_version` in `gradle.properties`.

3. **Build.**

   ```bash
   ./gradlew clean build
   ```

   Confirm `BUILD SUCCESSFUL` and that `build/libs/unionclef-<mod_version>+26.2.jar` exists.

4. **Publish.** Create a GitHub release tagged `v<mod_version>` and attach the jar (there is only one
   jar — the sources/dev variants are not produced). Paste `docs/releases/<mod_version>.md` as the
   body.

   **Baritone is REQUIRED, and it is BUNDLED — say so as the release body's FIRST section.** There is
   nothing for the user to download, so the opener is one path rather than two:

   - a heading like *"Read this first — Baritone is REQUIRED, and it is INCLUDED"*,
   - "nothing to download: one file" plus the two log lines they should expect
     (`Baritone bridge ready.` and `Baritone settings applied: ...`),
   - and the note that a player who already has Baritone in `mods/` keeps theirs — Fabric Loader
     resolves one version per mod id, so their copy takes priority.

   Name the bundled build and its licence each time (1.0.0: `baritone-api-fabric-1.19.0.jar`,
   LGPL-3.0, shipped at `META-INF/licenses/LGPL-3.0.txt`). `docs/releases/0.97.0.md` is the worked
   example — copy its opening section.

5. **Re-scan and re-pin the VirusTotal hash.** The README and the release body both carry a scan link
   pinned to one specific file's SHA-256. Run the new jar through VirusTotal, then update the link
   **and the digest** in `README.md` and in the release notes. A link pinned to the previous build is
   worse than no link: it invites a user to verify a file they are not downloading.

6. **Verify.** Download the uploaded asset and confirm its `fabric.mod.json` reports the right
   version and `license`, **and that its `jars` array lists the bundled Baritone jar**
   (`META-INF/jars/baritone-api-fabric-<version>.jar`). Then install it in a test instance
   **with no standalone Baritone in `mods/`** and launch — that is what catches a nested jar that
   is present but undeclared.

## What ships in the jar

The jar is a self-contained Fabric jar-in-jar bundle — users install **one file**:

- UnionClef (altoclef) classes
- tungsten — the fallback pathfinder, and the aim/combat engine (the `:tungsten` subproject)
- **Baritone** — the movement engine, bundled as a Fabric nested JAR
  (`META-INF/jars/baritone-api-fabric-1.19.0.jar`), unmodified
- the Nether pathfinder
- Jackson (core / annotations / databind)
- Py4j
- Cloth Config
- `META-INF/LICENSE` (GPL-3.0), `META-INF/licenses/LGPL-3.0.txt` (Baritone's), and
  `META-INF/THIRD_PARTY_NOTICES.md`

Baritone is **both required and bundled**: `fabric.mod.json` declares it under `depends`, and the jar
provides it, so a user who would never go and find it still gets the best movement engine. It is added
to the jar by `processResources` rather than Loom's `include` (which needs a module component, not a
file), **and it is declared in `fabric.mod.json`'s `jars` array — Fabric Loader discovers nested jars
only from that array, not by scanning `META-INF/jars`.** It needs no remapping because it is
Baritone's **released** jar — already built for production Fabric. A player who has their own
Baritone in `mods/` keeps it: Fabric Loader resolves one version per mod id.

## License

UnionClef is released under the **GNU General Public License v3.0** (`GPL-3.0-only`, see
[`../LICENSE`](../LICENSE)). If you redistribute a modified build you must also make the complete
corresponding source available under the same license.
