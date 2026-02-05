package com.elgatopro300.bbsphoton.client.gui;

import com.elgatopro300.bbsphoton.forms.PhotonForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.utils.UI;

public class UIPhotonFormPanel extends UIFormPanel<PhotonForm> {
    public UITextbox effect;

    public UIPhotonFormPanel(UIForm editor) {
        super(editor);

        this.effect = new UITextbox(1000, (t) -> this.form.effect.set(t));
        this.effect.tooltip(IKey.raw("Photon Effect ID (e.g. photon:effect_name)"));

        this.options.add(UI.label(IKey.raw("Effect ID")).marginTop(6), this.effect);
    }

    @Override
    public void startEdit(PhotonForm form) {
        super.startEdit(form);

        this.effect.setText(form.effect.get());
    }
}
