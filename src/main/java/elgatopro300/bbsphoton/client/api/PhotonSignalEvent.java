package elgatopro300.bbsphoton.client.api;

import net.minecraft.nbt.CompoundTag;

import elgatopro300.bbsphoton.forms.PhotonForm;

/** Posted on BBSMod.events on the client thread, only for enabled live playback. */
public record PhotonSignalEvent(PhotonForm form, String effectId, String channel, String name,
                               CompoundTag payload, double time)
{
    public PhotonSignalEvent
    {
        payload = payload.copy();
    }

    @Override
    public CompoundTag payload()
    {
        return this.payload.copy();
    }
}
