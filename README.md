# BBS Photon Addon

BBS Photon Addon, as the name suggests, is a BBS addon that allows you to use Photon particles as a form. This open-source code is intended to be a reference for those who want to create addons, whether with programming knowledge or using AI.

I hope this code helps you with your BBS addons!

## Runtime

- **Minecraft** 26.1.2 on **NeoForge** (Sinytra Connector)
- **Host**: [BBS CML 3.0](https://github.com/ElGatoPro300/BBS-CML-3.0), addon API **2**
- **NeoForge**: 26.1.2.122, with Sinytra Connector 3.0.0-beta.6, Launchpad 1.10.2 and Forgified Fabric API 0.155.3+26.1.2+3.5.9
- **FX engine**: Photon Editor / Photon2 **26.1.2.3** (NeoForge; `mc26.1.x-26.1.2.3-neoforge`), including mounted **`.fxpack`** effects

The addon uses Connector to expose its BBS entrypoints. Its metadata requires NeoForge; this is not a native Fabric build. CML 2.x is no longer supported by this branch. The saved form type `bbs:photon` and Value IDs `photon_fx` / `paused` are preserved.

The form editor also provides per-object animated parameters (including lights/fog), optional film synchronization with seed and offset controls, object anchors, local signal events/diagnostics, and animated post processing with isolated previews. See [runtime controls and limitations](docs/architecture/photon-runtime-features.md) before enabling film resimulation.

## Build against local CML 3.0

Use Java 25. Place the CML 3.0 Minecraft 26.1.x distribution JAR (supporting 26.1.2) at `libs/bbs-cml-3.0-beta-2-26.1.jar`, or pass its location with `bbs_cml_jar`. Older 1.21.1 builds are incompatible.

```powershell
./gradlew.bat build
./gradlew.bat runClient
# Override the host location when needed:
./gradlew.bat build "-Pbbs_cml_jar=C:/path/to/bbs-cml-3.0-beta-2-26.1.jar"
```

For an isolated development directory, use `./gradlew.bat runClient "-Pgame_directory=build/validation/client-26.1.2"`.

Development launches stage the host and addon JARs into `run/mods` for Connector discovery. Do not add another copy of either mod to that directory. The build does not bundle CML or Photon into the addon JAR.

LDLib2 is pinned to 26.1.2.42 and KilaGraph to 26.1.0.16. Development launches include Sodium 0.9.2 and Iris 1.11.4 for NeoForge. These dependencies are not bundled. See [26.1.2 migration and verification](docs/architecture/cml3-migration.md) for the API changes and remaining visual checks.

## For contributors & AI agents

Before changing code, read:

1. [`AGENTS.md`](AGENTS.md) — build commands and mandatory standards
2. [`CONTRIBUTING.md`](CONTRIBUTING.md) — contribution and code style
3. Feature rules (Cursor: `.cursor/rules/`, Antigravity: `.agents/rules/`)
4. Architecture index: [`docs/architecture/README.md`](docs/architecture/README.md)

Host addon API reference: [BBS CML 3.0 `docs/ADDONS.md`](https://github.com/ElGatoPro300/BBS-CML-3.0/blob/master/docs/ADDONS.md).

See `LICENSE.md` for information about the license.
