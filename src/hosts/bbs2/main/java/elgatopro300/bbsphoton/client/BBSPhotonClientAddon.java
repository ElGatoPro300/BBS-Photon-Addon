package elgatopro300.bbsphoton.client;

import mchorse.bbs_mod.events.Subscribe;
import mchorse.bbs_mod.events.register.RegisterL10nEvent;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;

import elgatopro300.bbsphoton.client.gui.UIPhotonForm;
import elgatopro300.bbsphoton.client.render.PhotonFormRenderer;
import elgatopro300.bbsphoton.forms.PhotonForm;

/** Methods are declared directly because upstream's event bus ignores inherited handlers. */
public final class BBSPhotonClientAddon
{
    public void initialize()
    {
        FormUtilsClient.register(PhotonForm.class, PhotonFormRenderer::new);
        UIFormEditor.register(PhotonForm.class, UIPhotonForm::new);
        new BBSPhotonClient().initialize();
    }

    @Subscribe
    public void languages(RegisterL10nEvent event)
    {
        event.l10n.registerOne(language -> Link.create("bbs_photon:strings/" + language + ".json"));
        event.l10n.reload();
    }
}
