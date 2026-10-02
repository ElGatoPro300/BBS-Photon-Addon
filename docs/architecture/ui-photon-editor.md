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
  1. BBS resource links under `photon/…` ending in **`.fx`**
  2. Files under `ldlib2/assets/photon/fx` ending in **`.fx`** (creates the folder if missing; folder-open helper for Windows explorer)
- **Paused** — `UIToggle` writes `form.paused`.
- Labels via `L10n.lang("bbs_photon.ui.*")`.

**Planned:** discover and select Photon **`.fxpack`** packs (Photon `2.2.x`) in addition to loose `.fx` — see [photon2-compat-plan.md](photon2-compat-plan.md) Phase A / C2. Do not invent pack id formats until Phase A documents Photon’s API.

Do not start Photon runtimes from the panel. Preview rendering goes through `PhotonFormRenderer.renderInUI`.

## Keyframe factory

Registered in `BBSPhotonClient.init` by wrapping the global STRING factory:

- If the sheet id is `photon_fx` or ends with `/photon_fx` → `UIPhotonFxKeyframeFactory`
- Else → original string factory

This lets the film timeline pick Photon FX the same way as the form panel.

Host keyframe concepts: [BBS keyframes.md](https://github.com/ElGatoPro300/BBS-CML-EDITION/blob/master/docs/architecture/keyframes.md).

## Localization keys (examples)

Keys live under `assets/bbs_photon/strings/` (e.g. `en_us.json`, `es_es.json`, …):

- `bbs_photon.ui.panel_title`
- `bbs_photon.ui.pick_effect`
- `bbs_photon.ui.paused`
- `bbs_photon.ui.select_effect`

Add new keys for any new widget copy; keep all bundled languages in sync when possible.

## Hard rules

- No hardcoded English in panels.
- Do not rename `photon_fx` sheet id without migrating film data + factory matcher.
- Effect filesystem layout may change with LDLib2 / Photon `2.2.x` — verify paths when bumping (see [photon2-compat-plan.md](photon2-compat-plan.md)).
- When adding `.fxpack`, prefer extending a shared discovery helper rather than duplicating scan logic in the panel and keyframe factory.

## Related

- Form Values: [photon-form.md](photon-form.md)
- Registration: [bbs-addon-hooks.md](bbs-addon-hooks.md)
- Preview draw: [photon-renderer.md](photon-renderer.md)
