# Photon runtime features: implementation blueprint

Status: implemented, with the limits and verification described below. Covers the six requested improvements on Photon 2.2.8, Minecraft 1.21.1, NeoForge + Connector and CML 3.0 addon API 2. No dependency bump or publication is part of this work.

## Authoring controls

The Photon form editor adds four panels: **Parameters**, **Playback and signals**, **Object anchors**, and **Post processing**. All new settings use BBS Values and participate in ordinary film property tracks. Existing `photon_fx`, `paused` and `bbs:photon` identifiers are unchanged.

- Parameters: add an entry, choose an authored FX object, then choose a property. The picker reads Photon's per-object runtime binding metadata, including supported custom GPU data. Edit the resulting Boolean, integer, color, scalar or three-component value. Lights and fog use this same panel. Disable/remove an entry to restore the underlying authored/Photon timeline sample. Changing a property does not rewrite the shared `.fx` definition. Properties retain Photon's own module enable flags and sampling semantics; a start-only property is not retroactive to already spawned particles.
- Playback: opt into film synchronization to resimulate to `floor(max(0, filmTick * speed + startOffset))`; speed applies when its override is enabled. Thus an animated speed retimes the current target rather than integrating a historical speed curve. Film sync uses a stable seed even if the separate fixed-seed toggle is off. Outside film sync, a fixed seed and start offset can still be used. Explicit form pause holds the sampled target while the film advances.
- Seeking: backward seeks recreate the runtime and replay from zero. Large jumps advance at most 100 simulation ticks per render call until reaching the requested tick; the diagnostic counter shows progress. This uses the **current** form parameters, root placement and anchor samples. It does not reconstruct historical actor transforms, parameter keyframes, collisions or shader time. Reproducibility is limited to compatible FX and stable simulation inputs. Normal forward one-tick playback can dispatch signals/audio; seeking/catch-up suppresses them.
- Anchors: choose a destination FX-object UUID, a CML model attachment/bodypart path, and an offset transform. The root form's public matrix cache and current rendered placement resolve sibling/bone transforms, including bodypart contexts whose stub entity has no form. Disabling a binding restores the underlying local transform. Names are labels; UUIDs identify objects.
- Signals: enable local events in Playback, then open the diagnostic monitor for a snapshot of the latest 32 events, unresolved bindings and catch-up progress. Dispatch is local to the client and opt-in. No commands, network actions or configurable gameplay side effects are added.
- Post processing: select a Photon render/fullscreen graph, weight and exposed scalar/vector/Boolean/integer parameters. World rendering submits once per frame to Photon. Preview rendering uses its own stack and a cropped color/depth target, applies only inside the current viewport, and restores framebuffer, viewport, shader/render flags, scissor and shader textures afterward. Closing playback releases its private target. Photon client configuration and Iris compatibility still govern world post-effect execution.

## Client event API

Client addons can subscribe to `elgatopro300.bbsphoton.client.api.PhotonSignalEvent` through the existing `BBSMod.events` bus (`@Subscribe`, or register/unregister their own listener through that bus). The event contains the source `PhotonForm`, effect ID, channel, name, master-clock time and a defensively copied `CompoundTag` payload. Keep consumers client-only; the form reference is the live source instance. Forward playback emits once at the authored signal crossing; catch-up does not replay events.

## Scope and compatibility

The current form exposes only `photon_fx` and `paused`. Extend it with optional BBS Values, keeping `bbs:photon` and existing Value IDs unchanged. Missing new fields must preserve existing playback. World forms, film playback, form-editor previews and bodyparts share the same implementation. Iris and vanilla use the same control semantics, subject to Photon's documented rendering fallbacks.

Affected subsystems: form serialization, keyframe editing, editor panels/localization, playback clock, Photon runtime overrides, root/object transforms, event dispatch, post-processing, client ticks and cleanup. This is an architectural feature spanning more than five files.

Reuse `PhotonFxBridge`, `IPhotonFxHandle`, `PausableEntityEffectExecutor`, `PhotonFxDiscovery`, `BbsRenderBridge` and `BbsFilmPauseProbe`. Reuse CML Values, keyframe factories and anchor/bodypart facilities. Do not add parallel asset scanners or duplicate runtime ownership in panels.

## Stages

| Stage | Behavior | Existing integration points | Focused responsibilities to extract/add |
| --- | --- | --- | --- |
| 1. Animated parameters | Optional per-object color, emission, start speed, gravity, particle budget and supported custom GPU data overrides; restore authored fallback when disabled | `PhotonForm`, `UIPhotonForm`, `UIPhotonFormPanel`, addon keyframe registration, `PhotonFxBridge` | Typed serializable parameter bindings, binding editor, runtime override application |
| 2. Film clock | Playback rate, start offset, stable seed and opt-in film synchronization; rebuild/resimulate on seeking backward or jumping | Renderer timeline samples, executor randomness, pause controller | Playback clock and bounded resimulation controller, independent of UI |
| 3. Lights/fog | Bind supported intensity, range, color, density and related runtime properties to BBS Values | Stage 1 binding mechanism; Photon `LightObject` / `FogVolumeObject` runtime slots | Extend typed bindings rather than create a second override system |
| 4. Object anchors | Select an FX object by UUID, bind its transform to a CML anchor/bodypart target with an offset | `BbsRenderBridge`, CML anchor/bodypart transforms, `IPhotonFxHandle` | Per-instance anchor resolution and interpolated transform application |
| 5. Signals | Receive named Photon timeline signals with source form, channel, name, payload and time; expose local events and diagnostics | Executor `onTimelineSignal`, client lifecycle | Scoped signal dispatcher; configurable actions only if chosen and supported by host contracts |
| 6. Post-processing | Select a Photon post-effect and animate weight/typed parameters; isolate preview ownership and cleanly cease requests when hidden/stopped | Photon per-frame request API, executor `postEffectSink`, CML render context | Post-effect submission adapter and preview render-target integration |

## Implemented contracts

The Photon adapters own runtime behavior; panels only edit Values.

- Domain: typed binding entries hold object UUID, property identifier, enabled state and typed BBS Values. No Photon/Minecraft types or manually serialized NBT in the form.
- `IPhotonFxHandle.renderControls(context, paused)` delegates to an instance-owned `PhotonRuntimeController`. `diagnostics()` returns an immutable list for the editor. The form-aware bridge `start` overload configures the controller before emission; the original overload remains available.
- `PhotonRuntimeController` owns simulation time, restart decisions, bounded catch-up, signal history and the focused parameter/anchor/post-effect adapters. `PausableEntityEffectExecutor` wraps root evaluation so enabled BBS overrides are applied after Photon's tick and frame samples.
- `PhotonRuntimeParameters` restores the underlying slot values before Photon evaluation and applies enabled overrides afterward. `PhotonObjectCatalog` discovers object/property metadata without emitting an effect. `PhotonObjectAnchors` scopes and restores transform overrides. `PhotonPostEffects` owns the preview stack/target and render-state restoration.
- Signals use the host event bus and an instance-owned history. No new mutable static registry. Consumers run on the client thread.
- Editor: dedicated parameter, playback, anchor and post-effect sections/panels. Reuse standard CML typed Value editors and localize labels across bundled languages.

## Accepted decisions

1. Explicitly enabled BBS overrides take precedence; disabled overrides restore Photon ownership.
2. Signals are local events plus a diagnostic monitor. Configurable actions are outside this implementation.

## Verified API constraints and risks

- `RuntimeValue.set/clear` affects one runtime; never modify the cached shared FX definition. Photon timeline evaluation writes the same slots, so precedence must be explicit.
- Photon runtime object lookup supports UUIDs and names. UUIDs avoid ambiguous duplicate names. Removed or incompatible targets must be reported in the editor without crashing playback.
- `FXRuntime.setRate` exists, but authored root speed tracks can override it. Preserve the current full-runtime pause safeguards and define clock ownership when external speed is enabled.
- A seed alone does not guarantee reproducibility of world collisions, moving anchors, sound or shader world-time inputs. Seeking needs controlled simulation inputs/history, bounded catch-up and explicit limits. Do not claim a public constant-time seek API.
- Historical actor/anchor transforms during resimulation may require host support. Inspect CML before choosing replay sampling or an explicitly restricted mode; report gaps rather than silently simulating every historical tick at the current position.
- Signals are forward-playback events. Do not replay side effects during scrub/resimulation. Suppress audio dispatch during catch-up, then resume normal forward behavior.
- Post-effects must be submitted every visible frame. Freezing timeline evaluation must retain the frozen visual sample without repeatedly firing signals. An isolated stack also needs a correctly owned render target; changing `postEffectSink` alone is insufficient.
- CML picking, shadows, UI previews and export can run multiple render passes. Submit effects once to the correct target; do not let a form thumbnail affect the world camera.
- Photon 2.2.8 introduces lights/fog, while core lifecycle surfaces match 2.2.7. Keep version-sensitive access in Photon adapters and avoid new reflection unless a missing public contract is established.
- Keep `PhotonFormRenderer` a coordinator. Move clock/binding/anchor/post-effect responsibilities out rather than growing it into one large class.

## Verification

Observed in the NeoForge development client: 30 checks pass without shaders and the same 30 with Iris/Caz-Toon 1.2.1 active. These cover legacy defaults, serialization and host copying, actual Photon animation-track precedence/restoration, light/fog bindings, bounded/backward seeks, explicit pause, speed mapping, seed repeatability, CML attachment offsets/restoration, authored signal dispatch/suppression, payload isolation, panel construction/population, post-effect resolution and dead-host cleanup.

A separate GPU probe passes in both modes: an invert graph changes the cropped region from approximately `(0.2, 0.3, 0.4)` to `(0.8, 0.7, 0.6)`, leaves surrounding pixels unchanged, restores render/scissor flags and reports no OpenGL error. Nine existing registration/resource/persistence checks also pass. Development-only probes and output are ignored under `build/validation`; they are not shipped in the addon.

The full interactive acceptance matrix below remains a manual review, especially moving bodyparts, real film exports and multiple simultaneous forms. The probes exercise the integration surfaces but do not establish historical transform/parameter reconstruction or universal shader compatibility.

Compile after each stage. Check old form/film round trips and defaults, typed keyframes, override removal and conflicting timeline tracks. Exercise two concurrent instances of the same FX with different settings to detect shared-definition mutations.

In a loaded client world, test world playback, form-editor open/close, F7, bodyparts/model blocks, forward/backward seeking, pause/resume, resource reload and disconnect. Check signals fire once during forward playback and never during catch-up. Check light/fog providers, executor caches and post-effect requests are released on stop.

Repeat rendering checks with Iris shaders enabled and disabled. Check preview/world post-effect isolation and export frame stepping. Record actual results and remaining limits; do not substitute compilation for interactive acceptance.

## Sources

- [CML addon contract](../../../BBS-CML-3.0/docs/ADDONS.md) (sibling repository)
- [Runtime data injection](https://low-drag-mc.github.io/LowDragMC-Doc/en/photon2/java-api/runtime-data-injection.html)
- [Custom executors, timeline and signals](https://low-drag-mc.github.io/LowDragMC-Doc/en/photon2/java-api/custom-executors-timeline-and-signals.html)
- [Runtime objects and transforms](https://low-drag-mc.github.io/LowDragMC-Doc/en/photon2/java-api/runtime-objects-and-transforms.html)
- [Post-processing API](https://low-drag-mc.github.io/LowDragMC-Doc/en/photon2/java-api/post-processing-api.html)
