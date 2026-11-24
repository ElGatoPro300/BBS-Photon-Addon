# CML, FS and BBS 2 host compatibility

Photon supports FS 2.7 on Minecraft 1.21.1 (NeoForge with Connector). Minecraft 26.1.2 remains CML-only.

Photon adds upstream BBS 2.5 on Minecraft 1.21.1 (NeoForge with Connector). Other upstream versions and Minecraft targets are not declared compatible.

CML remains the primary host. A release contains the canonical CML implementation
and package-isolated secondary host implementations, generated from the same sources plus
small explicit host overrides. Neither host is bundled. Form ids and resource ids
remain identical across hosts.

The Fabric language adapter selects the host from its metadata before loading
addon classes. CML is identified by `bbs:fork_id=CML`. Published FS 2.7 is identified
by its name and exact version. Upstream BBS 2.5 is identified by `BBS mod`, exact version `2.5-1.21.1`, and absence of the CML fork marker. Unrecognized hosts fail closed.

FS compilation uses the SHA-512-pinned Modrinth distributions in
`gradle/fs-hosts.json`, never a local FS build. The upstream distribution is pinned independently in `gradle/bbs2-hosts.json`. Downloads and generated Java live
under `build/fs-compat/`. CML-only lifecycle and track events stay outside FS
classes because the FS event bus resolves every declared method signature.

FS support is restricted to the published host/engine intersections listed in
the manifest. Other Minecraft targets retain CML support only. Build validation
must cover every declared host using the same final release, including registration,
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

## Upstream BBS 2 adapter

The existing source generator accepts `--host bbs2`; it emits isolated `.bbs2`
classes and applies `src/hosts/bbs2` overrides. The normal release build packages
all supported host implementations into one JAR per Minecraft target. Neither
the host nor FX engine is bundled, and persistent form/Value IDs remain unchanged.
Targets without an upstream adapter do not declare its mixin configuration or
accept its host version.

BBS 2 uses `events.BBSAddonMod` rather than the CML/FS `api` interface. Its common
addon entrypoint registers source packs and waits for `RegisterSettingsEvent`,
which occurs after construction of the form registry. The upstream event bus
only scans declared methods, so registration handlers are declared directly.

BBS 2 does not discover `bbs-client-addon`. A host-guarded mixin subscribes the
client language handler before `BBSModClient.onInitializeClient`, then registers
renderers/editors and native lifecycle callbacks after initialization. A palette
setup hook rebuilds the addon section with the host and uses upstream's persisted
visibility manager. These hooks never apply to CML/FS and remain client-only.

Photon resolves owning replay time using the replay's integer list index,
which is how upstream keys its entity map. The local replay clock preserves
looping, pause and backward seeks. Preview time comes from the preview entity
age and interpolation; it does not read absent CML/FS context fields. Small
editor adapters isolate numeric Value conversion and target-entity access.
The FX keyframe picker intercepts only `photon_fx` in upstream's keyframe popup
and preserves its undo callback; other string editors retain host behavior.

Build with JDK 25 as before. To run the isolated production-artifact checks:

```powershell
./gradlew.bat buildAll
python tools/test-clients.py --host bbs2 --mc 1.21.1
python tools/test-clients.py --host bbs2 --mc 1.21.1 --server
```

Client fixtures verify canonical IDs, serialization, renderer/editor/palette
registration, palette rebuilding, English/Spanish switching and host-specific
clock/pause linkage. The dedicated server checks form serialization and resource
registration without loading client classes. Fixtures stay outside release JARs;
checksums ensure each host uses the same collected production artifact.

Visual native-effect playback in world/model blocks/bodyparts, film export and
shader-enabled rendering still require a manual regression pass before claiming
complete visual parity. Upstream BBS 2.5 itself emits resource-enumeration warnings
for Connector union paths and missing first-run settings; these warnings are
separate from addon registration and are not patched by this adapter.

## Validation on 2026-11-23

`buildAll` and production-artifact verification passed for every configured
Minecraft target (AAA: 1.20.1, 1.21.1, 26.1.2; Photon: 1.21.1, 26.1.2).
The 1.21.1 client fixtures passed on CML, published FS 2.7 and published upstream
BBS 2.5. The same collected release SHA-512 is checked across those hosts.
Upstream dedicated servers reached startup, passed common form/resource checks
and stopped cleanly. AAA's existing eight playback tests passed on each of its
three targets (24 executions). Photon also passed post-effect graph resolution
and preview framebuffer/viewport/scissor restoration.

These are integration checks in actual client/server processes. They do not
establish visual native-effect or shader parity; the manual cases listed above
remain pending.
