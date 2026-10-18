# CML 3.0 migration

## Minecraft 26.1.2 target and build

This branch targets Java 25, Minecraft 26.1.2, NeoForge 26.1.2.122, Connector 3.0.0-beta.6, Launchpad 1.10.2 and Forgified Fabric API 0.155.3+26.1.2+3.5.9. Photon is 26.1.2.3, LDLib2 26.1.2.42 and KilaGraph 26.1.0.16. Development shaders use Sodium 0.9.2 / Iris 1.11.4 for NeoForge.

The CML host remains compile-only and defaults to `libs/bbs-cml-3.0-beta-2-26.1.jar`, whose metadata includes 26.1.2. There is no fallback to an arbitrary JAR in `libs`; a missing configured host fails explicitly. `verifyCmlHost` checks addon API presence and Java 25 bytecode, rejecting the old 1.21.1 host. Stage tasks and launches share `game_directory` (default `run`); use `-Pgame_directory=build/validation/client-26.1.2` to isolate tests from personal worlds.

CML and NeoForge now use matching unobfuscated signatures. The addon keeps API 2, `bbs:photon`, all Value IDs, `.fx` / `.fxpack` identifiers and NeoForge + Connector support. See [Photon API differences](photon2-compat-plan.md#minecraft-2612-adaptation).

## Verification on 26.1.2

`build` passes with Java 25. An isolated `runClient` loads `bbs_photon_addon` on COMMON and CLIENT with API 2 and starts a fresh integrated-server flat world. Development-only attached probes pass 25 checks without shaders, then the same checks with Iris/Caz-Toon 1.2.1 active (plus a shader-active assertion).

The checks cover canonical form type, saved effect/pause round trip, extended control copying, renderer registration, editor construction, icon loading, post graph resolution, disabled post-effect request cleanup, runtime start, pause/rate restoration, authored-delay preservation, bounded 100-tick catch-up, completion and backward seek, explicit pause, seed policy and cache retirement. The GPU checks invert the crop from approximately `(0.2, 0.3, 0.4)` to `(0.8, 0.7, 0.6)`, preserve outside pixels, restore framebuffer/scissor/program state and preserve the RGBA16F HDR format and leave no GL error. Probe files and reports remain ignored in `build/validation` and are not shipped.

Interactive world/model-block/bodypart placement, F7, real film exports, signal crossings, light/fog behavior, resource reload and multiple simultaneous forms still require visual acceptance. These probes do not establish universal shader compatibility or historical transform reconstruction. Startup also logs pre-existing host/Connector warnings (including the adapter refmap and host projectile attributes); neither prevents the tested world from loading.

## Historical 1.21.1 target and build

This branch targets Minecraft 1.21.1, NeoForge 21.1.248+, Connector 2.0.0-beta.12, Forgified Fabric API 0.116.7+2.2.0+1.21.1 and CML 3.0 addon API 2. Photon remains pinned to 2.2.7, LDLib2 to 2.2.41 and KilaGraph to 21.1.0.15. No Photon simulation or effect format upgrade is part of this migration.

`bbs_cml_jar` defaults to `../BBS-CML-3.0/build/libs/bbs-cml-3.0-dev-1-1.21.1.jar`. It must be the remapped distribution JAR, not the Yarn-named development classes or sources JAR. The host is compile-only; `stageCmlHost` puts it in the development mods directory, where Connector remaps it. `stageConnectorAddon` stages the addon JAR. Both tasks precede client and server launches. Neither dependency is bundled in the release JAR.

The addon itself uses Mojang Minecraft calls and the NeoForge Photon artifact. Fabric-shaped metadata is used only for Connector discovery and CML's BBS entrypoints. Metadata explicitly requires NeoForge, and there is no native Fabric launch configuration. Connector is the required loader bridge, but is not listed as a Fabric dependency because its own mod is not exposed to that dependency resolver.

## Observed host API changes

| Old integration | CML 3.0 integration |
| --- | --- |
| `addons.BBSAddon` / `BBSClientAddon` | `api.BBSAddonMod`, `requiredApiVersion() == 2` |
| `events.Subscribe` and `events.register.*` | `api.Subscribe`, common `api.events.*`, client `api.client.events.*` |
| `bbs-addon-client` | `bbs-client-addon` |
| Combined renderer/editor registration | Separate renderer and editor events |
| Global STRING factory replacement | Property-specific keyframe registration |
| Private replay COLORS/ICONS maps | Public track presentation descriptor; custom icon is not part of that contract |
| Late Extra category mutation | Public form section registration, rebuilt with the palette |
| Private dashboard panel/runner reflection | Active `UIScreen` menu plus public dashboard/film-panel methods |
| Entity age alone for seeks | Explicit `previewTick` / `animationTick`, with entity age fallback |
| Renderer repeats the form transform | Host-provided transformed stack, including overlays and bodyparts |

`BbsRenderBridge` contains intermediary-to-Mojang access. `PhotonFxBridge` remains the only owner of Photon APIs. The renderer ignores picking-only passes so stencil selection does not start or move live FX.

## Saved data

`PhotonForm` and its constructor are unchanged. The canonical type is still `bbs:photon`; the persistent properties remain `photon_fx` and `paused`. Existing `namespace:path` FX IDs, loose `.fx` effects and mounted `.fxpack` IDs retain the same interpretation. Host-wide compatibility of old films remains CML's responsibility.

## Verification

The migration compiles against the local CML 3.0 distribution. Runtime verification must also cover Connector discovery of both BBS entrypoints, resource loading, property-specific keyframe editing, world/model-block playback, bodypart placement, editor pause and backward seeking, one-shot completion, cleanup on close/disconnect and Iris shaders enabled/disabled.

The initial development launch exposed a NeoForge 21.1.219 versus Forgified Fabric API minimum 21.1.248 mismatch and the need to stage Fabric host JARs in `run/mods`; the build now accounts for both. A successful compilation does not establish visual rendering compatibility.

## Companion host corrections and validation (2026-10-13)

The local CML checkout also needs the fixes in this migration: the declared Fabric Loader minimum is 0.16.0 (public APIs in use), so Connector's 0.17.x identity resolves; server sound hooks use complete descriptors to distinguish Mojang overloads; and block-interaction, loopback-audio and mob-feature Redirect hooks use the existing WrapOperation pattern. Calls forward through Operation.call, preserving the host's behavior and other operation wrappers. The temporary loader override used to diagnose the issue was removed.

Both repository builds passed. The NeoForge client reached the title screen and loaded `bbs_photon_addon` on COMMON and CLIENT with API 2. A temporary verification agent on the render thread passed nine checks: canonical form registration, saved form type, effect/paused serialization round trip, renderer registration, editor construction, icon resource, Spanish resource, property-specific keyframe editor and Photon palette section. The verification fixture and reports live only under the ignored `build/validation` directory.

The dedicated NeoForge server also loaded the COMMON addon, generated its development world and reached `Done`. Client and server were then stopped cleanly; both Gradle launch tasks reported success. This verifies initialization and common/client separation, not multiplayer FX synchronization.

Visual world/model-block/bodypart playback, pause/scrub, Iris shader packs enabled/disabled, action recording and loopback audio capture still require an in-game review. The startup checks do not establish those behaviors.
