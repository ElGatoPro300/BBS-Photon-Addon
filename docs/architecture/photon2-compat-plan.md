# Photon 2.2.x compatibility plan

## Status

| Field | Value |
|-------|--------|
| **Current phase** | **B complete** — Photon `2.2.7` + LDLib2 / KilaGraph pins compile green |
| **Next phase** | **C** — bridge extraction + FX lifecycle / stacking investigation |
| Target artifact | Photon2 / photon-editor **`mc1.21.1-2.2.7-neoforge`** |
| Line | `2.2.x` (not a new major) |
| Host | BBS CML Edition (current NeoForge 1.21.1 pin in this repo) |
| Loader policy | NeoForge + Connector only (no Fabric-primary port in this plan) |
| Effect formats | Keep **`.fx`**; add **`.fxpack`** listing + load path |
| Agent model hint | See [Agent model guidance](#agent-model-guidance) |

Gradle Phase B pins target **`mc1.21.1-2.2.7-neoforge`** for Photon, with matching LDLib2 / KilaGraph versions listed below. Keep later dependency edits explicit and scoped (FEATURE-RULES: no invented bumps).

### Current gap (`.fxpack`)

Today `UIPhotonFormPanel.populateEffects` only enumerates:

- BBS resource links ending in `.fx` under `photon/…`
- Files ending in `.fx` under `ldlib2/assets/photon/fx`

There is **no** `.fxpack` discovery or load path yet. Runtime load goes through Photon helpers (`FXHelper.getFX`) keyed by the string stored in `photon_fx`. Phase A findings: pack-backed effects use the **same** `namespace:path` id scheme once Photon mounts the pack as a resource pack — no new Value id required.

---

## Goals

1. **Photon 2.2.7 compatibility** — make **BBS Photon Addon** work with Photon Editor / Photon2 **`2.2.x`** on NeoForge 1.21.1.
2. **`.fxpack` support** — discover, pick, and play Photon effects packaged as **`.fxpack`** (in addition to legacy loose **`.fx`** files).
3. **FX lifecycle integrity** — after adapting to `2.2.7`, verify and fix FX stacking / incomplete teardown (reported especially on **model blocks**; may affect other hosts too). Folded into **Phase C**, not a pre-bump hotfix.

---

## Non-goals

- Rewriting BBS CML Edition
- Supporting Photon 1.x or unrelated forks
- Inventing a Fabric-only distribution
- Renaming `photon_fx` / `paused` Value ids (unless a separate migration is approved)
- Replacing `.fx` support (packs are additive unless Photon itself deprecates loose `.fx`)
- Implementing pause via `FXRuntime.setRate(0)` before a solid conclusion (explore in C; no code until verified)

---

## Current known hotspots

Code most likely to break or need adapters when moving `2.2.1` → `2.2.7`, and when adding packs:

1. `PhotonFormRenderer` — `com.lowdragmc.photon.client.fx.*` (`FXHelper`, `EntityEffectExecutor`, `FXRuntime`, emitters)
2. Inner `PausableEntityEffectExecutor` — overrides of tick/frame update APIs; candidate to move behind bridge / pause controller
3. `UIPhotonFormPanel` — LDLib2 FX directory layout (`ldlib2/assets/photon/fx`); **extend for `.fxpack`** (prefer Photon listing APIs where possible)
4. Any reflective access to Photon or BBS film classes
5. Iris interaction if Photon changes render entry points
6. Photon pack mount / `.fxpack` (`com.lowdragmc.photon.client.fx.fxpack.FXPacks`)
7. **KilaGraph** — required by recent Photon `2.2.x` lines; Phase B must confirm Modrinth/maven pin and that NeoForge resolves it (already present on older pin; bump with Photon)

Domain `PhotonForm` should remain untouched for a pure API bump. Phase A confirmed: **no new Values** — pack entries reuse `photon_fx` string ids.

---

## Agent model guidance

Announce **current / next phase** at the start and end of each phase. Suggested agent strength:

| Phase | Strength | Why |
|-------|----------|-----|
| **A** Inventory / docs | Medium (current) | Research + write-up only |
| **B** Dependency bump + compile | Medium | Mostly pin + fix compile errors |
| **C** Bridge extract + leak investigate/fix | **Strong** | Lifecycle races + renderer extraction; easy to regress pause/scrub |
| **C2** `.fxpack` UX | Medium | Listing + picker + L10n |
| **D** In-game verification | Human + medium checklist | `runClient` / Iris on-off cannot be fully automated |
| **E** Docs / release | Medium | Changelog and cross-links |

---

## Phased approach

### Phase A — Inventory (no behavior change) — **DONE**

- [x] Diff Photon `2.2.1` vs `2.2.7` (public APIs used by this addon + changelogs / Photon `1.21` sources).
- [x] Inventory **`.fxpack`** support in `2.2.7`: layout, mount, list/load, id strings.
- [x] List likely compile / behavior breakages against this addon’s imports (see API diff log).
- [x] Record findings in **API diff log** and **`.fxpack` notes** below.
- [x] Decide bridge interface shape (`IPhotonFxBridge`) — see below.
- [x] Decide picker id scheme: keep existing `namespace:path` in `photon_fx` (packs included once mounted).
- [x] Document FX stacking leak hypotheses and schedule investigation in **Phase C** (post-`2.2.7` adapt).
- [x] Note `FXRuntime.setRate(0)` as pause alternative to explore in C (no implementation until concluded).
- [x] Note KilaGraph / LDLib2 auto-bump policy for Phase B.

### Phase B — Dependency bump + compile green

- [x] Explicit maintainer approval to edit `build.gradle` / `gradle.properties`.
- [x] Pin Photon to `mc1.21.1-2.2.7-neoforge`.
- [x] **Auto-bump** LDLib2 and **KilaGraph** to matching recent NeoForge pins:
  - LDLib2: `mc1.21.1-2.2.41-neoforge`
  - KilaGraph: `mc1.21.1-21.1.0.15-neoforge`
- [x] Fix compile errors **behind bridges** where possible; avoid drive-by refactors unrelated to the bump.
  - No Java compile fixes were required after the dependency bump.
- [x] Update this file’s dependency matrix with the exact pins that compiled.
  - `./gradlew build` succeeded on Phase B pins.

### Phase C — Extract while adapting + FX lifecycle (recommended with / after B)

- [ ] Introduce `IPhotonFxBridge` / `PhotonFxBridge` (start/stop/load for `.fx` and mounted `.fxpack` ids).
- [ ] Move pause/scrub freeze out of the god-file where touched by API changes.
- [ ] **Explore** `FXRuntime.setRate(0)` vs current frozen-age pause; implement only after a solid conclusion (document choice here).
- [ ] Keep `PhotonFormRenderer` as coordinator; do not increase its LOC net-positive without extraction.
- [ ] **FX stacking / leak (post-`2.2.7`)**: re-test especially on **model blocks**; if still present, fix lifecycle inside the bridge/cleanup path (hypotheses below). Do **not** pre-fix on `2.2.1` as a separate B0 — prefer investigating after API adaptation so behavior matches the target stack.

### Phase C2 — `.fxpack` UX (may overlap C)

- [ ] Extend effect discovery (`populateEffects` / shared helper): use the hybrid decision below — `FXHelper.listAllFX()` for loadable ids plus folder UX for loose FX / pack installation paths.
- [ ] Ensure form editor picker + `UIPhotonFxKeyframeFactory` can select pack-backed ids.
- [ ] Open-folder / docs hints for `photon/fxpacks` (and keep `ldlib2/assets/photon/fx` for loose `.fx`).
- [ ] L10n keys if new UI copy is needed (`IKey` / `bbs_photon` strings).

### Phase D — In-game verification

- [ ] World playback of Photon forms on stubs / actors
- [ ] **Model blocks** with Photon forms: no stacking / leftover particles after remove/switch
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
- Phase D checklist completed (including model-block stacking check)
- No new mutable global render state
- Renderer debt not worsened (net LOC in `PhotonFormRenderer` flat or down)

---

## API diff log (Phase A)

Sources: addon pin `mc1.21.1-2.2.1-neoforge`; Photon `1.21` branch sources / CHANGELOGS through **v2.2.7**; Modrinth NeoForge artifacts confirm `2.2.7-neoforge` exists. Exact jar-to-jar bytecode diff deferred to Phase B when both artifacts are on the classpath.

| Area | 2.2.1 behavior / symbol (addon usage) | 2.2.7 change | Addon action |
|------|----------------------------------------|--------------|--------------|
| `FXHelper.getFX(ResourceLocation)` | Load `assets/<ns>/fx/<path>.fx`; cache by id | Same contract; warns if `.fx` still embeds deprecated `resources` section (assets belong in `.fxpack`) | Keep resolving `photon_fx` strings via bridge |
| `FXHelper.listAllFX()` | Not used by addon (listing API cached ~2.2.4s+) | Returns every loadable FX id from mods, resource packs, ldlib2 inject, and **mounted `.fxpack`s** | Prefer for C2 discovery |
| `FXHelper.clearCache()` | Exists / reload-related | Still clears FX + listing caches on resource reload | Rely on Photon reload; do not invent parallel caches |
| `EntityEffectExecutor` | `CACHE`, ctor `(FX, Level, Entity, AutoRotate)`, `start()` | Same surface + `retire` / `runtimeEnded` / `shouldSkipStart` / `allowMulti` / `onFinished` via `FXEffectExecutor` | Bridge must **destroy + retire** on stop; do not rely on abandoned CACHE entries |
| `FXRuntime` | `destroy(boolean)`, `isAlive()`, object tree | + `setRate(float)`, richer `isValid()` / timeline finish semantics | Explore `setRate(0)` for pause in C; keep age-freeze until concluded |
| `PausableEntityEffectExecutor` (addon) | Overrides tick/frame; freezes emitter ages | May need revalidation vs `setRate` / `isValid` | Extract with bridge; do not grow renderer |
| Iris | Addon restores GL state carefully | Photon 2.2.2+ more Iris compact | Phase D on + off |
| KilaGraph | Already declared on older pin | Photon 2.2.7 changelog: bump ldlib2 **and** kilagraph | Phase B auto-bump + verify resolve |
| LDLib2 | `mc1.21.1-2.2.29-neoforge` in `gradle.properties` | Photon 2.2.7 bumps ldlib2 | Auto-bump with Photon pin |
| Compile risk | Imports: `FX`, `FXHelper`, `EntityEffectExecutor`, `FXRuntime`, `IFXObject`, `Emitter` | Package paths appear stable on `1.21` line; behavior/lifecycle helpers grew | Expect mostly behavioral fixes, not wholesale renames — confirm at B compile |

### Likely behavioral breakages (not necessarily compile errors)

- Async `Minecraft.execute` stop vs immediate null of `currentEffect` → stacked executors if start races stop.
- Nulling `currentEffect` when dummy entity is removed **without** `destroy`/`CACHE` retire → orphaned FX.
- New dummy `ArmorStand` per restart defeats Photon’s per-entity `allowMulti` dedup (old entity bucket may linger).
- `isAlive()` / finish semantics with timelines may differ from older “particle still playing” assumptions.

---

### `.fxpack` notes (Phase A)

| Topic | Finding | Addon action |
|-------|---------|--------------|
| File location(s) | User: `<game>/photon/fxpacks/*.fxpack`. Mods: jar-root `fxpacks/` (folder or zip). Cache: `<game>/photon/fxpack_cache` for nested mod zips. | Document in UI/docs (C2/E); optional open-folder helper for `photon/fxpacks` |
| Mount / load | `FXPacks.repositorySource()` mounts packs as **hidden always-on lowest-priority** resource packs. Contents are standard `assets/<ns>/fx/<path>.fx` (+ packed assets). | No custom deserializer required for playback |
| Public list/load API | **Load:** `FXHelper.getFX(id)` (same as loose `.fx`). **List all mounted FX:** `FXHelper.listAllFX()`. **List ids inside one file:** `FXPacks.listFx(File)`. Dir helper: `FXPacks.getFxPacksDir()`. | Bridge `load`/`listEffectIds`; C2 picker prefers `listAllFX` |
| Effect id string format | `namespace:path` — omits `fx/` prefix and `.fx` suffix (same as today). | Store unchanged in `photon_fx` |
| Compatibility with existing `photon_fx` values | Legacy loose `.fx` ids keep working. Pack-only effects appear after pack mount + resource reload (F3+T). | No Value migration |
| Relation to loose `.fx` | Additive. Editor/export path prefers packs for shipping assets; loose files under ldlib2 still load. | Keep `.fx` scan or subsume via `listAllFX` |

---

## Proposed bridge shape (implement in Phase C; design locked in A)

Adapter-only. Domain stays free of Photon imports.

```text
IPhotonFxBridge
  load(effectId) -> FX | null          // FXHelper.getFX; covers .fx + mounted .fxpack
  start(effectId, level, hostEntity) -> handle | null
  stop(handle)                         // destroy(force) + retire from EntityEffectExecutor.CACHE; client-thread safe
  setPaused(handle, paused)            // age-freeze and/or setRate(0) after C conclusion
  isAlive(handle) -> boolean
  listEffectIds() -> List<String>      // prefer FXHelper.listAllFX(); used by C2 picker
```

- One implementation class (e.g. `PhotonFxBridge`) tied to the pinned Photon `2.2.x`.
- `PhotonFormRenderer` becomes a **coordinator** (resolve id → ensure executor → pause → transform → cleanup), not the owner of Photon API details.
- Do not leave duplicate start/stop logic in the renderer after extraction (FEATURE-RULES Part 2).

---

## FX stacking / leak hypotheses (investigate in Phase C)

**Reporter context:** stacking observed on **model blocks** with Photon particle forms under BBS CML 2.x; other surfaces (film, form editor, world stubs) not fully ruled out.

Hypotheses in current `PhotonFormRenderer` (pre-`2.2.7` code):

1. **Abandon without destroy** — if `dummyEntity.isRemoved()`, code nulls `currentEffect` without `runtime.destroy` / CACHE retire.
2. **Async stop race** — `stopCurrentEffect` schedules destroy on the client queue but clears local refs immediately; a following `startEffect` can stack a second executor.
3. **Per-entity CACHE buckets** — each restart may spawn a new ArmorStand; Photon’s `allowMulti` dedup does not merge across old hosts, so orphans keep ticking/rendering.
4. **Watchdog limits** — `checkCleanup` (~100 ms without `render3D`) does not cover cases where render keeps running while restarts fail to tear down prior FX.

**Policy:** do not land a surgical `2.2.1`-only hotfix as Phase B0. After Phase B+C adaptation to `2.2.7`, re-test model blocks; if stacking persists, fix inside bridge/cleanup with strong-agent scrutiny.

---

## Picker discovery options (C2)

Two approaches (not mutually exclusive):

| Option | What it does | Pros | Cons |
|--------|----------------|------|------|
| **A — `FXHelper.listAllFX()`** | Ask Photon for every FX id currently loadable (mods, RPs, ldlib2, **mounted `.fxpack`s**) | One API; packs "just appear" after F3+T; matches playback `getFX` | May list FX the author did not expect; depends on Photon cache/reload |
| **B — Manual folder scan (today)** | Walk BBS links + `ldlib2/assets/photon/fx/*.fx` (+ optionally `photon/fxpacks` via `FXPacks.listFx`) | Familiar; can open folders in Explorer | Misses pack-mounted ids unless extended; duplicates Photon’s own discovery |

**C2 decision:** use a **hybrid**. The selectable effect list should prefer `FXHelper.listAllFX()` so effects from mods, resource packs, LDLib2, and mounted `.fxpack`s all share the same runtime source of truth. The UI should still offer folder actions for authoring/import workflows, especially:

- loose `.fx`: `ldlib2/assets/photon/fx`
- packs: `photon/fxpacks`
- any additional Photon-owned install folder exposed by public API (prefer `FXPacks.getFxPacksDir()` over hardcoded paths when available)

This lets users load effects/packs that are not strictly authored inside the old LDLib2 loose-FX folder, while keeping playback ids aligned with `FXHelper.getFX()`.

---

## Pause exploration note (Phase C)

Photon `2.2.7` adds `FXRuntime.setRate(float)` (0 = frozen clock + objects). Candidate replacement or complement for the addon’s emitter age-freeze in `PausableEntityEffectExecutor`.

- **Explore** during C (compare scrub/film pause, model blocks, Iris).
- **Do not implement** until the conclusion is written here and signed off.
- Fallback remains current age-freeze + `setDelay` behavior.

---

## Related

- Architecture rules: `.cursor/rules/FEATURE-RULES-ARCHITECTURE.mdc` §4
- Renderer: [photon-renderer.md](photon-renderer.md)
- Editor / picker: [ui-photon-editor.md](ui-photon-editor.md)
- Organization: [code-organization.md](code-organization.md)
