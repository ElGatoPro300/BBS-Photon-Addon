# BBS CML 3.0 addon hooks

The host contract is [CML 3.0 ADDONS.md](https://github.com/ElGatoPro300/BBS-CML-3.0/blob/master/docs/ADDONS.md), API version 2.

## Discovery

`fabric.mod.json` exposes `bbs-addon` (`BBSPhotonAddon`) and `bbs-client-addon` (`BBSPhotonClientAddon`). Both implement `BBSAddonMod` and require API 2. The runtime requires NeoForge and Connector. A separate native NeoForge mod descriptor would make Connector treat the addon as a native mod with no Fabric entrypoints, so there is no duplicate native bootstrap.

CML owns discovery, subscription, failure diagnostics, the addon catalog and shutdown. Common registration never references client classes.

## Common registration

| Event | Responsibility |
| --- | --- |
| `RegisterFormsEvent` | Register `PhotonForm` under the existing `bbs:photon` ID |
| `RegisterSourcePacksEvent` | Expose the existing `assets/bbs_photon` folder as `bbs_photon` links |

## Client registration

| Event | Responsibility |
| --- | --- |
| `RegisterFormRenderersEvent` | Register `PhotonFormRenderer` |
| `RegisterFormEditorsEvent` | Register `UIPhotonForm` |
| `RegisterFormSectionsEvent` | Rebuild the Photon palette entry through `PhotonFormSection` |
| `RegisterKeyframeEditorsEvent` | Register the FX picker for the `photon_fx` property, including bodypart paths |
| `RegisterTrackPresentationEvent` | Register the translated Photon FX track label and magenta color |
| `RegisterL10nEvent` | Register bundled languages with the existing English fallback |

There is no global STRING factory override, replay-editor map reflection, late category injection or immediate language reload. `BBSPhotonClient` owns only client tick callbacks and effect cleanup on world exit/process shutdown. The palette thumbnail remains the Photon texture; the track presentation API does not provide a custom icon field.

## Rendering integration

`BbsFilmPauseProbe` reads the active dashboard's public film-panel state. `BbsRenderBridge` isolates the small number of reflective calls needed because the host distribution uses intermediary Minecraft signatures while the addon uses NeoForge/Mojang names. It unwraps the current screen's menu, retrieves the entity world and reads the pose stack. It does not reflect into private film fields or registries.

See [CML migration](cml3-migration.md), [renderer](photon-renderer.md) and [editor](ui-photon-editor.md).
