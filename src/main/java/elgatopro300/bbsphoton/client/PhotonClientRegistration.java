package elgatopro300.bbsphoton.client;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.RegisterFormEditorsEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormRenderersEvent;
import mchorse.bbs_mod.api.client.events.RegisterFormSectionsEvent;
import mchorse.bbs_mod.api.client.events.RegisterKeyframeEditorsEvent;
import mchorse.bbs_mod.api.client.events.RegisterL10nEvent;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.resources.Link;

import java.util.List;

import elgatopro300.bbsphoton.client.gui.PhotonFormSection;
import elgatopro300.bbsphoton.client.gui.UIPhotonForm;
import elgatopro300.bbsphoton.client.gui.UIPhotonFxKeyframeFactory;
import elgatopro300.bbsphoton.client.render.PhotonFormRenderer;
import elgatopro300.bbsphoton.forms.PhotonForm;

/** Common form/editor registration with no host-specific lifecycle signatures. */
public abstract class PhotonClientRegistration implements BBSAddonMod
{
    private final BBSPhotonClient lifecycle = new BBSPhotonClient();

    protected final void initializeClient()
    {
        this.lifecycle.initialize();
    }

    protected final void shutdownClient()
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
    public void registerLanguages(RegisterL10nEvent event)
    {
        event.l10n.register(language -> List.of(
            Link.create("bbs_photon:strings/" + L10n.DEFAULT_LANGUAGE + ".json"),
            Link.create("bbs_photon:strings/" + language + ".json")
        ));
    }
}
