package elgatopro300.bbsphoton.client.gui;

import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.editors.UIKeyframeValueContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;

import java.util.function.Consumer;

/** Preserve the upstream undo callback for both typed and picked FX identifiers. */
public final class PhotonKeyframeEditor
{
    public static void open(UIContext context, String value, Consumer<Object> callback)
    {
        UITextbox textbox = new UITextbox(10000, callback::accept);
        textbox.setText(value);
        UIButton picker = new UIButton(L10n.lang("bbs_photon.ui.pick_effect"), button ->
        {
            UIListOverlayPanel panel = new UIListOverlayPanel(L10n.lang("bbs_photon.ui.select_effect"), selected ->
            {
                textbox.setText(selected);
                callback.accept(selected);
            });
            panel.addValues(PhotonFxDiscovery.listEffectIds());
            panel.setValue(textbox.getText());
            UIOverlay.addOverlay(context, panel, 0.5F, 0.7F);
        });
        context.replaceContextMenu(new UIKeyframeValueContextMenu(textbox, picker));
        context.focus(textbox);
    }
}
