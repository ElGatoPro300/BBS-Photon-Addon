# Contribution rules (BBS Photon Addon)

If you submit features, bugfixes, or pull requests to this repository, follow this document. Failure to adhere may result in rejection of the change.

These rules keep a small integration addon maintainable while Photon `2.2.x` and BBS CML Edition evolve.

If you use AI assistants, they may help with the **Code Contribution Rules**, but humans must still understand the **Feature Contribution Rules** and the mandatory FEATURE-RULES in `.cursor/rules/` / `.agents/rules/` before merging non-trivial work.

---

# Feature Contribution Rules (FCR)

## General principles

- Prefer **plan → blueprint → implement → in-game test** (see FEATURE-RULES Part 1). Jumping straight to code is how Photon API bumps break film scrub/pause.
- Communicate non-trivial features or dependency bumps (Photon / BBS / NeoForge) with maintainers **before** landing them.
- Do not rename persisted Value ids (`photon_fx`, `paused`) without an explicit migration plan — saved films and forms depend on them.
- Do not inflate `PhotonFormRenderer` further; extract bridges/services when adding Photon `2.2.x` work.
- This project targets **NeoForge 1.21.1** only unless maintainers ask for another loader.

## Mandatory rules

1. **Test in-game** (`./gradlew runClient`) before pushing. For render/FX changes, test with and without Iris.
2. **Bugfixes must be surgical.** If a fix would require a large redesign, ship a minimal fix and propose the redesign separately.
3. **No silent dependency bumps.** Changing Photon, BBS, LDLib, or NeoForge versions requires an explicit maintainer request and an update to `docs/architecture/photon2-compat-plan.md` when Photon is involved.
4. **Preserve existing UI affordances** (effect picker, paused toggle, keyframe FX picker) unless maintainers approve a replacement that keeps equivalent capability.
5. **Know what you changed** so it can be listed in the next changelog / release notes.

## Recommended rules

- Keep PRs focused (one concern: Photon API adapt, bugfix, docs, UI polish).
- Prefer public Photon / BBS APIs over reflection; if reflection is required, centralize it.
- Match English comments and localization key style already used under `assets/bbs_photon/`.

---

# Code Contribution Rules (CCR)

## Formatting and Java style

- Braces on new lines:

```java
if (condition)
{
    doSomething();
}
```

- Always use `this.` for instance field and method access.
- Method length soft max ~150 lines; class soft max ~600–800 lines.
- Use capital suffixes on literals: `1F`, `2D`, `3L`.
- Do not use `var`.
- Prefer diamond generics: `new ArrayList<>()`.
- Comments in English. Prefer `/* */` for inline body comments.
- Class member order: constants → static fields → instance fields → static init → static methods → constructors → instance methods.

## Architecture (must)

- Domain (`forms/`) must not import Minecraft or Photon packages.
- Persistent data uses BBS `Value` types.
- User-facing strings use `L10n` / `IKey` keys (see `assets/bbs_photon/strings/`).
- Follow FEATURE-RULES Part 2 (Domain/Adapter, DRY, no new mutable globals).

## Gradle / build

- Do **not** modify `build.gradle`, `gradle.properties`, or wrapper files unless a maintainer explicitly requests it (e.g. Photon `2.2.7` bump).

## Commits / PRs

- Prefer small, descriptive commits focused on why the change exists.
- Do not commit secrets, local `run/` worlds, or large binary dumps.
- Reference architecture docs when the PR changes FX lifecycle or registration.

## License

See `LICENSE.md`.
