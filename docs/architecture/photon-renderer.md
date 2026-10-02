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
| Watchdog | Stops FX when render stalls; editor/entity paths stay aggressive (~100ms), world/model-block paths use a longer timeout to avoid false restarts |
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

- Switching form editor panels can duplicate particles if cleanup is too slow — the entity/editor watchdog exists for that.
- Model-block/world render paths can skip frames for longer than the editor path; do not apply the 100ms watchdog there or looping FX can be stopped and restarted while still in use.
- Stopping must release Photon runtimes; leaks show up as FX continuing after the form is gone.
- `activeRenderers` iteration on client tick must stay exception-safe.

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
