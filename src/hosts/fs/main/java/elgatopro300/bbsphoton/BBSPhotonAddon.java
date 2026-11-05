package elgatopro300.bbsphoton;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.events.RegisterFormsEvent;
import mchorse.bbs_mod.api.events.RegisterSourcePacksEvent;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.resources.packs.InternalAssetsSourcePack;

import elgatopro300.bbsphoton.forms.PhotonForm;

public class BBSPhotonAddon implements BBSAddonMod
{
    @Subscribe
    public void registerForms(RegisterFormsEvent event)
    {
        event.forms.register(Link.create("bbs:photon"), PhotonForm.class);
    }

    @Subscribe
    public void registerResources(RegisterSourcePacksEvent event)
    {
        event.provider.register(new InternalAssetsSourcePack("bbs_photon", "assets/bbs_photon", BBSPhotonAddon.class));
    }
}
