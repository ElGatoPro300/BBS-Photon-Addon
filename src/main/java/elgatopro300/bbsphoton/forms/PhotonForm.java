package elgatopro300.bbsphoton.forms;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueLong;

public class PhotonForm extends Form
{
    public final ValueString photon_fx = new ValueString("photon_fx", "");
    public final ValueBoolean paused = new ValueBoolean("paused", false);
    public final PhotonValueList<PhotonBinding> parameters = new PhotonValueList<>("photon_parameters", PhotonBinding::new);
    public final PhotonValueList<PhotonAnchor> anchors = new PhotonValueList<>("photon_anchors", PhotonAnchor::new);
    public final ValueBoolean filmSync = new ValueBoolean("photon_film_sync", false);
    public final ValueBoolean speedOverride = new ValueBoolean("photon_speed_override", false);
    public final ValueFloat speed = new ValueFloat("photon_speed", 1F, 0F, 16F);
    public final ValueFloat startOffset = new ValueFloat("photon_start_offset", 0F, 0F, Float.MAX_VALUE);
    public final ValueBoolean fixedSeed = new ValueBoolean("photon_fixed_seed", false);
    public final ValueLong seed = new ValueLong("photon_seed", 0L);
    public final ValueBoolean signals = new ValueBoolean("photon_signals", false);
    public final ValueBoolean postEnabled = new ValueBoolean("photon_post_enabled", false);
    public final ValueString postEffect = new ValueString("photon_post_effect", "");
    public final ValueFloat postWeight = new ValueFloat("photon_post_weight", 1F, 0F, 1F);
    public final PhotonValueList<PhotonBinding> postParameters = new PhotonValueList<>("photon_post_parameters", PhotonBinding::new);

    public PhotonForm()
    {
        super();
        
        this.add(this.photon_fx);
        this.add(this.paused);
        this.add(this.parameters);
        this.add(this.anchors);
        this.add(this.filmSync);
        this.add(this.speedOverride);
        this.add(this.speed);
        this.add(this.startOffset);
        this.add(this.fixedSeed);
        this.add(this.seed);
        this.add(this.signals);
        this.add(this.postEnabled);
        this.add(this.postEffect);
        this.add(this.postWeight);
        this.add(this.postParameters);
    }
}
