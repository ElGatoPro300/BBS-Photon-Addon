package elgatopro300.bbsphoton.forms;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

/** Typed BBS tracks; property identities are interpreted only by the client adapter. */
public class PhotonBinding extends ValueGroup
{
    public final ValueBoolean enabled = new ValueBoolean("enabled", true);
    public final ValueString target = new ValueString("target", "");
    public final ValueString property = new ValueString("property", "");
    public final ValueString kind = new ValueString("kind", "FLOAT");
    public final ValueFloat x = new ValueFloat("x", 0F);
    public final ValueFloat y = new ValueFloat("y", 0F);
    public final ValueFloat z = new ValueFloat("z", 0F);
    public final ValueFloat w = new ValueFloat("w", 1F);
    public final ValueInt integer = new ValueInt("integer", 0);
    public final ValueInt color = new ValueInt("color", -1).colorAlpha();
    public final ValueBoolean flag = new ValueBoolean("flag", false);

    public PhotonBinding(String id)
    {
        super(id);
        this.add(this.enabled);
        this.add(this.target);
        this.add(this.property);
        this.add(this.kind);
        this.add(this.x);
        this.add(this.y);
        this.add(this.z);
        this.add(this.w);
        this.add(this.integer);
        this.add(this.color);
        this.add(this.flag);
    }
}
