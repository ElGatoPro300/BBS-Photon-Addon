# Photon form (domain)

## Role

`elgatopro300.bbsphoton.forms.PhotonForm` is the BBS **form** that stores which Photon FX to play and whether it should stay paused. It is intentionally tiny and must remain free of Minecraft / Photon imports.

## Fields (Values)

| Value | Type | Id | Default | Meaning |
|-------|------|----|---------|---------|
| `photon_fx` | `ValueString` | `photon_fx` | `""` | Photon FX identifier / path used by the renderer |
| `paused` | `ValueBoolean` | `paused` | `false` | User/editor pause flag for the FX |

Registered in the constructor via `this.add(...)`.

## Persistence

BBS serializes Values automatically (NBT/JSON). Film replays can keyframe form properties; CML 3.0's property-specific keyframe registration matches `photon_fx`, including bodypart paths.

**Do not rename Value ids** without a migration plan and explicit maintainer approval — existing projects store these keys.

## Registration

Common/server addon registers the form type:

- Link: `bbs:photon`
- Class: `PhotonForm`
- Event: `RegisterFormsEvent` in `BBSPhotonAddon`

See [bbs-addon-hooks.md](bbs-addon-hooks.md).

## Hard rules

- No Photon FX start/stop logic here.
- No UI widgets here.
- New persistent options → new `Value*` fields + L10n keys + UI bindings in `client.gui`.

## Related

- Renderer: [photon-renderer.md](photon-renderer.md)
- Editor: [ui-photon-editor.md](ui-photon-editor.md)
- Host forms overview: [BBS forms.md](https://github.com/ElGatoPro300/BBS-CML-EDITION/blob/master/docs/architecture/forms.md)
