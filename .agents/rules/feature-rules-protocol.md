---
trigger: always_on
glob:
description:
---

# Feature Development Rules (Part 1/2) — Engineering Protocol & AI Workflow

> [!IMPORTANT]
> **This is Part 1 of a 2-part engineering constitution for BBS Photon Addon.**
> - **Part 1 (This document)**: `.agents/rules/feature-rules-protocol.md` — Planning phases, AI interaction rules, and review process.
> - **Part 2**: `.agents/rules/feature-rules-architecture.md` — Domain/Adapter separation, DRY, state management, and Photon/BBS dependency portability.
>
> **Both documents are mandatory and must be followed together for every code modification.**
>
> Cursor agents: use `.cursor/rules/FEATURE-RULES-PROTOCOL.mdc` (same content).

---

## §0 — Scope and Enforcement

These rules apply to **every** code change — features, improvements, bugfixes, refactors — regardless of size.

- **Developers** must follow the planning protocol (§1) before writing code.
- **AI agents** must strictly obey agent rules (§2). An agent that skips planning or interrogation produces non-compliant code that must be rejected.
- **Exemptions**: Trivial changes (typo fixes, comment updates, single-line format fixes) are exempt from the full planning phases but must still adhere to Part 2 architecture rules.

---

## §1 — Mandatory Planning Protocol: Plan Before Code

**No implementation code may be written until planning phases are complete.** Jumping directly to coding is a root cause of Photon API breakages and BBS film/editor regressions.

### Phase 0 — Impact Analysis

Before proposing any implementation, answer these 5 questions:

1. **Which subsystems does this change touch?** Cross-reference with the feature map in `PROJECT-STRUCTURE.mdc` (or `project-structure.md`). List every affected subsystem explicitly (form data, renderer / FX lifecycle, UI editor, registration, keyframes, client tick/cleanup).
2. **Which classes/files will be modified?** If more than 5 files are affected, the change is architecturally significant and requires extra scrutiny.
3. **Does similar functionality already exist?** Search this repo for `*Utils`, `*Helper`, `*Bridge`, `*Pipeline`, and existing Photon FX helpers. Prefer extending BBS APIs (`Value`, `FormRenderer`, `UIFormPanel`, event registration) over reinventing them. If reusable code exists, extend it — never duplicate.
4. **What is the blast radius?** Could this break FX pause/scrub, form-editor cleanup, film timeline `photon_fx` tracks, Iris/shadow draws, or saved `PhotonForm` NBT/JSON? List all potential side effects.
5. **Is this change dependency-version-specific?** Does it touch Photon Editor / Photon2 APIs, LDLib paths, BBS form/render APIs, or NeoForge/Minecraft surfaces that differ across Photon `2.2.x` builds?

### Phase 1 — Blueprint (Design Document)

For any change touching more than 2 files or modifying more than a single method, produce a design blueprint before coding:

- **Problem statement**: What is being solved or added, and why.
- **Proposed approach**: High-level design and architectural pattern (prefer Domain data + Adapter bridge).
- **Affected classes and roles**: Exact responsibilities of each modified/created class.
- **Public API surface**: Signatures and contracts of new methods, fields, or interfaces.
- **Risks and trade-offs**: What could go wrong? What alternatives were considered?
- **Dependency impact**: How the design accommodates Photon `2.2.x` (target `2.2.7`) and the pinned BBS CML Edition version.

### Phase 2 — Interrogation of Unknowns

**Ambiguity must be resolved before implementation, not during.** Explicitly list:

- Assumptions being made and whether they have been verified.
- Unspecified edge cases (form editor open/close, film scrub, Actor Mode, body-part Photon forms).
- Behavior variations (vanilla vs Iris shaders; paused FX vs live world).

AI agents **must** present these questions to the developer and wait for answers before writing any code.

### Phase 3 — Incremental Implementation

- Implement in small, atomic steps. Each step must compile cleanly.
- After each significant step, verify that existing Photon FX playback and editor picking remain unbroken.
- Keep commits granular and descriptive (as mandated by `CONTRIBUTING.md`).

### Phase 4 — Verification and Review

- **In-game testing** via `./gradlew runClient` is mandatory (there are no automated tests).
- If rendering / FX is touched, test **with and without shaders** (Iris/Sodium on NeoForge).
- Verify form editor pick/pause, film timeline `photon_fx` keyframes, and world stub playback.
- Verify that changes do not introduce unintended diffs or formatting changes in unrelated files.

---

## §2 — Rules for AI Agents

These rules are **mandatory behavioral constraints** for any AI agent working on this codebase.

### §2.1 — No Code on First Turn

When requested to implement a feature, improvement, or non-trivial bugfix, the AI agent **must not** produce implementation code in its first response. It must:
1. Complete Phase 0 (Impact Analysis).
2. Present a concise Blueprint (Phase 1).
3. List clarifying questions (Phase 2).
4. Wait for developer approval before writing any code.

*Exception*: If the developer explicitly instructs to "skip planning" for a trivial fix, the agent may proceed directly.

### §2.2 — Mandatory Interrogation (Ask Before Assuming)

If the developer's prompt is ambiguous or underspecified, the agent must ask:
- Whether world playback, form editor preview, film timeline, or all of them should change.
- Desired behavior with vs without shaders.
- Whether a change could affect existing saved films/forms (`photon_fx` string ids).
- Never guess user intent on architectural decisions (e.g. reflection vs public Photon API).

### §2.3 — Search Before Create

Before creating any new class, method, or helper, the agent must actively search this addon and (when relevant) documented BBS addon contracts. Propose extending existing code before creating new files.

### §2.4 — Dependency-Version Awareness

When touching Photon FX, LDLib asset paths, mixins, or Minecraft render APIs, the agent must:
1. Identify which surfaces differ across Photon `2.2.x` (see Part 2 §4).
2. Explicitly document version differences in its response.
3. Propose adapter / bridge patterns to isolate Photon-volatile code.
4. Never assume code working against `2.2.1` will work unmodified on `2.2.7`.

### §2.5 — No Invented Ports

Do not port this addon to Fabric-only, other MC versions, or other Photon major lines unless the developer **explicitly** asks. When a Photon/`2.2.x` bump is requested, it requires API adaptation and manual testing — never blind dependency bumps.

### §2.6 — Respect Existing Architecture

Never introduce a parallel pattern if an established system already exists:

- Persistent form fields → BBS `Value` system.
- UI strings → `L10n` / `IKey` (no hardcoded English in panels).
- Registration → BBS `BBSAddon` / `BBSClientAddon` events.
- Rendering → `FormRenderer` + isolated Photon bridges (not ad-hoc static mutable globals).

### §2.7 — Surgical Bugfixes

Bugfixes must be minimal and localized. Never refactor unrelated code or alter public form Value ids during a bugfix unless fundamentally required. If deeper architectural debt is discovered (e.g. `PhotonFormRenderer` god-file), fix the immediate bug minimally and propose the refactor separately.

---

## §3 — Protocol Quick Checklist

Before any commit or PR:

- [ ] Completed Phase 0 Impact Analysis (subsystems, files, blast radius).
- [ ] Submitted Blueprint and obtained developer sign-off.
- [ ] Clarified all ambiguities and edge cases beforehand.
- [ ] Searched codebase for existing reusable helpers before adding new ones.
- [ ] Tested manually in-game (`./gradlew runClient`).
- [ ] Validated FX rendering with shaders ON and shaders OFF when rendering changed.
- [ ] Confirmed compliance with Part 2 (Architecture Standards).
