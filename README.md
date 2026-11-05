# BBS Photon Addon

BBS Photon Addon, as the name suggests, is a BBS addon that allows you to use Photon particles as a form. This open-source code is intended to be a reference for those who want to create addons, whether with programming knowledge or using AI.

I hope this code helps you with your BBS addons!

## Runtime

One source tree supports **NeoForge + Sinytra Connector** on Minecraft **1.21.1 and 26.1.2**. The exact Photon, LDLib, Connector, FFAPI and NeoForge dependencies are pinned in `gradle/minecraft-targets.json`. CML addon API 2 remains the host contract. The canonical `bbs:photon` form and persistent Value identifiers are preserved.

The editor provides per-object animated parameters, film synchronization, seeds, anchors, signals and isolated post-effect previews. See [runtime controls](docs/architecture/photon-runtime-features.md) for behavior and limits.

## Build

Run the wrapper with **JDK 25** and place the matching CML distribution JARs in `libs/`:

```powershell
./gradlew.bat buildAll
./gradlew.bat build '-Pmc=1.21.1'
./gradlew.bat runClient '-Pmc=26.1.2'
```

All installable JARs appear in `build/libs/`. The default build targets 1.21.1. Development runs stage CML and the addon in `build/runs/<mc>/mods` for Connector discovery. Dependencies are not bundled in release JARs.

Use `-PcmlJar=<path>` (or the existing `-Pbbs_cml_jar=<path>`) for one host, or `-PlibDir=<directory>` for a folder with several versions. See [multiversion architecture and validation](docs/multiversion.md) for native adapters, dependency validation and runtime limits.

## For contributors & AI agents

Before changing code, read:

1. [`AGENTS.md`](AGENTS.md) — build commands and mandatory standards
2. [`CONTRIBUTING.md`](CONTRIBUTING.md) — contribution and code style
3. Feature rules (Cursor: `.cursor/rules/`, Antigravity: `.agents/rules/`)
4. Architecture index: [`docs/architecture/README.md`](docs/architecture/README.md)

Host addon API reference: [BBS CML 3.0 `docs/ADDONS.md`](https://github.com/ElGatoPro300/BBS-CML-3.0/blob/master/docs/ADDONS.md).

See `LICENSE.md` for information about the license.
