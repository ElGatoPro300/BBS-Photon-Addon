package elgatopro300.bbsphoton.client;

import mchorse.bbs_mod.api.AddonContext;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.RegisterTrackPresentationEvent;
import mchorse.bbs_mod.api.replays.TrackPresentation;

public class BBSPhotonClientAddon extends PhotonClientRegistration
{
    @Override
    public int requiredApiVersion()
    {
        return 2;
    }

    @Override
    public void initialize(AddonContext context)
    {
        this.initializeClient();
    }

    @Override
    public void shutdown(AddonContext context)
    {
        this.shutdownClient();
    }

    @Subscribe
    public void registerTracks(RegisterTrackPresentationEvent event)
    {
        event.register(new TrackPresentation("photon_fx", "bbs.keyframe.photon_fx", 0xFF00B2,
            "bbs_photon:properties", "bbs_photon.ui.panel_title", "", 0));
    }

}
