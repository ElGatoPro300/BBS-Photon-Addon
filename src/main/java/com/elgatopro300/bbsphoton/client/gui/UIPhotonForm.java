package com.elgatopro300.bbsphoton.client.gui;

import com.elgatopro300.bbsphoton.forms.PhotonForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;

public class UIPhotonForm extends UIForm<PhotonForm> {
    public UIPhotonForm() {
        super();
        this.defaultPanel = new UIPhotonFormPanel(this);
    }
}
