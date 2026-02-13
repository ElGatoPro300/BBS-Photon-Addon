package com.elgatopro300.bbsphoton.forms;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;

public class PhotonForm extends Form {
    public final ValueString effect = new ValueString("effect", "");
    public final ValueBoolean paused = new ValueBoolean("paused", false);

    public PhotonForm() {
        super();
        this.add(this.effect);
        this.add(this.paused);
    }
}
