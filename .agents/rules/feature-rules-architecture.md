---
trigger: always_on
glob:
description:
---

# Feature Development Rules (Part 2/2) — Architecture & Dependency Standards

> [!IMPORTANT]
> **This is Part 2 of a 2-part engineering constitution for BBS Photon Addon.**
> - **Part 1**: `.agents/rules/feature-rules-protocol.md` — Planning protocol, AI agent behavior, and pre-implementation workflow.
> - **Part 2 (This document)**: `.agents/rules/feature-rules-architecture.md` — Architecture rules, centralization, state management, and Photon/BBS portability.
>
> **Both documents are mandatory and must be followed together for every code modification.**
>
> Cursor agents: use `.cursor/rules/FEATURE-RULES-ARCHITECTURE.mdc` (same content).

---

## §1 — Domain / Adapter Separation (High-Level vs Low-Level)

This addon sits between **BBS CML Edition** (host) and **Photon Editor / Photon2** (FX engine). Keep those concerns separated.

### Domain Layer (High-Level — Engine-Agnostic)

- Contains form data (`PhotonForm`), `Value` fields, and any pure helpers that do not call Minecraft or Photon APIs.
- **Rules**: Pure Java only. **Zero direct imports** from `net.minecraft.*`, `com.mojang.*`, `com.lowdragmc.photon.*`, Iris/Sodium packages, or NeoForge client render APIs.
- Domain code should remain stable across Photon `2.2.x` bumps and portable in spirit toward cleaner BBS 3.0 addon contracts.

### Adapter Layer (Low-Level — Engine / Photon-Specific)

- Contains `PhotonFormRenderer`, FX executors, UI panels that list Photon assets, LDLib path IO, client tick hooks, and any reflection bridges into Photon.
- **Rules**: All Photon-volatile and Minecraft-volatile code must be isolated behind interfaces or focused bridge/helper classes. Do not grow a single god-file renderer further.

```java
/* WRONG — Domain form directly constructs Photon FX */
public class PhotonForm extends Form
{
    public void startFx(Level level) { FXHelper.getFX(...); }
}

/* RIGHT — Domain holds data; Adapter runs Photon */
public class PhotonForm extends Form
{
    public final ValueString photon_fx = new ValueString("photon_fx", "");
}

public class PhotonFxBridge /* Adapter */
{
    public void start(String effectId, ...) { ... }
}
```

---

## §2 — Mandatory Centralization (DRY)

**If any logic or pattern appears in 2 or more places, extract it to a shared component.**

1. **Java Logic**: FX start/stop, pause/scrub freeze, effect-id resolution, and matrix/pose setup must live in shared `*Bridge`, `*Helper`, or `*Pipeline` classes — not copy-pasted between world `render3D` and UI thumbnail paths.
2. **Reflection**: Any reflective access to BBS or Photon internals must be centralized (one bridge), never scattered across UI + renderer.
3. **Search First**: Before creating helpers, search this repo and prefer BBS public APIs documented in [BBS CML ADDONS.md](https://github.com/ElGatoPro300/BBS-CML-EDITION/blob/master/docs/ADDONS.md).

---

## §3 — State Management & Code Boundaries

### §3.1 — Zero Mutable Global State

- **Prohibited**: New `public static` mutable fields for “current FX”, active colors, pause flags, or global render switches.
- **Allowed**: `public static final` immutable constants (e.g. icon `Link`).
- **Legacy debt**: `PhotonFormRenderer.activeRenderers` is known mutable global state used for cleanup. Do **not** add more globals of this kind. Prefer instance-scoped registries or explicit client services when refactoring.
- **Rule**: Rendering / FX parameters travel via method arguments or scoped context objects.

### §3.2 — Size and Responsibility Limits

- **Methods**: Maximum ~150 lines. Extract sub-routines with descriptive names.
- **Classes**: Soft limit of ~600–800 lines. Split multi-responsibility classes into focused components.
- **God Objects**: Zero tolerance for *new* god-files.
- **Known debt**: `PhotonFormRenderer` (~800+ LOC) currently mixes FX lifecycle, pause/scrub, reflection into BBS film UI, world render, and UI preview. Future Photon `2.2.x` work **must extract** (bridges / passes / services) instead of further inflating this class. Bugfixes may touch it surgically; features that enlarge it require a decomposition plan.

### §3.3 — Persistence & Localization

- **Data Persistence**: All persistent form fields **must** use the BBS `Value` system (`ValueString`, `ValueBoolean`, …). Never use raw fields with manual NBT/JSON for form data. Do not rename existing Value ids (`photon_fx`, `paused`) without a migration plan — they appear in saved films/forms.
- **UI Strings**: All user-facing text **must** use BBS `L10n` / `IKey` keys under `bbs_photon` string packs. Never hardcode English in panels.

---

## §4 — Dependency Portability (Photon / BBS / NeoForge)

This repository currently targets **NeoForge 1.21.1** with Sinytra Connector (Fabric API shims). It is **not** a multi-branch Fabric/NeoForge monorepo. Still isolate volatile APIs.

### §4.1 — Current Dependency Matrix (document changes when bumping)

| Dependency | Role | Notes |
|---|---|---|
| Minecraft / NeoForge | Runtime | 1.21.1 / NeoForge (see `gradle.properties`) |
| BBS CML Edition | Host mod / form API | Forms, Values, UI, film timeline |
| Photon Editor / Photon2 | FX engine | Target line **`2.2.x`**, preferred pin **`mc1.21.1-2.2.7-neoforge`** |
| LDLib2 | Photon asset / library host | FX files under `ldlib2/assets/photon/fx` |
| Iris / Sodium | Shader stack (runtime) | Test render paths with and without |

### §4.2 — Adapter Pattern for Photon API Drift

When Photon `2.2.x` changes FX helpers, executors, or package layout:

```java
/* Shared adapter surface inside this addon */
public interface IPhotonFxBridge
{
    void start(String effectId, ...);
    void stop();
    void setPaused(boolean paused);
}

/* Implementation tied to a Photon 2.2.x build */
public class PhotonFxBridge227 implements IPhotonFxBridge { ... }
```

Prefer public Photon APIs over reflection. If reflection is unavoidable, confine it to the bridge and document the target class/method names in `docs/architecture/photon-renderer.md`.

### §4.3 — Bump Protocol (Photon or BBS)

1. Classify modified files as domain (stable) or adapter (Photon/MC).
2. Update `gradle.properties` / `build.gradle` pins only when the developer explicitly asks.
3. Update `docs/architecture/photon2-compat-plan.md` with observed API diffs.
4. Compile and in-game test: world FX, form editor, film scrub/pause, Iris on/off.

### §4.4 — No Fabric-Only Target (Current Policy)

Do not invent a Fabric-primary build or dual-loader split unless the developer explicitly requests it. NeoForge + Connector is the supported runtime.

---

## §5 — Structural Patterns (from BBS 3.0 lessons, scaled to an addon)

Inspired by [BBS CML 3.0 code organization](https://github.com/ElGatoPro300/BBS-CML-3.0/blob/master/docs/architecture/code-organization.md), applied at addon scale:

1. **Panels orchestrate; they do not own FX engines.** `UIPhotonForm` / `UIPhotonFormPanel` edit Values and open pickers — they must not start Photon runtimes.
2. **Renderers are coordinators of passes/services**, not dumping grounds. Sequence named steps (resolve effect id → ensure executor → apply pause → draw / update frame).
3. **Finished extractions**: if you create `PhotonFxBridge`, migrate call sites fully; do not leave duplicate start/stop logic in the old class.
4. **BBS CML Edition is a functional host**, not a paste source: use addon events and public APIs; do not copy monolithic BBS renderers into this repo.

Detail: `docs/architecture/code-organization.md`.

---

## §6 — Architecture Quick Checklist

Before any commit or PR:

- [ ] Domain logic has zero direct Minecraft / Photon / Iris imports.
- [ ] No duplicated FX start/stop/pause logic across classes.
- [ ] No new `public static` mutable variables for render/FX state.
- [ ] Persistent data uses the BBS `Value` system; Value ids unchanged unless migrated.
- [ ] UI strings use `L10n` / `IKey`.
- [ ] Methods under ~150 lines; classes under ~800 lines (or an approved extraction plan for `PhotonFormRenderer`).
- [ ] Photon-volatile APIs isolated behind bridges/adapters.
- [ ] NeoForge-only policy respected (no invented Fabric ports).
