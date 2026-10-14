package elgatopro300.bbsphoton.client;

import mchorse.bbs_mod.api.AddonContext;
import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.RegisterFormEditorsEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormRenderersEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormSectionsEvent;
import mchorse.bbs_mod.api.client.events.RegisterKeyframeEditorsEvent;
import mchorse.bbs_mod.api.client.events.RegisterL10nEvent;
import mchorse.bbs_mod.api.client.events.RegisterTrackPresentationEvent;
import mchorse.bbs_mod.api.replays.TrackPresentation;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.resources.Link;

import java.util.List;

import elgatopro300.bbsphoton.client.gui.PhotonFormSection;
import elgatopro300.bbsphoton.client.gui.UIPhotonForm;
import elgatopro300.bbsphoton.client.gui.UIPhotonFxKeyframeFactory;
import elgatopro300.bbsphoton.client.render.PhotonFormRenderer;
import elgatopro300.bbsphoton.forms.PhotonForm;

public class BBSPhotonClientAddon implements BBSAddonMod
{
    private final BBSPhotonClient lifecycle = new BBSPhotonClient();

    @Override
    public int requiredApiVersion()
    {
        return 2;
    }

    @Override
    public void initialize(AddonContext context)
    {
        this.lifecycle.initialize();
    }

    @Override
    public void shutdown(AddonContext context)
    {
        this.lifecycle.stopEffects();
    }

    @Subscribe
    public void registerRenderers(RegisterFormRenderersEvent event)
    {
        event.register(PhotonForm.class, PhotonFormRenderer::new);
    }

    @Subscribe
    public void registerEditors(RegisterFormEditorsEvent event)
    {
        event.register(PhotonForm.class, UIPhotonForm::new);
    }

    @Subscribe
    public void registerSections(RegisterFormSectionsEvent event)
    {
        event.register(PhotonFormSection::new);
    }

    @Subscribe
    public void registerKeyframes(RegisterKeyframeEditorsEvent event)
    {
        event.<String>registerProperty("photon_fx", UIPhotonFxKeyframeFactory::new);
    }

    @Subscribe
    public void registerTracks(RegisterTrackPresentationEvent event)
    {
        event.register(new TrackPresentation("photon_fx", "bbs.keyframe.photon_fx", 0xFF00B2,
            "bbs_photon:properties", "bbs_photon.ui.panel_title", "", 0));
    }

    @Subscribe
    public void registerLanguages(RegisterL10nEvent event)
    {
        event.l10n.register(language -> List.of(
            Link.create("bbs_photon:strings/" + L10n.DEFAULT_LANGUAGE + ".json"),
            Link.create("bbs_photon:strings/" + language + ".json")
        ));
    }
}
