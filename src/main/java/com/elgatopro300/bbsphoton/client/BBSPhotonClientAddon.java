package com.elgatopro300.bbsphoton.client;

import com.elgatopro300.bbsphoton.client.gui.UIPhotonForm;
import com.elgatopro300.bbsphoton.client.render.PhotonFormRenderer;
import com.elgatopro300.bbsphoton.forms.PhotonForm;
import mchorse.bbs_mod.addons.BBSClientAddon;
import mchorse.bbs_mod.events.Subscribe;
import mchorse.bbs_mod.events.register.RegisterDashboardPanelsEvent;
import mchorse.bbs_mod.events.register.RegisterFormCategoriesEvent;
import mchorse.bbs_mod.events.register.RegisterFormsRenderersEvent;
import mchorse.bbs_mod.events.register.RegisterIconsEvent;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BBSPhotonClientAddon extends BBSClientAddon
{
    public static final Logger LOGGER = LoggerFactory.getLogger("bbs-photon-addon-client");

    @Override
    @Subscribe
    public void registerDashboardPanels(RegisterDashboardPanelsEvent event)
    {
        /* Register dashboard panels */
    }

    @Override
    @Subscribe
    public void registerFormsRenderers(RegisterFormsRenderersEvent event)
    {
        LOGGER.info("Registering Photon form renderer and panel...");
        /* Register form renderers */
        event.registerRenderer(PhotonForm.class, PhotonFormRenderer::new);
        event.registerPanel(PhotonForm.class, UIPhotonForm::new);
    }

    @Override
    @Subscribe
    public void registerFormCategories(RegisterFormCategoriesEvent event)
    {
        /* NOTE: This is handled in BBSPhotonClient.onInitializeClient via ClientLifecycleEvents.CLIENT_STARTED
         * to avoid being overwritten by BBSResources.init() */
    }

    @Override
    @Subscribe
    public void registerIcons(RegisterIconsEvent event)
    {
        LOGGER.info("Registering Photon form icon...");
        /* Updated to match actual icon size (96x96) */
        event.register(new Icon(PhotonFormRenderer.ICON, "bbs:photon", 0, 0, 40, 40, 40, 40));
    }
}
