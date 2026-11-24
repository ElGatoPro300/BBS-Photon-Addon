package elgatopro300.bbsphoton.client.gui;

import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.settings.values.base.BaseValueNumber;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;

/** Keep host editor and numeric Value differences outside the shared panels. */
public final class HostEditorAccess
{
    @SuppressWarnings("unchecked")
    public static <T extends Number> void setNumber(BaseValueNumber<T> value, double number)
    {
        value.set((T) value.getFactory().yToValue(number));
    }

    public static IEntity entity(UIForm<?> form)
    {
        return form.editor.getTargetEntity();
    }
}
