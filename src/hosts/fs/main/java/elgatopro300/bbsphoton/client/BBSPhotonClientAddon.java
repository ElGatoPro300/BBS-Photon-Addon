package elgatopro300.bbsphoton.client;

import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.editor.TrackCategory;
import mchorse.bbs_mod.api.client.events.RegisterTrackCategoriesEvent;
import mchorse.bbs_mod.api.client.events.RegisterTrackStylesEvent;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class BBSPhotonClientAddon extends PhotonClientRegistration
{
    public BBSPhotonClientAddon()
    {
        this.initializeClient();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> this.shutdownClient());
    }

    @Subscribe
    public void categories(RegisterTrackCategoriesEvent event)
    {
        event.register(new TrackCategory("bbs_photon:properties", Icons.PARTICLE,
            L10n.lang("bbs_photon.ui.panel_title"), L10n.lang("bbs_photon.ui.panel_title")), (track, pose) -> "photon_fx".equals(track.property()));
    }

    @Subscribe
    public void registerTracks(RegisterTrackStylesEvent event)
    {
        event.register("photon_fx", Icons.PARTICLE, 0xFF00B2, L10n.lang("bbs.keyframe.photon_fx"));
    }

}
