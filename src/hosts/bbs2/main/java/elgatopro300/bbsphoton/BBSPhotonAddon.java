package elgatopro300.bbsphoton;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.events.BBSAddonMod;
import mchorse.bbs_mod.events.Subscribe;
import mchorse.bbs_mod.events.register.RegisterSettingsEvent;
import mchorse.bbs_mod.events.register.RegisterSourcePacksEvent;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.resources.packs.InternalAssetsSourcePack;

import elgatopro300.bbsphoton.forms.PhotonForm;

/** Upstream emits settings after constructing its form registry. */
public final class BBSPhotonAddon implements BBSAddonMod
{
    @Subscribe
    public void forms(RegisterSettingsEvent event)
    {
        BBSMod.getForms().register(Link.bbs("photon"), PhotonForm.class, null);
    }

    @Subscribe
    public void resources(RegisterSourcePacksEvent event)
    {
        event.provider.register(new InternalAssetsSourcePack("bbs_photon", "assets/bbs_photon", BBSPhotonAddon.class));
    }
}
