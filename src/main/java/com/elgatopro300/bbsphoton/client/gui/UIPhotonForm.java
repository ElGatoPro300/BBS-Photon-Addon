package com.elgatopro300.bbsphoton.client.gui;

import com.elgatopro300.bbsphoton.forms.PhotonForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class UIPhotonForm extends UIForm<PhotonForm> {
    public UIPhotonForm() {
        super();
        this.defaultPanel = new UIPhotonFormPanel(this);
        
        this.registerPanel(this.defaultPanel, IKey.raw("Photon"), Icons.PARTICLE);
        this.registerDefaultPanels();
    }
}
