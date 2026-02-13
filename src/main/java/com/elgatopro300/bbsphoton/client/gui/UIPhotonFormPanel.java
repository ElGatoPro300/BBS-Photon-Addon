package com.elgatopro300.bbsphoton.client.gui;

import com.elgatopro300.bbsphoton.forms.PhotonForm;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class UIPhotonFormPanel extends UIFormPanel<PhotonForm>
{
    public UIButton pickEffect;
    public UIToggle paused;

    private List<String> cachedEffects;

    public UIPhotonFormPanel(UIForm editor)
    {
        super(editor);

        this.pickEffect = new UIButton(IKey.raw("Pick Photon Effect"), (b) -> this.openPicker());
        
        this.paused = new UIToggle(IKey.raw("Paused"), (b) ->
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
        if (this.cachedEffects == null)
        {
            this.cachedEffects = new ArrayList<>();
            this.populateEffects(this.cachedEffects);
        }
        
        UIListOverlayPanel panel = new UIListOverlayPanel(IKey.raw("Select Photon Effect"), (str) ->
        {
            if (this.form != null)
            {
                this.form.effect.set(str);
            }
        });
        
        panel.addValues(this.cachedEffects);
        UIOverlay.addOverlay(this.getContext(), panel, 0.5F, 0.7F);
    }
    
    @Override
    public void startEdit(PhotonForm form)
    {
        super.startEdit(form);
        
        /* Always keep the static label as requested */
        this.pickEffect.label = IKey.raw("Pick Photon Effect");
        
        this.paused.setValue(form.paused.get());
    }
    
    private void populateEffects(List<String> list)
    {
        /* 1. Scan standard BBS resources for .fx files */
        try
        {
            Collection<String> sources = BBSMod.getProvider().getSourceKeys();
            
            for (String source : sources)
            {
                try
                {
                    /* Look for assets/<source>/photon */
                    Collection<Link> links = BBSMod.getProvider().getLinksFromPath(new Link(source, "photon"), true);
                    
                    for (Link link : links)
                    {
                        if (link.path.endsWith(".fx"))
                        {
                            /* Construct ID: source:path_relative_to_photon
                             * path is like "photon/subdir/effect.fx" */
                            String path = link.path;
                            
                            if (path.startsWith("photon/"))
                            {
                                path = path.substring(7); /* remove "photon/" */
                            }
                            
                            if (path.endsWith(".fx"))
                            {
                                path = path.substring(0, path.length() - 3); /* remove ".fx" */
                            }
                            
                            /* Clean up "fx/" prefix if present, as it seems common */
                            if (path.startsWith("fx/"))
                            {
                                path = path.substring(3);
                            }
                            
                            list.add(source + ":" + path);
                        }
                    }
                }
                catch (Exception e)
                {
                    /* Ignore errors for specific sources */
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        /* 2. Scan ldlib/assets/photon/fx directory manually */
        try
        {
            File gameDir = FabricLoader.getInstance().getGameDir().toFile();
            File photonFxDir = new File(gameDir, "ldlib2/assets/photon/fx");
            
            if (photonFxDir.exists() && photonFxDir.isDirectory())
            {
                File[] files = photonFxDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".fx"));
                
                if (files != null)
                {
                    for (File file : files)
                    {
                        String name = file.getName();
                        String id = name.substring(0, name.length() - 3); /* remove .fx */
                        
                        /* Assuming these belong to the "photon" namespace */
                        list.add("photon:" + id);
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        Collections.sort(list);
    }
}
