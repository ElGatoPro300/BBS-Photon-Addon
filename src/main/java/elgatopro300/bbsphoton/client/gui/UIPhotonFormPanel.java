package elgatopro300.bbsphoton.client.gui;

import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.util.List;

import elgatopro300.bbsphoton.forms.PhotonForm;

public class UIPhotonFormPanel extends UIFormPanel<PhotonForm>
{
    public UIButton pickEffect;
    public UIToggle paused;

    public UIPhotonFormPanel(UIForm editor)
    {
        super(editor);

        this.pickEffect = new UIButton(L10n.lang("bbs_photon.ui.pick_effect"), (b) -> this.openPicker());
        
        this.paused = new UIToggle(L10n.lang("bbs_photon.ui.paused"), (b) ->
        {
            if (this.form != null)
            {
                this.form.paused.set(b.getValue());
            }
        });
        
        this.options.add(this.pickEffect.marginTop(6));
        this.options.add(this.paused.marginTop(6));
    }
    
    private void openPicker()
    {
        List<String> effects = PhotonFxDiscovery.listEffectIds();

        UIListOverlayPanel panel = new UIListOverlayPanel(L10n.lang("bbs_photon.ui.select_effect"), (str) ->
        {
            if (this.form != null)
            {
                this.form.photon_fx.set(str);
            }
        });
        
        UIIcon openFxFolder = new UIIcon(Icons.FOLDER, (b) ->
        {
            PhotonFxDiscovery.openFolder(PhotonFxDiscovery.getLooseFxDir());
        });
        openFxFolder.tooltip(L10n.lang("bbs_photon.ui.open_fx_folder"));
        panel.icons.add(openFxFolder);

        UIIcon openFxPacksFolder = new UIIcon(Icons.FOLDER, (b) ->
        {
            PhotonFxDiscovery.openFolder(PhotonFxDiscovery.getFxPacksDir());
        });
        openFxPacksFolder.tooltip(L10n.lang("bbs_photon.ui.open_fxpack_folder"));
        panel.icons.add(openFxPacksFolder);

        panel.addValues(effects);
        if (this.form != null)
        {
            panel.setValue(this.form.photon_fx.get());
        }
        UIOverlay.addOverlay(this.getContext(), panel, 0.5F, 0.7F);
    }
    
    @Override
    public void startEdit(PhotonForm form)
    {
        super.startEdit(form);
        
        this.pickEffect.label = L10n.lang("bbs_photon.ui.pick_effect");
        
        this.paused.setValue(form.paused.get());
    }
    
    public static void populateEffects(List<String> list)
    {
        list.addAll(PhotonFxDiscovery.listEffectIds());
    }
}
