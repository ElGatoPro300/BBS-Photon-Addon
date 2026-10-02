# Photon 2.2.x compatibility plan

## Goals

Two related objectives for this roadmap:

1. **Photon 2.2.7 compatibility** — make **BBS Photon Addon** work with Photon Editor / Photon2 **`2.2.x`** on NeoForge 1.21.1.
2. **`.fxpack` support** — discover, pick, and play Photon effects packaged as **`.fxpack`** (in addition to legacy loose **`.fx`** files), using whatever APIs Photon `2.2.x` exposes for packs.

| Field | Value |
|-------|--------|
| Target artifact | Photon2 / photon-editor **`mc1.21.1-2.2.7-neoforge`** |
| Line | `2.2.x` (not a new major) |
| Host | BBS CML Edition (current NeoForge 1.21.1 pin in this repo) |
| Loader policy | NeoForge + Connector only (no Fabric-primary port in this plan) |
| Effect formats | Keep **`.fx`**; add **`.fxpack`** listing + load path |

Gradle today may still resolve an older pin (e.g. `2.2.1`). **Do not bump dependencies until maintainers explicitly start the implementation phase** of this plan (FEATURE-RULES: no invented bumps).

### Current gap (`.fxpack`)

Today `UIPhotonFormPanel.populateEffects` only enumerates:

- BBS resource links ending in `.fx` under `photon/…`
- Files ending in `.fx` under `ldlib2/assets/photon/fx`

There is **no** `.fxpack` discovery or load path yet. Runtime load goes through Photon helpers (`FXHelper` / similar) keyed by the string stored in `photon_fx`. Phase A must document how `2.2.7` identifies and resolves pack entries so the picker and `PhotonFxBridge` can speak the same id scheme without breaking existing `photon:…` / `namespace:…` saves.

---

## Non-goals

- Rewriting BBS CML Edition
- Supporting Photon 1.x or unrelated forks
- Inventing a Fabric-only distribution
- Renaming `photon_fx` / `paused` Value ids (unless a separate migration is approved)
- Replacing `.fx` support (packs are additive unless Photon itself deprecates loose `.fx`)

---

## Current known hotspots

Code most likely to break or need adapters when moving `2.2.1` → `2.2.7`, and when adding packs:

1. `PhotonFormRenderer` — `com.lowdragmc.photon.client.fx.*` (`FXHelper`, `EntityEffectExecutor`, `FXRuntime`, emitters)
2. Inner `PausableEntityEffectExecutor` — overrides of tick/frame update APIs
3. `UIPhotonFormPanel` — LDLib2 FX directory layout (`ldlib2/assets/photon/fx`); **extend for `.fxpack`**
4. Any reflective access to Photon or BBS film classes
5. Iris interaction if Photon changes render entry points
6. **New:** Photon pack registry / `.fxpack` load API (id format, nested effects, folder vs archive)

Domain `PhotonForm` should remain untouched for a pure API bump. `.fxpack` may only need new id conventions inside the existing `ValueString photon_fx` (preferred) rather than new Values — confirm in Phase A.

---

## Phased approach

### Phase A — Inventory (no behavior change)

- [ ] Diff Photon `2.2.1` vs `2.2.7` artifacts (packages, `FXHelper`, executor constructors, pause APIs).
- [ ] Inventory **`.fxpack`** support in `2.2.7`: file layout, public load/list APIs, effect id strings, relation to loose `.fx`.
- [ ] List compile breakages against this addon’s imports.
- [ ] Record findings in the **API diff log** (and **`.fxpack` notes**) sections below.
- [ ] Decide bridge interface shape (`IPhotonFxBridge`) before editing the renderer bulk — include resolve/load for both `.fx` and `.fxpack` ids.
- [ ] Decide picker id scheme so old saved `photon_fx` strings keep working.

### Phase B — Dependency bump + compile green

- [ ] Explicit maintainer approval to edit `build.gradle` / `gradle.properties`.
- [ ] Pin Photon to `mc1.21.1-2.2.7-neoforge` (and adjust LDLib2 if required by that Photon build).
- [ ] Fix compile errors **behind bridges** where possible; avoid drive-by refactors unrelated to the bump.

### Phase C — Extract while adapting (recommended with B)

- [ ] Introduce `PhotonFxBridge` (start/stop/load for `.fx` and `.fxpack` ids).
- [ ] Move pause/scrub freeze out of the god-file where touched by API changes.
- [ ] Keep `PhotonFormRenderer` as coordinator; do not increase its LOC net-positive without extraction.

### Phase C2 — `.fxpack` UX (may overlap C)

- [ ] Extend effect discovery (`populateEffects` / shared helper) to list `.fxpack` (and nested entries if Photon requires it).
- [ ] Ensure form editor picker + `UIPhotonFxKeyframeFactory` can select pack-backed ids.
- [ ] Open-folder / docs hints updated if pack install path differs from `ldlib2/assets/photon/fx`.
- [ ] L10n keys if new UI copy is needed (`IKey` / `bbs_photon` strings).

### Phase D — In-game verification

- [ ] World playback of Photon forms on stubs / actors
- [ ] Form editor: pick effect, preview, open/close without duplicate FX
- [ ] Film timeline: `photon_fx` keyframes + scrub/pause
- [ ] `paused` Value honored
- [ ] Iris **on** and **off**
- [ ] Effect folder picker still lists FX from LDLib2 paths (`.fx`)
- [ ] **`.fxpack`**: list, select, preview in editor, play in world/film; legacy `.fx` ids still work

### Phase E — Docs / release

- [ ] Update this file’s status + dependency matrix
- [ ] Document `.fxpack` install / id format in `ui-photon-editor.md` (and README if useful)
- [ ] Update `AGENTS.md` / `README.md` version notes if needed
- [ ] Changelog entry for the addon release

---

## Definition of Done

- Compiles and runs on NeoForge 1.21.1 with Photon **`2.2.7`**
- Existing saved forms/films with `photon_fx` / `paused` still load
- Users can pick and play effects from **`.fxpack`** as well as **`.fx`**
- Phase D checklist completed
- No new mutable global render state
- Renderer debt not worsened (net LOC in `PhotonFormRenderer` flat or down)

---

## API diff log (fill during Phase A/B)

| Area | 2.2.1 behavior / symbol | 2.2.7 change | Addon action |
|------|-------------------------|--------------|--------------|
| *(pending)* | | | |

### `.fxpack` notes (fill during Phase A)

| Topic | Finding | Addon action |
|-------|---------|--------------|
| File location(s) | *(pending)* | |
| Public list/load API | *(pending)* | |
| Effect id string format | *(pending)* | |
| Compatibility with existing `photon_fx` values | *(pending)* | |

---

## Related

- Architecture rules: `.cursor/rules/FEATURE-RULES-ARCHITECTURE.mdc` §4
- Renderer: [photon-renderer.md](photon-renderer.md)
- Editor / picker: [ui-photon-editor.md](ui-photon-editor.md)
- Organization: [code-organization.md](code-organization.md)
