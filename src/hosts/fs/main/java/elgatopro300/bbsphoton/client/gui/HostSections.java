package elgatopro300.bbsphoton.client.gui;

import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UISection;

/** Host-specific section construction, preserving labels and control bindings. */
public final class HostSections
{
    public static ValueBoolean visible(FormCategories categories, String id)
    {
        return categories.preferences.visible(id);
    }

    public static UISection create(IKey title, String id, UIElement... fields)
    {
        UISection section = new UISection(title);
        section.fields.add(fields);
        return section;
    }
}
