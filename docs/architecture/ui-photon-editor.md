# Photon editor UI

## Role

Client UI that edits `PhotonForm` Values and customizes film keyframe editing for `photon_fx`.

| Class | Role |
|-------|------|
| `UIPhotonForm` | `UIForm<PhotonForm>` — registers default panel + standard form panels |
| `UIPhotonFormPanel` | Effect picker button + paused toggle bound to Values |
| `UIPhotonRuntimePanel` | Foldable parameter, playback/signal, anchor and post-effect controls |
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

`UIPhotonRuntimePanel` adds parameter, playback/signal, anchor and post-effect panels. Property pickers read `PhotonObjectCatalog` and Photon's post-effect schema; typed controls edit BBS Values. The diagnostic monitor displays the current renderer's latest signal history, unresolved bindings and catch-up counter. See [runtime features](photon-runtime-features.md) for authoring instructions and synchronization limits.

The five Photon tabs use CML's native particle, gear, play, link and filter icons, respectively. Controls are grouped in native `UISection` widgets: effect/pause on the main tab; timing, seed and diagnostics on playback; an individual group for each parameter or anchor; and separate post-effect settings and parameter groups. Add-entry buttons remain outside the foldable groups. Explicit section IDs include the tab and entry Value ID so collapse preferences survive panel rebuilds and distinguish parameter lists from post-effect lists. Numbered titles reuse the existing localization keys across all bundled languages. UI controls continue to edit the same Values; section state belongs to CML's editor layout settings.

Verified in the NeoForge development client: all five tabs construct and display their icons; add/remove actions update parameter, anchor and post-parameter Values; fold state survives rebuilding; a header mouse event collapses and reflows its group; scrolling reaches the last parameter action. Screenshots and development-only probes are kept in ignored `build/validation`. No FX clock, renderer, dependency or saved form format changes are involved.

Effect ids remain the same `namespace:path` strings stored in `photon_fx`; `.fxpack` simply mounts more `assets/<namespace>/fx/<path>.fx` resources into Photon.

Do not start Photon runtimes from the panel. Preview rendering goes through `PhotonFormRenderer.renderInUI`.

## Keyframe factory

Registered through CML 3.0's `RegisterKeyframeEditorsEvent.registerProperty("photon_fx", ...)`. CML resolves bodypart property paths to their leaf name. Other string properties keep the host's standard editor.

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

All 22 languages listed in CML 3.0's `assets/bbs/assets/languages.json` are bundled with the same 42 addon keys: `ar_ar`, `az_az`, `de_de`, `en_us`, `es_es`, `fa_ir`, `fr_fr`, `he_il`, `hu_hu`, `id_id`, `ko_kr`, `pl_pl`, `pt_br`, `pt_pt`, `ru_ru`, `th_th`, `tr_tr`, `uk_ua`, `ur_pk`, `vi_vn`, `zh_cn`, and `zh_tw`. Files use UTF-8; preserve Unicode characters when editing through shell scripts. The addon registration retains the English fallback for languages supplied by external resource packs.

## Hard rules

- No hardcoded English in panels.
- Do not rename `photon_fx` sheet id without migrating film data + factory matcher.
- Effect filesystem layout may change with LDLib2 / Photon `2.2.x` — verify paths when bumping (see [photon2-compat-plan.md](photon2-compat-plan.md)).
- Keep effect discovery centralized in `PhotonFxDiscovery`; do not duplicate scan/list logic in the panel and keyframe factory.

## Related

- Form Values: [photon-form.md](photon-form.md)
- Registration: [bbs-addon-hooks.md](bbs-addon-hooks.md)
- Preview draw: [photon-renderer.md](photon-renderer.md)
