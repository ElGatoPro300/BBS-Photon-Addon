# Shared Photon addon sources

Current build dependency: **CML 3.0 beta 9 from Modrinth**, pinned by version ID and SHA-512 in `gradle/cml-hosts.json`. The build downloads and verifies the matching published host automatically. Earlier validation notes below describe the original migration.

## Scope and design

CML is the primary host and uses the canonical addon sources. Minecraft version selection (`gradle/minecraft-targets.gradle`) is independent of optional host compatibility (`gradle/host-compat.gradle`). The latter builds isolated FS/BBS 2 adapters through `tools/prepare_host_compat.py` and `tools/build_host_compat.py`, always with an explicit `--host`. See [host compatibility](host-compatibility.md) for those secondary distributions.

The existing version branches now share a source tree. Two NeoForge/Connector targets are configured: 1.21.1 and 26.1.2.

Shared form definitions, serialized Value identifiers, effect loading, playback and editor controls remain in their existing packages. Native version boundaries live in src/platform. NativeBbsAccess isolates legacy intermediary CML access and official typed access; NativePhotonAssets isolates the ResourceLocation/Identifier change. NativePostPreview owns each dependency family's HDR target, graph execution and GL state restoration. PhotonPostEffects keeps submission, parameters and once-per-frame behavior shared.

This migration preserves the current rendering behavior of each branch. It does not claim support for unconfigured Minecraft releases, switch loaders, replace the particle engine, or rename saved form properties. Existing branches are retained as historical references.

## Build

Run the Gradle wrapper on JDK 25:

```powershell
./gradlew.bat buildAll
./gradlew.bat build '-Pmc=1.21.1'
```

Installable JARs are collected in build/libs. Target-specific intermediates and test reports live under build/targets/<mc>. Every artifact declares its exact Minecraft target and corresponding Java requirement. Builds stop on unknown targets and incompatible CML dependencies.

`gradle/cml-host.gradle` invokes `tools/prepare_cml.py` to resolve the pinned published CML host into `build/cml-hosts/<mc>/`. Both compilation and development/validation runs use that exact artifact. No CML checkout, local compilation or manual `libs/` provisioning is required. Cached files are checked on every resolution; a missing or corrupted download is replaced only after SHA-512, host identity, API presence, Minecraft version and mapping namespace checks pass. The first build needs network access; a verified cached host can be reused offline.

The manifest's `release` also defines the CML minimum in addon metadata. To update CML, select a published compatible beta for every configured target and update its version ID, URL and SHA-512 in the manifest. Local host overrides (`cmlJar`, `libDir`, `bbs_dev_jar`, `bbs_cml_jar`, `cml_version`) are no longer supported. For 26.1.2 the published file is the CML 26.1 family artifact with official mappings.

The default build remains 1.21.1. settings.gradle selects ModDevGradle 1.0.21 with Connector 2 userdev on 1.21.1, or ModDevGradle 2.0.148 with Connector 3/Launchpad on 26.1.2. Both keep NeoForge as their runtime. Development runs stage host and addon JARs in isolated build/runs/<mc>/mods; -Pgame_directory overrides that development path.

## Verification

```powershell
python tools/verify-targets.py
python tools/test-clients.py
```

Artifact verification checks exact metadata, bytecode, duplicate archive entries, collected-byte equality and exclusion of CML, particle engines, Minecraft and nested dependencies. Both dependency families compile independently through buildAll.

The opt-in -PportSmoke fixture is excluded from release artifacts. It checks native client initialization, canonical form registration, effect/runtime serialization, renderer registration and editor construction after resource loading. Tests use build/validation/<mc>/client, never player profiles. Native run tasks use the selected Java toolchain. Connector discovers the test-only Fabric entrypoint from a separate smoke JAR staged only in the isolated validation mods folder.

Visual acceptance remains separate: actual authored effects, pause/scrub and seeking, model-block/bodypart placement, preview/world alignment, cleanup and shaders enabled/disabled need in-game inspection. Native startup checks do not establish rendering parity. Both native NeoForge/Connector client fixtures passed after selecting the compatible beta-2 CML host on 1.21.1. They registered COMMON and CLIENT addon API 2, checked canonical form/renderer/editor construction and round-tripped runtime controls and saved effect ids. Both also resolved and executed the built-in invert post graph in preview mode, then verified framebuffer, viewport and scissor restoration. This does not establish correct particles or post-effect pixels in authored world scenes.

## Published beta-9 host validation (2026-11-23)

`buildAll` and artifact verification passed for every configured target against
the checksum-pinned Modrinth beta-9 distributions. CML clients and dedicated
servers passed on each target. FS and BBS 2 client regressions passed on AAA
1.20.1 and Photon 1.21.1 with the same collected release JARs. Validation reports
record both the loaded host version and the published host SHA-512.

Integrity checks rejected corrupt hashes, mismatched host versions and wrong
mapping namespaces. Verified cached hosts resolved with network access disabled.
The old local-JAR override was rejected before dependency selection.

On 26.1.2 the title-screen fixture verifies the registered addon palette section
in isolation: CML's built-in Extra section constructs item stacks whose vanilla
components are not bound before world load. `ModernPaletteChecks` handles only
that specific host/lifecycle exception and does not change the published host
or production addon. Complete world palette behavior, authored effects and
shader visuals remain outside this startup verification.
