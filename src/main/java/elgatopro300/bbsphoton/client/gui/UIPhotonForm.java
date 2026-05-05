package elgatopro300.bbsphoton.client.gui;

import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import elgatopro300.bbsphoton.forms.PhotonForm;

public class UIPhotonForm extends UIForm<PhotonForm>
{
    public UIPhotonForm()
    {
        super();
        
        this.defaultPanel = new UIPhotonFormPanel(this);
        this.registerPanel(this.defaultPanel, L10n.lang("bbs_photon.ui.panel_title"), Icons.PARTICLE);
        this.registerDefaultPanels();
    }
}
