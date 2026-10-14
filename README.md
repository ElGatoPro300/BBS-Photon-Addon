# BBS Photon Addon

BBS Photon Addon, as the name suggests, is a BBS addon that allows you to use Photon particles as a form. This open-source code is intended to be a reference for those who want to create addons, whether with programming knowledge or using AI.

I hope this code helps you with your BBS addons!

## Runtime

- **Minecraft** 1.21.1 on **NeoForge** (Sinytra Connector)
- **Host**: [BBS CML 3.0](https://github.com/ElGatoPro300/BBS-CML-3.0), addon API **2**
- **NeoForge**: 21.1.248 or newer, with Sinytra Connector 2.0.0-beta.12 and Forgified Fabric API
- **FX engine**: Photon Editor / Photon2 **2.2.8** (NeoForge), including mounted **`.fxpack`** effects

The addon uses Connector to expose its BBS entrypoints. Its metadata requires NeoForge; this is not a native Fabric build. CML 2.x is no longer supported by this branch. The saved form type `bbs:photon` and Value IDs `photon_fx` / `paused` are preserved.

The form editor also provides per-object animated parameters (including lights/fog), optional film synchronization with seed and offset controls, object anchors, local signal events/diagnostics, and animated post processing with isolated previews. See [runtime controls and limitations](docs/architecture/photon-runtime-features.md) before enabling film resimulation.

## Build against local CML 3.0

Use Java 21. Build the Minecraft 1.21.1 distribution in the sibling `BBS-CML-3.0` checkout first. The default `bbs_cml_jar` property points to its remapped distribution JAR:

```powershell
./gradlew.bat build
./gradlew.bat runClient
# Override the host location when needed:
./gradlew.bat build "-Pbbs_cml_jar=C:/path/to/bbs-cml-3.0-dev-1-1.21.1.jar"
```

Development launches stage the host and addon JARs into `run/mods` for Connector discovery. Do not add another copy of either mod to that directory. The build does not bundle CML or Photon into the addon JAR.

Rebuild the companion CML checkout with the Connector compatibility fixes included in this migration. Older local JARs may reject Connector's loader version or fail in host mixins. See [CML 3.0 migration and verification](docs/architecture/cml3-migration.md) for integration details and verification status.

## For contributors & AI agents

Before changing code, read:

1. [`AGENTS.md`](AGENTS.md) — build commands and mandatory standards
2. [`CONTRIBUTING.md`](CONTRIBUTING.md) — contribution and code style
3. Feature rules (Cursor: `.cursor/rules/`, Antigravity: `.agents/rules/`)
4. Architecture index: [`docs/architecture/README.md`](docs/architecture/README.md)

Host addon API reference: [BBS CML 3.0 `docs/ADDONS.md`](https://github.com/ElGatoPro300/BBS-CML-3.0/blob/master/docs/ADDONS.md).

See `LICENSE.md` for information about the license.
