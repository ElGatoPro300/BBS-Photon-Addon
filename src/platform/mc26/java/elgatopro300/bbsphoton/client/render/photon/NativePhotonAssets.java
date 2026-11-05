package elgatopro300.bbsphoton.client.render.photon;

import net.minecraft.resources.Identifier;

import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;

final class NativePhotonAssets
{
    private NativePhotonAssets()
    {
    }

    static FX effect(String effectId)
    {
        Identifier id = Identifier.tryParse(effectId);
        return id == null ? null : FXHelper.getFX(id);
    }
}
