# AGENTS.md — BBS Photon Addon

This file provides guidance to all AI assistants (Cursor, Antigravity, Claude Code, Codex, Copilot) and developers working in this repository.

## Project Overview

**BBS Photon Addon** integrates [Photon Editor / Photon2](https://github.com/LowDragMC/Photon) particle FX into [BBS CML Edition](https://github.com/ElGatoPro300/BBS-CML-EDITION) as a first-class **form**. Authors can pick a Photon FX id, pause/scrub it with the film editor, and use it on replays like other BBS forms.

This repository is intentionally small and open-source so it can serve as a **reference addon** for people (and AIs) learning the BBS addon API.

### Runtime target

- **Minecraft**: 1.21.1
- **Loader**: NeoForge (Sinytra Connector + Forgified Fabric API for Fabric-shaped entrypoints)
- **Host**: BBS CML Edition
- **FX engine**: Photon Editor / Photon2 **`2.2.x`** (compatibility goal: `mc1.21.1-2.2.7-neoforge`)
- **Effect formats**: legacy **`.fx`** today; planned **`.fxpack`** support (see `docs/architecture/photon2-compat-plan.md`)
- **Assets**: LDLib2 Photon FX folder (`ldlib2/assets/photon/fx`)

Do **not** invent a Fabric-only primary target unless the developer explicitly asks.

## Mandatory Engineering Standards (Read First)

ALL AI agents and developers **MUST** follow the two-part Feature Development Rules before writing, proposing, or modifying any code:

- **Part 1: Planning Protocol & AI Interaction**
  - Cursor: `.cursor/rules/FEATURE-RULES-PROTOCOL.mdc`
  - Antigravity / Generic: `.agents/rules/feature-rules-protocol.md`
  *(Phase 0–4 planning, NO code on first turn, search before create, interrogation)*

- **Part 2: Architecture & Dependency Standards**
  - Cursor: `.cursor/rules/FEATURE-RULES-ARCHITECTURE.mdc`
  - Antigravity / Generic: `.agents/rules/feature-rules-architecture.md`
  *(Domain/Adapter separation, DRY, zero mutable global state, Photon/BBS adapters, LOC limits)*

Also keep open:

- `.cursor/rules/PROJECT-STRUCTURE.mdc` (or `.agents/rules/project-structure.md`) — short feature map
- `docs/architecture/README.md` — deep-dive index
- Host addon contract (functional): [BBS CML `docs/ADDONS.md`](https://github.com/ElGatoPro300/BBS-CML-EDITION/blob/master/docs/ADDONS.md)

## Build Commands

```bash
./gradlew build          # Build the project
./gradlew runClient      # Launch Minecraft client with the mod (primary testing method)
./gradlew runServer      # Launch Minecraft server
./gradlew jar            # Create JAR artifact only
./gradlew clean          # Clean build directory
```

There is no automated test suite — testing is done in-game via `./gradlew runClient`.

**Do not modify gradle configuration** unless the developer explicitly asks (dependency bumps for Photon `2.2.7` are an explicit ask).

## Code Style (from CONTRIBUTING.md — strictly enforced)

- Braces on new lines: `if (condition)\n{\n    ...\n}`
- All instance field/method access must use `this.`
- Max ~150 lines per method
- Soft limit ~600–800 lines per class (zero *new* god objects; extract from `PhotonFormRenderer` instead of growing it)
- Float/Double/Long literals require capital suffixes: `1F`, `2D`, `3L`
- No `var` keyword; no redundant generic type args: use `new ArrayList<>()` not `new ArrayList<String>()`
- Comments in English; inline body comments use `/* */` not `//`
- Field/method order per class: constants → static fields → instance fields → static constructor → static methods → constructors → instance methods
- No pure AI-generated code (must be understood and manually tested)

## Architecture Snapshot

| Area | Package / class | Layer |
|------|-----------------|--------|
| Form data | `elgatopro300.bbsphoton.forms.PhotonForm` | Domain |
| Common registration | `BBSPhotonAddon` | Adapter (BBS events) |
| Client registration | `BBSPhotonClientAddon`, `BBSPhotonClient` | Adapter |
| Renderer / FX | `client.render.PhotonFormRenderer` | Adapter (Photon + MC) — **debt: oversized** |
| Editor UI | `client.gui.UIPhotonForm*` | Adapter (BBS UI) |

Entrypoints (`fabric.mod.json` / NeoForge metadata): `bbs-addon`, `bbs-addon-client`.

## Related Repos

- Host mod: [BBS-CML-EDITION](https://github.com/ElGatoPro300/BBS-CML-EDITION)
- Greenfield host rewrite (architectural inspiration only): [BBS-CML-3.0](https://github.com/ElGatoPro300/BBS-CML-3.0)
- This addon: [BBS-Photon-Addon](https://github.com/ElGatoPro300/BBS-Photon-Addon)
