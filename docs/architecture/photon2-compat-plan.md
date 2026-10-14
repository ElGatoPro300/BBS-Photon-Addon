# Photon 2.2.x compatibility plan

The current host is CML 3.0 addon API 2. Photon is pinned to **2.2.8**, with LDLib **2.2.42** and KilaGraph **21.1.0.15**. See [CML migration](cml3-migration.md) for BBS API changes. The phases below record the historical 2.2.7 migration; the current adaptation is described next.

## Photon 2.2.8 adaptation

The [official changelog](https://github.com/Low-Drag-MC/Photon/blob/1.21/CHANGELOG.md) adds dynamic point/spot lights, shadows, volumetric lights and fog volumes, material presets, custom motion spaces, and trail/animation fixes. Comparing the published 2.2.7 and 2.2.8 sources found no changes to the eight core adapter surfaces: `FXRuntime`, `EntityEffectExecutor`, `FXEffectExecutor`, `FXHelper`, `FXPacks`, `IFXObject`, `Emitter`, and `IEffectExecutor`. Existing `.fx` and mounted `.fxpack` discovery/loading remain applicable; no new persistent Value or effect-id format is needed.

The pause adapter now uses `FXRuntime.setRate(0F)` instead of emitter age restoration. Root tick/frame callback guards prevent a `SpeedTrack` targeting root from overriding the pause. Resume restores both the previous runtime rate and root scale. Authored start delays are preserved at end-of-tick rather than replaced with the old `setDelayAll(0/1)` workaround. Paused-host death still destroys the runtime and retires its executor; Photon handles light/fog provider detachment. These calls use public Photon APIs, without new production reflection.

Validation: Java 21 Gradle build passes. An attached development-only probe in a loaded NeoForge client world passes 12 runtime checks without shaders and the same checks with Iris/Caz-Toon 1.2.1 active: light/fog age progression and freeze, timeline tick/frame freeze, root-speed tracks, delay preservation, rate restoration, and dead-host/provider/cache cleanup. Nine additional checks pass for registration, persistence, editor construction, resources and keyframe/palette registration. A separate probe passes light/fog/emitter playback and pause/resume through actual global-engine ticks and render frames both with and without shaders. Probe sources and logs live in ignored `build/validation`. Full interactive form-editor, film-scrub, bodypart/model-block and visual shader review remain manual acceptance checks; the probes do not replace them. Caz-Toon uses Photon's `ENCODED_TARGET` fallback: FX are composited after the pack passes and do not receive its bloom, DOF or fog. Arbitrary shader wall-clock/world-time animation is outside the pause contract, and arbitrary-position deterministic resimulation is not introduced.

## Historical 2.2.7 status

The subsequent parameter, film-clock, light/fog, anchor, signal and post-processing integration is documented in [Photon runtime features](photon-runtime-features.md), including the chosen override ownership and resimulation limits.

| Field | Value |
|-------|--------|
| **Current phase** | **C2 complete** — picker UX uses hybrid `FXHelper.listAllFX()` + folder actions |
| **Next phase** | **D** — in-game verification (legacy `.fx`, `.fxpack`, model-block stacking, pause/scrub, Iris) |
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

### Phase C — Extract while adapting + FX lifecycle — **DONE**

- [x] Introduce `IPhotonFxBridge` / `PhotonFxBridge` under new subpackage `elgatopro300.bbsphoton.client.render.photon` (start/stop + `FXHelper.listAllFX()` surface for C2).
- [x] Opaque `IPhotonFxHandle` — renderer no longer imports `com.lowdragmc.photon.*`.
- [x] Extract `PausableEntityEffectExecutor` out of the renderer god-file to its own adapter class (same age-freeze behavior; no breakage).
- [x] Extract `BbsFilmPauseProbe` — isolates the UIDashboard / UIFilmPanel / runner reflection; renderer keeps the 1s cache.
- [x] Keep `PhotonFormRenderer` as coordinator — **LOC: 834 → 518 (−316)** while adding bridge wiring; well inside the ~800 soft limit.
- [x] **FX stacking / leak fixes applied inside the bridge** — see [C.4 leak fixes](#c4-leak-fixes-applied) below.
- [x] `FXRuntime.setRate(0)` evaluation documented in [C.5 pause conclusion](#c5-pause-conclusion) — **no code change this phase** per plan.

### Phase C2 — `.fxpack` UX (may overlap C)

- [x] Extend effect discovery via shared `PhotonFxDiscovery`: primary source is `PhotonFxBridge.get().listEffectIds()` (`FXHelper.listAllFX()`), with compatibility fallback for BBS resources and loose files under `ldlib2/assets/photon/fx`.
- [x] Ensure form editor picker + `UIPhotonFxKeyframeFactory` can select pack-backed ids. Both refresh on open (no extra UI cache; Photon already caches internally).
- [x] Open-folder UX:
  - loose `.fx`: `ldlib2/assets/photon/fx`
  - packs: `FXPacks.getFxPacksDir()` exposed through `PhotonFxBridge` (normally `photon/fxpacks`)
- [x] L10n keys added for all bundled string packs.

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

## C.4 leak fixes (applied)

Implemented inside `PhotonFxBridge` and the C-shaped `PhotonFormRenderer`. Mapped to the hypotheses:

1. **Abandon without destroy (fixed)** — `render3D` previously nulled `currentEffect` and `dummyEntity` when the dummy was removed, without destroying the runtime; the orphan kept ticking from `EntityEffectExecutor.CACHE`. Replaced with a call to `stopCurrentEffect()` → `bridge.stop(handle)` which runs `runtime.destroy(true)` **and** removes the executor from `EntityEffectExecutor.CACHE` by host entity.
2. **Async stop race (fixed)** — `bridge.stop` detects `Minecraft.getInstance().isSameThread()` and runs the destroy + retire **synchronously** on the client thread. The old code scheduled destroy via `Minecraft.execute(...)` and immediately nulled the local reference, so a subsequent `startEffect` could stack a second executor onto a brand-new ArmorStand host. Off-thread callers still route through `Minecraft.execute` for safety.
3. **Per-entity CACHE buckets (fixed in practice)** — because stop is now synchronous and always retires the old host from the CACHE, recreating the dummy ArmorStand no longer leaves prior buckets ticking.
4. **Watchdog limits (unchanged, still useful)** — the 100 ms `checkCleanup` watchdog still protects the "no more render3D" case. No new mutable global added; `activeRenderers` stays as pre-existing debt.

Also: `IPhotonFxHandle.isAlive()` now combines Photon `2.2.7` `isAlive() && isValid()`, so runtimes that the particle engine silently discards (level change, `/photon_client clear_particles`) are reported dead and the renderer restarts cleanly instead of leaking a stale handle.

---

## Lifecycle note — one-shot vs looping FX (Photon 2.2.x)

On Photon **`2.2.x`** (verified against `2.2.7`), emitter looping + emission interact more visibly than on older Photon builds used with earlier addon versions. Authors who remember effects like `photon:forcefield` *not* spawning a fresh layer every few seconds on older stacks should treat that as a **Photon/FX runtime change**, not a BBS form-Value change.

**What the FX file controls**

- Looping emitters with `duration` *D* and `startLifetime` ≫ *D* will emit again each cycle while older particles are still alive — e.g. forcefield: `duration=100` (~5 s), `looping=true`, burst count `1`, `startLifetime=10000`. Accumulation is **by design** of the `.fx` (also visible via Photon’s own `/photon fx … block` command, outside this addon). Tune interval/`duration` and particle lifetime in the Photon editor if a single sustained layer is desired.
- One-shot emitters (`looping=false`) finish when the runtime reports `isFinished()` — e.g. `photon:hanabi`.

**Addon policy**

- **One-shot:** when `FXRuntime.isFinished()`, the renderer stops the handle and sets `effectCompleted` so it does **not** auto-restart every finish cycle. Replay only after `photon_fx` id change or film backward seek.
- **Looping / long-lifetime FX:** the addon does **not** prune or rewrite emission; Photon plays the authored `.fx` as-is.

---

## C.5 pause conclusion (historical; superseded by 2.2.8 adaptation above)

`FXRuntime.setRate(float)` behavior reviewed from the Photon `1.21` `FXRuntime` source:

- `setRate(0)` sets `root.setSelfTimeScale(0)` **and** ticks `timelinePlayer` with rate 0 — it freezes the simulation **and** the timeline together.
- Scales **every** descendant via `IFXObject#timeScale()` inheritance — not just `Emitter`s. Our current `PausableEntityEffectExecutor` only freezes `Emitter.getAge()` and lets non-emitter objects (trails, beams, model particles, lights) keep advancing.
- Caveat (`FXRuntime` javadoc): an FX whose own timeline speed track targets root will overwrite the external rate for the ticks that track covers. For most authored FX without a speed track targeting root, `setRate(0)` is a complete freeze.

**Conclusion:** `setRate(0)` is the stronger pause and will be the default once validated in-game. Recommended future shape:

```text
setPaused(true):  runtime.setRate(0F); frozenAges snapshot kept as fallback
setPaused(false): runtime.setRate(1F); clear frozenAges
```

**Not implemented this phase** per plan directive (and because switching pause behavior is best verified against the model-block repro during Phase D). Follow-up ticket: swap `PausableEntityEffectExecutor` pause branch to `setRate(0)` once Phase D confirms no regression on scrub/pause/Iris, and drop the companion `setDelayAll(0/1)` dance from the renderer.

---

## Related

- Architecture rules: `.cursor/rules/FEATURE-RULES-ARCHITECTURE.mdc` §4
- Renderer: [photon-renderer.md](photon-renderer.md)
- Editor / picker: [ui-photon-editor.md](ui-photon-editor.md)
- Organization: [code-organization.md](code-organization.md)
