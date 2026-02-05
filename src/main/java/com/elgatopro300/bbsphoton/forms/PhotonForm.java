package com.elgatopro300.bbsphoton.forms;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.core.ValueString;

public class PhotonForm extends Form {
    public final ValueString effect = new ValueString("effect", "");

    public PhotonForm() {
        super();
        this.add(this.effect);
    }
}
