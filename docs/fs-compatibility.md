# CML and FS host compatibility

Photon supports FS 2.7 on Minecraft 1.21.1 (NeoForge with Connector). Minecraft 26.1.2 remains CML-only.

CML remains the primary host. A release contains the canonical CML implementation
and a package-isolated FS implementation, generated from the same sources plus
small explicit host overrides. Neither host is bundled. Form ids and resource ids
remain identical across hosts.

The Fabric language adapter selects the host from its metadata before loading
addon classes. CML is identified by `bbs:fork_id=CML`. Published FS 2.7 is identified
by its name and exact version; unrecognized forks fail closed.

FS compilation uses the SHA-512-pinned Modrinth distributions in
`gradle/fs-hosts.json`, never a local FS build. Downloads and generated Java live
under `build/fs-compat/`. CML-only lifecycle and track events stay outside FS
classes because the FS event bus resolves every declared method signature.

FS support is restricted to the published host/engine intersections listed in
the manifest. Other Minecraft targets retain CML support only. Build validation
must cover both hosts using the same final release, including registration,
serialization, editors, tick cleanup and film playback. Rendering validation
must exercise native effects and shaders before claiming official support.

## Build and verify

`./gradlew build -Pmc=1.21.1` compiles both host implementations and produces one
installable addon JAR. `./gradlew buildAll` still collects all supported Minecraft
releases into `build/libs/`. Python is required for source generation. The first
FS build downloads the pinned published JAR; subsequent builds reuse the verified
copy under `build/fs-compat/`. No FS repository checkout or build is needed.

After building, run `python tools/test-clients.py --host fs` and
`python tools/test-clients.py --host cml`. Each command launches isolated clients
and writes its logs and artifact checksums to `build/validation/`. The legacy AAA
checks load the collected production JAR through Loom; Photon loads that same
release through Connector. Modern CML-only AAA uses the existing development
fixture. Test classes and host distributions are excluded from releases.

The FS adapter uses its public track styles/categories, category preferences and
client lifecycle callbacks. Photon reads the owning replay's local tick (including
looping and pause) and FS's viewport timer, because FS does not expose CML's sampled
render-clock fields. Shader/world playback still needs a visual regression pass;
startup and preview state checks cannot establish complete visual parity.

## Validation on 2026-11-04

All configured production targets compiled. The 3 host/target client checks
passed registration, serialization and editor construction, with identical release
checksums between CML and FS on shared Minecraft targets. Photon also passed its
preview post-graph and framebuffer/viewport/scissor restoration checks.  These checks do not replace
visual world/film playback and shader validation before publication.
