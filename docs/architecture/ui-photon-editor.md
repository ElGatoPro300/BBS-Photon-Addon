# Photon editor UI

## Role

Client UI that edits `PhotonForm` Values and customizes film keyframe editing for `photon_fx`.

| Class | Role |
|-------|------|
| `UIPhotonForm` | `UIForm<PhotonForm>` — registers default panel + standard form panels |
| `UIPhotonFormPanel` | Effect picker button + paused toggle bound to Values |
| `UIPhotonFxKeyframeFactory` | Keyframe editor widget for `photon_fx` string tracks |

## Form panel behavior

- **Pick effect** — opens `UIListOverlayPanel` listing FX ids from:
  1. Photon `FXHelper.listAllFX()` through `PhotonFxBridge` — primary source; includes loose `.fx`, mods, resource packs, LDLib2 resources, and mounted **`.fxpack`** ids.
  2. Compatibility fallback from BBS resource links under `photon/…` ending in **`.fx`**.
  3. Compatibility fallback from files under `ldlib2/assets/photon/fx` ending in **`.fx`** (recursive).
- **Folder actions** — two folder icons:
  1. Loose FX folder: `ldlib2/assets/photon/fx`
  2. FX pack folder: Photon `FXPacks.getFxPacksDir()` (normally `photon/fxpacks`)
- **Paused** — `UIToggle` writes `form.paused`.
- Labels via `L10n.lang("bbs_photon.ui.*")`.

Effect ids remain the same `namespace:path` strings stored in `photon_fx`; `.fxpack` simply mounts more `assets/<namespace>/fx/<path>.fx` resources into Photon.

Do not start Photon runtimes from the panel. Preview rendering goes through `PhotonFormRenderer.renderInUI`.

## Keyframe factory

Registered in `BBSPhotonClient.init` by wrapping the global STRING factory:

- If the sheet id is `photon_fx` or ends with `/photon_fx` → `UIPhotonFxKeyframeFactory`
- Else → original string factory

This lets the film timeline pick Photon FX the same way as the form panel.

The keyframe picker uses the same `PhotonFxDiscovery` helper as the form panel. It refreshes on open instead of keeping a separate static cache, so ids mounted after a resource reload (F3+T) can appear without restarting the editor.

Host keyframe concepts: [BBS keyframes.md](https://github.com/ElGatoPro300/BBS-CML-EDITION/blob/master/docs/architecture/keyframes.md).

## Localization keys (examples)

Keys live under `assets/bbs_photon/strings/` (e.g. `en_us.json`, `es_es.json`, …):

- `bbs_photon.ui.panel_title`
- `bbs_photon.ui.pick_effect`
- `bbs_photon.ui.paused`
- `bbs_photon.ui.select_effect`
- `bbs_photon.ui.open_fx_folder`
- `bbs_photon.ui.open_fxpack_folder`

Add new keys for any new widget copy; keep all bundled languages in sync when possible.

## Hard rules

- No hardcoded English in panels.
- Do not rename `photon_fx` sheet id without migrating film data + factory matcher.
- Effect filesystem layout may change with LDLib2 / Photon `2.2.x` — verify paths when bumping (see [photon2-compat-plan.md](photon2-compat-plan.md)).
- Keep effect discovery centralized in `PhotonFxDiscovery`; do not duplicate scan/list logic in the panel and keyframe factory.

## Related

- Form Values: [photon-form.md](photon-form.md)
- Registration: [bbs-addon-hooks.md](bbs-addon-hooks.md)
- Preview draw: [photon-renderer.md](photon-renderer.md)
