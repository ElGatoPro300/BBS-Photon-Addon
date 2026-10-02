# Photon form renderer (adapter)

## Role

`elgatopro300.bbsphoton.client.render.PhotonFormRenderer` extends BBS `FormRenderer<PhotonForm>` and implements `ITickable`. It is the **adapter** that turns `PhotonForm` Values into live Photon FX in:

- World / film stub rendering (`render3D`)
- Form editor / UI thumbnails (`renderInUI`)
- Client tick pause updates (`onClientTickStart`)
- Abandonment cleanup (`checkCleanup`, global `activeRenderers`)

## Responsibilities (today — monolithic)

| Concern | Notes |
|---------|--------|
| FX start/stop | `startEffect` / `stopCurrentEffect` via Photon `FXHelper` / `EntityEffectExecutor` |
| Pause & scrub | `PausableEntityEffectExecutor`, frozen ages, BBS film pause detection (reflection into dashboard / `UIFilmPanel`) |
| Dummy entity | Armor stand (or similar) as FX host when needed |
| Transforms | Pose / camera-relative placement for the form |
| Watchdog | Stops FX when render stalls (~100ms) to avoid duplicates when leaving the form editor |
| Icon link | `ICON` → `bbs_photon:textures/photon_texture.png` |

## Known architectural debt

1. **Class size** (~834 LOC) exceeds the soft ~600–800 limit.
2. **Mutable global**: `public static final List<PhotonFormRenderer> activeRenderers` — cleanup registry; do not add more globals of this kind.
3. **Reflection into BBS film UI** (`isBBSPaused`) — brittle across BBS versions; prefer a public BBS pause API if/when available, or a small isolated `BbsFilmPauseProbe` bridge.
4. **Photon API coupling** mixed with render math — extract `IPhotonFxBridge` before / during the `2.2.7` bump.

Future Photon `2.2.x` work **must extract** (see [code-organization.md](code-organization.md) §4.2) instead of adding more private methods here. Surgical bugfixes remain allowed.

## Iris / shaders

Follow host guidance: [BBS rendering-iris.md](https://github.com/ElGatoPro300/BBS-CML-EDITION/blob/master/docs/architecture/rendering-iris.md).

Practical rules for this addon:

- Avoid expensive or state-leaking work on Iris **shadow** passes when BBS marks them.
- After Photon draws, do not leave GL / RenderSystem state dirty for the next BBS form (blend, depth mask, shader color, texture unit 0). Prefer restoring via BBS helpers when available.
- Always verify FX in the film viewport **with and without** Iris after renderer changes.

## Cleanup & lifecycle pitfalls

- Switching form editor panels can duplicate particles if cleanup is too slow — the watchdog exists for that.
- Stopping must release Photon runtimes; leaks show up as FX continuing after the form is gone.
- `activeRenderers` iteration on client tick must stay exception-safe.
- One-shot FX: do not auto-restart after Photon `isFinished()` (`effectCompleted`); see [photon2-compat-plan.md](photon2-compat-plan.md) § Lifecycle note.
- Looping FX with `startLifetime` ≫ `duration` can accumulate particles by `.fx` design on Photon 2.2.x — not an addon restart loop.
- F7 exterior (`UIModelBlockPanel.toggleRendering`): the immersive editor keeps running a full-screen PREVIEW stencil/picking pass even when the in-UI form draw is suppressed. Ignore PREVIEW/`modelRenderer` `render3D` while `toggleRendering` is true so Photon FX is driven only by `MODEL_BLOCK`. Restart FX when the F7 flag itself toggles (not on every `FormRenderType` change) to clear World-space trails and replay one-shots without per-frame restart thrash.

## Extraction targets (recommended order)

1. `PhotonFxBridge` — load FX by id, start/stop executor  
2. `PhotonFxPauseController` — pause flag + scrub freeze ages  
3. `PhotonFxCleanupService` — replace bare static list  
4. `BbsFilmPauseProbe` — isolate reflection  

Document API diffs for Photon `2.2.7` in [photon2-compat-plan.md](photon2-compat-plan.md).

## Related

- Domain form: [photon-form.md](photon-form.md)
- Client hooks / ticks: [bbs-addon-hooks.md](bbs-addon-hooks.md)
- UI preview path: [ui-photon-editor.md](ui-photon-editor.md)
