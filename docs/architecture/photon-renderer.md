# Photon form renderer

`PhotonFormRenderer` adapts the stable `PhotonForm` Values to live Photon FX. It extends CML 3.0's `FormRenderer<PhotonForm>` and implements `ITickable`.

## Responsibilities

| Component | Responsibility |
| --- | --- |
| `PhotonFormRenderer` | Effect-ID changes, one-shot completion, pause/seek coordination, dummy host and cleanup |
| `PhotonFxBridge` / `IPhotonFxHandle` | Photon 2.2.7 load/start/pause/stop/retire, effect discovery and root updates |
| `PausableEntityEffectExecutor` | Existing emitter age-freeze implementation |
| `BbsFilmPauseProbe` | Read the active dashboard's public film-panel state |
| `BbsRenderBridge` | Intermediary/Mojang access and transformed-stack placement |
| `BBSPhotonClient` | Tick callbacks and cleanup on world/process exit |

The renderer is approximately 510 lines following the CML migration. Reflection into private film fields and Minecraft signature access are no longer scattered through it. `activeRenderers` remains legacy mutable global state; no additional global registry was introduced.

## Render and timeline behavior

Picking-only passes do not start, reposition or keep FX alive. F7 exterior preview passes are ignored while the world/model-block pass owns playback. Toggling F7 restarts once to discard old world-space trails.

`ensurePlayback` reads explicit CML `previewTick` / `animationTick` values when available and falls back to entity age. Backward seeking resets one-shot completion and restarts the effect. Pause still combines the saved `paused` Value, film-panel state, timeline movement and the existing seek window. This migration does not introduce deterministic resimulation to arbitrary timeline positions.

The inherited CML renderer already applies form, overlay and bodypart transforms. The bridge uses that stack once instead of appending the form transform again. The palette thumbnail uses `bbs_photon:textures/photon_texture.png`.

## Lifecycle and shaders

Stop destroys and retires the Photon handle before releasing the dummy entity. The 100 ms watchdog stops abandoned effects. World exit and client shutdown also stop the active set. Missing effects release their dummy host; a completed one-shot does not loop until an ID change or backward seek. Authored looping emitters retain Photon's own emission behavior.

Photon still draws through its global engine, including its Iris integration. CML's shadow-pass guards and the addon picking guard do not replace visual checks with shaders enabled/disabled. Verify world playback, model blocks, bodyparts, form editor/F7, pause/scrub and disconnect cleanup in a compatible runtime.

See [CML migration](cml3-migration.md), [addon hooks](bbs-addon-hooks.md) and [Photon compatibility](photon2-compat-plan.md).
