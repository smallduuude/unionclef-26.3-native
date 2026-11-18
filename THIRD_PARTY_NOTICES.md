# Third-party notices

UnionClef (mod id `altoclef`) is distributed under the **GNU General Public License v3.0**
(GPL-3.0-only). The full text travels with the jar at `META-INF/LICENSE` and in the repository as
[`LICENSE`](LICENSE).

## Bundled inside the jar

These are compiled or shaded into the release jar, and their notices are reproduced here as their
licences require.

| Component | Licence | Project |
|---|---|---|
| **[Baritone](https://github.com/cabaletta/baritone)** | **LGPL-3.0** | <https://github.com/cabaletta/baritone> |
| **Tungsten** | GPL-3.0 (this project's own subproject) | — |
| **Nether Pathfinder** | MIT | <https://github.com/babbaj/nether-pathfinder> |
| **Jackson** (core, annotations, databind) | Apache-2.0 | <https://github.com/FasterXML/jackson> |
| **Py4j** | BSD-3-Clause | <https://www.py4j.org/> |
| **Cloth Config** (`cloth-config-fabric`) | LGPL-3.0 | <https://github.com/shedaniel/cloth-config> |

Baritone ships as a **Fabric nested JAR** at `META-INF/jars/baritone-api-fabric-1.20.0.jar`, exactly
as release-published and unmodified. LGPL-3.0 section 4 asks that a combined work carry a copy of the
GNU GPL and of the LGPL, and give notice that the library is used — hence `META-INF/LICENSE` (GPL-3.0)
and `META-INF/licenses/LGPL-3.0.txt` inside this jar, and this table. Section 4(d) asks that the user
be able to substitute a modified library: Fabric Loader does that natively — it resolves one version of
a mod id and prefers the best match, so dropping your own Baritone into `mods/` **replaces** this nested
copy rather than conflicting with it. Baritone's full corresponding source is upstream at the URL above.

Copyright notices for the above remain with their respective authors. Apache-2.0 requires that its
NOTICE contents be preserved; none of the bundled Jackson artifacts ship a separate `NOTICE` file, so
this table stands as the notice.

## Runtime dependencies that are NOT bundled

| Component | Licence | Notes |
|---|---|---|
| **Minecraft** | Mojang EULA | not redistributed |
| **Fabric Loader / Fabric API** | Apache-2.0 | installed by the user |
| **[Simple Voice Chat](https://github.com/henkelmax/simple-voice-chat) API** | MIT | compile-time only; the integration is inert unless you install Simple Voice Chat |

## Upstream

UnionClef is the union of two upstream projects, both GPL-3.0:

- **AltoClef** by TacoTechnica and contributors — <https://github.com/3ndetz/unionclef>
- **Tungsten** — vendored here as the `tungsten/` subproject

The Minecraft 26.3 native port is maintained at
<https://github.com/smallduuude/unionclef-26.3-native>.

If you distribute a modified UnionClef, the GPL requires that you make the complete corresponding
source available under the same licence.
