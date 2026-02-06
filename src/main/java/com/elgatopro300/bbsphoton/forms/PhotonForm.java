package com.elgatopro300.bbsphoton.forms;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;

public class PhotonForm extends Form {
    public final ValueString effect = new ValueString("effect", "");
    public final ValueBoolean paused = new ValueBoolean("paused", false);
    
    public final ValueFloat user1 = new ValueFloat("user1", 0F);
    public final ValueFloat user2 = new ValueFloat("user2", 0F);
    public final ValueFloat user3 = new ValueFloat("user3", 0F);
    public final ValueFloat user4 = new ValueFloat("user4", 0F);

    public PhotonForm() {
        super();
        this.add(this.effect);
        this.add(this.paused);
        
        this.add(this.user1);
        this.add(this.user2);
        this.add(this.user3);
        this.add(this.user4);
    }
}
