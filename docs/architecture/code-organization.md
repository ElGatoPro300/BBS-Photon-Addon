# Code organization (BBS Photon Addon)

Structural patterns for this addon. Code style (`this.`, braces, `/* */`, literals) is governed by `CONTRIBUTING.md`. FEATURE-RULES Part 2 enforces Domain/Adapter and size limits.

Inspiration (scaled down): [BBS CML 3.0 code organization](https://github.com/ElGatoPro300/BBS-CML-3.0/blob/master/docs/architecture/code-organization.md). Host addon API: [BBS CML ADDONS.md](https://github.com/ElGatoPro300/BBS-CML-EDITION/blob/master/docs/ADDONS.md).

---

## 1. What this repo is

A **thin bridge**:

```text
BBS CML Edition (forms, Values, UI, film)  ←→  this addon  ←→  Photon2 FX + LDLib2 assets
```

It is not a fork of BBS and not a fork of Photon. Prefer public APIs on both sides. Copying monolithic BBS renderers or Photon editor UI into this repo is out of scope.

---

## 2. Package layout (current)

NeoForge uses a single main source set. Client-only code still lives under a `client` package for clarity.

```text
elgatopro300.bbsphoton
├── BBSPhotonAddon.java         # BBSAddonMod — common form / resource events
├── forms/
│   └── PhotonForm.java         # DOMAIN — Values only
└── client/
    ├── BBSPhotonClient.java    # Client tick / shutdown callbacks
    ├── BBSPhotonClientAddon.java
    ├── gui/                    # Form editor + keyframe factory
    └── render/
        ├── PhotonFormRenderer.java   # ADAPTER — FX lifecycle coordinator
        └── photon/                  # Photon API and BBS render bridges
```

Resources: `src/main/resources/assets/bbs_photon/` (strings, textures, icon), `fabric.mod.json` (Connector discovery, NeoForge required). See [CML migration](cml3-migration.md).

---

## 3. Domain vs adapter

| Layer | Lives in | May import | Must not import |
|-------|----------|------------|-----------------|
| Domain | `forms/` | BBS `Form` / `Value*` | `net.minecraft.*`, `com.lowdragmc.photon.*` |
| Adapter | `client/**`, bootstrap | Minecraft, Photon, BBS client UI | — |

When Photon `2.2.x` APIs move, **only adapters change**. `PhotonForm` field ids stay stable for saved data.

---

## 4. Structural patterns (addon-scale)

### 4.1 Panels orchestrate Values

`UIPhotonForm` / `UIPhotonFormPanel` bind widgets to `photon_fx` and `paused`. They must not own `EntityEffectExecutor` / FX runtimes.

### 4.2 Renderer as coordinator (target shape)

Treat `PhotonFormRenderer` as a coordinator of named steps:

1. Resolve effect id from `PhotonForm` / keyframes  
2. Ensure / restart FX executor  
3. Apply pause / scrub freeze  
4. Update transforms for the host entity / preview  
5. Cleanup when unused  

Implement steps in focused helpers (`PhotonFxBridge`, pause controller, cleanup service) rather than more private methods on one 800+ LOC class.

### 4.3 Finished extractions

Runtime feature ownership is separated into `PhotonRuntimeController`, `PhotonRuntimeParameters`, `PhotonObjectAnchors` and `PhotonPostEffects`. `PhotonObjectCatalog` supplies read-only metadata to `UIPhotonRuntimePanel`; `forms/PhotonBinding`, `PhotonAnchor` and `PhotonValueList` hold host Values only. See [runtime features](photon-runtime-features.md).

If you introduce `PhotonFxBridge`, migrate **all** start/stop/pause call sites. Leaving duplicates in `PhotonFormRenderer` is prohibited (FEATURE-RULES Part 2 §2 / §5).

### 4.4 Registration via BBS events

Forms, renderers, panels, icons, and L10n register through BBS addon events (and documented client hooks). Do not bypass with silent static mutation of BBS internals except where already documented as debt (e.g. keyframe factory map put) — prefer public APIs when available.

---

## 5. Size heuristics

| Metric | Limit | Action |
|--------|-------|--------|
| Method | ~150 LOC | Extract named sub-routines |
| Class | ~600–800 LOC | Split collaborators |
| Responsibilities / class | 1 cohesive role | Split state vs render vs Photon API |

**Known exception / debt:** `PhotonFormRenderer` already exceeds the soft class limit. New Photon `2.2.7` work must shrink or extract it, not grow it.

---

## 6. Related docs

- Form data: [photon-form.md](photon-form.md)
- Renderer details: [photon-renderer.md](photon-renderer.md)
- Hooks: [bbs-addon-hooks.md](bbs-addon-hooks.md)
- UI: [ui-photon-editor.md](ui-photon-editor.md)
- Compat plan: [photon2-compat-plan.md](photon2-compat-plan.md)
