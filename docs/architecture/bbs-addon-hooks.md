# BBS addon hooks & client wiring

## Role

This document describes how the Photon addon plugs into BBS CML Edition. Functional contract (events, Values, UI patterns): [BBS CML ADDONS.md](https://github.com/ElGatoPro300/BBS-CML-EDITION/blob/master/docs/ADDONS.md).

## Entrypoints

From `fabric.mod.json` (also consumed under NeoForge via Connector):

| Entrypoint | Class |
|------------|--------|
| `bbs-addon` | `elgatopro300.bbsphoton.BBSPhotonAddon` |
| `bbs-addon-client` | `elgatopro300.bbsphoton.client.BBSPhotonClientAddon` |

NeoForge bootstrap lives in `BBSPhoton` / `BBSPhotonClient` and registers the BBS addon instances on the BBS event bus when needed.

Depends on: `bbs`, `photon` (and transitive LDLib2 at runtime via gradle).

## Common / server — `BBSPhotonAddon`

Implements `BBSAddon`:

- `registerForms(RegisterFormsEvent)` → `event.getForms().register(Link.create("bbs:photon"), PhotonForm.class)`

Keep this class free of client-only Photon rendering.

## Client — `BBSPhotonClientAddon`

Implements `BBSClientAddon`:

| Event | Action |
|-------|--------|
| `RegisterFormsRenderersEvent` | `PhotonFormRenderer` + `UIPhotonForm` panel |
| `RegisterIconsEvent` | Photon form icon (`PhotonFormRenderer.ICON`) |
| `RegisterL10nEvent` | `bbs_photon` string packs under `assets/bbs_photon/strings/` |
| `RegisterDashboardPanelsEvent` | Reserved / empty |
| `RegisterFormCategoriesEvent` | Deferred — see client init note below |

## Client — `BBSPhotonClient`

Additional wiring that is awkward or unsafe inside the event alone:

1. **Keyframe factory override** — wraps `UIKeyframeFactory.FACTORIES` for `KeyframeFactories.STRING` so sheets named `photon_fx` (or `*/photon_fx`) use `UIPhotonFxKeyframeFactory`.
2. **Replay editor chrome** — reflection into `UIReplaysEditor` COLORS / icon maps for track appearance (debt: isolate if BBS adds a public API).
3. **Form category registration** — timed on `ClientLifecycleEvents.CLIENT_STARTED` so it is not overwritten by `BBSResources.init()`.
4. **Tick cleanup** — `ClientTickEvents` walks `PhotonFormRenderer.activeRenderers` / `checkCleanup()`.

When changing these hooks, re-test: form picker category, film timeline `photon_fx` track UI, and FX cleanup when closing the form editor.

## Localization

- BBS editor strings: `assets/bbs_photon/strings/<lang>.json` via `RegisterL10nEvent`
- Vanilla-style lang (if present): `assets/bbs_photon/lang/`

UI must use `L10n.lang("bbs_photon....")` / `IKey` — no hardcoded English labels.

## Hard rules

- Register through BBS events whenever possible.
- Do not fork BBS registration by editing host mod sources.
- Dependency bumps (BBS or Photon) are explicit maintainer requests only.

## Related

- Form: [photon-form.md](photon-form.md)
- UI: [ui-photon-editor.md](ui-photon-editor.md)
- Renderer cleanup: [photon-renderer.md](photon-renderer.md)
