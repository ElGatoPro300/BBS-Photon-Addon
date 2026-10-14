package elgatopro300.bbsphoton.forms;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.core.ValueTransform;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.utils.pose.Transform;

/** Attachment path on the host form, plus an offset, for one Photon object. */
public class PhotonAnchor extends ValueGroup
{
    public final ValueBoolean enabled = new ValueBoolean("enabled", true);
    public final ValueString target = new ValueString("target", "");
    public final ValueString attachment = new ValueString("attachment", "");
    public final ValueTransform offset = new ValueTransform("offset", new Transform());

    public PhotonAnchor(String id)
    {
        super(id);
        this.add(this.enabled);
        this.add(this.target);
        this.add(this.attachment);
        this.add(this.offset);
    }
}
