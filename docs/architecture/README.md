# Architecture docs (agent / contributor reference)

Deep-dives for **BBS Photon Addon**. The always-on agent index lives in `.cursor/rules/PROJECT-STRUCTURE.mdc` (Antigravity: `.agents/rules/project-structure.md`).

**Usage for agents:** treat the project-structure rule as orientation; **read the matching file here before changing that area.**

This index follows the same layout used in [BBS CML Edition](https://github.com/ElGatoPro300/BBS-CML-EDITION/blob/master/docs/architecture/README.md) and [BBS CML 3.0](https://github.com/ElGatoPro300/BBS-CML-3.0/blob/master/docs/architecture/README.md), adapted to an addon that bridges BBS forms with Photon FX (not the full film/forms core).

## Core (this addon)

| Doc | Topic |
|-----|--------|
| [code-organization.md](code-organization.md) | Package roles, Domain/Adapter boundary, extraction targets |
| [photon-form.md](photon-form.md) | `PhotonForm` Values, persistence, film tracks |
| [photon-renderer.md](photon-renderer.md) | FX lifecycle, pause/scrub, cleanup, Iris notes, known debt |
| [bbs-addon-hooks.md](bbs-addon-hooks.md) | `BBSAddon` / `BBSClientAddon` registration and client wiring |
| [ui-photon-editor.md](ui-photon-editor.md) | Form editor panel, effect picker, `photon_fx` keyframe UI |
| [photon2-compat-plan.md](photon2-compat-plan.md) | Roadmap to Photon **`2.2.7`** + **`.fxpack`** support on NeoForge 1.21.1 |

## How this relates to BBS CML (do not duplicate here)

Prefer linking to the host mod rather than copying film/forms manuals into this repo:

| Host doc | When to read it |
|----------|-----------------|
| [ADDONS.md](https://github.com/ElGatoPro300/BBS-CML-EDITION/blob/master/docs/ADDONS.md) | Writing or extending addon registration, events, Values, UI |
| [forms.md](https://github.com/ElGatoPro300/BBS-CML-EDITION/blob/master/docs/architecture/forms.md) | How BBS forms/renderers/panels fit together |
| [keyframes.md](https://github.com/ElGatoPro300/BBS-CML-EDITION/blob/master/docs/architecture/keyframes.md) | Timeline keyframe factories / string tracks |
| [rendering-iris.md](https://github.com/ElGatoPro300/BBS-CML-EDITION/blob/master/docs/architecture/rendering-iris.md) | Iris/shadow constraints for world/film draws |
| [code-organization.md (3.0)](https://github.com/ElGatoPro300/BBS-CML-3.0/blob/master/docs/architecture/code-organization.md) | Orchestrator / pass patterns (inspiration for renderer extractions) |

## Other project docs (repo root)

* `AGENTS.md` — build commands, standards entrypoint for AI agents
* `CONTRIBUTING.md` — contribution and code style
* `README.md` — project introduction
* `LICENSE.md` — license

## Populating this folder

- Keep this README as an **index**, not a second project-structure map.
- Add a new deep-dive only when a subsystem is real in code (or explicitly planned, like Photon `2.2.7` compatibility).
- Do not invent film/clip/actor deep-dives here; link to BBS CML instead.
