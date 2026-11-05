# Shared Photon addon sources

Current build dependency: **CML 3.0 beta 7**, pinned by `cml_version` and the addon metadata. Provision the matching beta-7 host for each configured Minecraft target. Earlier validation notes below describe the original migration.

## Scope and design

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

Place the existing matching CML distribution JARs in libs. A folder containing multiple versions is supported: selection checks the mod id, manifest Minecraft version and namespace. For 26.1.2 the explicitly pinned host artifact is CML 26.1 (official names), matching the existing branch. Override a folder with -PlibDir=<directory>, or select one host with -PcmlJar=<path>. The legacy -Pbbs_cml_jar override is retained. CML 3.0 beta 2 or newer is required for the 1.21.1 Connector runtime: the old beta-1 host fails in ServerWorldMixin sound injections before addon initialization. The local beta-1 JAR was preserved under libs/archive and replaced with the already available beta-2 distribution. A single override must match each selected target; use a mixed libs directory for buildAll.

The default build remains 1.21.1. settings.gradle selects ModDevGradle 1.0.21 with Connector 2 userdev on 1.21.1, or ModDevGradle 2.0.148 with Connector 3/Launchpad on 26.1.2. Both keep NeoForge as their runtime. Development runs stage host and addon JARs in isolated build/runs/<mc>/mods; -Pgame_directory overrides that development path.

## Verification

```powershell
python tools/verify-targets.py
python tools/test-clients.py
```

Artifact verification checks exact metadata, bytecode, duplicate archive entries, collected-byte equality and exclusion of CML, particle engines, Minecraft and nested dependencies. Both dependency families compile independently through buildAll.

The opt-in -PportSmoke fixture is excluded from release artifacts. It checks native client initialization, canonical form registration, effect/runtime serialization, renderer registration and editor construction after resource loading. Tests use build/validation/<mc>/client, never player profiles. Native run tasks use the selected Java toolchain. Connector discovers the test-only Fabric entrypoint from a separate smoke JAR staged only in the isolated validation mods folder.

Visual acceptance remains separate: actual authored effects, pause/scrub and seeking, model-block/bodypart placement, preview/world alignment, cleanup and shaders enabled/disabled need in-game inspection. Native startup checks do not establish rendering parity. Both native NeoForge/Connector client fixtures passed after selecting the compatible beta-2 CML host on 1.21.1. They registered COMMON and CLIENT addon API 2, checked canonical form/renderer/editor construction and round-tripped runtime controls and saved effect ids. Both also resolved and executed the built-in invert post graph in preview mode, then verified framebuffer, viewport and scissor restoration. This does not establish correct particles or post-effect pixels in authored world scenes.
