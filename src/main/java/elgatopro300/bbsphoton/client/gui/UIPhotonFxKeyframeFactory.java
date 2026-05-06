package elgatopro300.bbsphoton.client.gui;

import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIStringKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.ArrayList;
import java.util.List;

public class UIPhotonFxKeyframeFactory extends UIStringKeyframeFactory
{
    private static List<String> cachedEffects;

    public UIPhotonFxKeyframeFactory(Keyframe<String> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        UIButton pickEffect = new UIButton(L10n.lang("bbs_photon.ui.pick_effect"), (b) ->
        {
            if (cachedEffects == null)
            {
                cachedEffects = new ArrayList<>();
                UIPhotonFormPanel.populateEffects(cachedEffects);
            }

            UIListOverlayPanel panel = new UIListOverlayPanel(L10n.lang("bbs_photon.ui.select_effect"), (str) ->
            {
                this.editor.getGraph().setValue(str, true);
                
                /* Update the text field if found */
                for (Object child : this.scroll.getChildren())
                {
                    if (child instanceof UITextbox)
                    {
                        ((UITextbox) child).setText(str);
                    }
                }
            });

            panel.addValues(cachedEffects);
            panel.setValue(keyframe.getValue());
            UIOverlay.addOverlay(this.getContext(), panel, 0.5F, 0.7F);
        });

        this.scroll.add(pickEffect);
    }
}
