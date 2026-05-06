package elgatopro300.bbsphoton.forms;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;

public class PhotonForm extends Form
{
    public final ValueString photon_fx = new ValueString("photon_fx", "");
    public final ValueBoolean paused = new ValueBoolean("paused", false);

    public PhotonForm()
    {
        super();
        
        this.add(this.photon_fx);
        this.add(this.paused);
    }
}
