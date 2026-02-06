package com.elgatopro300.bbsphoton.client;

import com.elgatopro300.bbsphoton.client.render.PhotonFormRenderer;
import com.elgatopro300.bbsphoton.forms.PhotonForm;
import mchorse.bbs_mod.BBS;
import mchorse.bbs_mod.BBSModClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.resources.packs.InternalAssetsSourcePack;

import com.elgatopro300.bbsphoton.client.gui.UIPhotonForm;

import java.util.ArrayList;
import java.util.List;

public class BBSPhotonClient {
    public static final Logger LOGGER = LoggerFactory.getLogger("bbs-photon-addon-client");

    public static void init() {
        LOGGER.info("Initializing BBS Photon Client Addon...");
        
        try {
            BBS.getEvents().register(new BBSPhotonClientAddon());
            LOGGER.info("Registered BBSPhotonClientAddon to BBS EventBus");
        } catch (Exception e) {
            LOGGER.error("Failed to register BBSPhotonClientAddon", e);
        }

        // Register global cleanup watchdog for Photon effects
        // This ensures effects are stopped when the form renderer is no longer active (e.g. UI closed)
        // preventing global Photon engine corruption
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!PhotonFormRenderer.activeRenderers.isEmpty()) {
                List<PhotonFormRenderer> renderers = new ArrayList<>(PhotonFormRenderer.activeRenderers);
                for (PhotonFormRenderer renderer : renderers) {
                    renderer.checkCleanup();
                }
            }
        });

        // Add PhotonForm to Extra category after the client has fully started
        // This ensures that BBSResources.init() has already run and we don't get overwritten
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            try {
                // Manual registration of Renderers and Panels (fallback)
                try {
                     LOGGER.info("Attempting manual registration of Photon renderers...");
                     // Manually register renderer
                     mchorse.bbs_mod.forms.FormUtilsClient.register(PhotonForm.class, PhotonFormRenderer::new);
                     // Manually register panel
                     mchorse.bbs_mod.ui.forms.editors.UIFormEditor.register(PhotonForm.class, UIPhotonForm::new);
                     LOGGER.info("Manually registered PhotonForm renderers and panels");
                } catch (Exception e) {
                    LOGGER.error("Failed to manually register renderers/panels (might already be registered or API mismatch)", e);
                }

                // Register source pack for bbs_photon namespace
                BBSMod.getProvider().register(new InternalAssetsSourcePack("bbs_photon", "assets/bbs_photon", BBSPhotonClient.class));
                LOGGER.info("Registered 'bbs_photon' source pack.");

                // Verify icon resource existence
                try {
                    var stream = BBSPhotonClient.class.getResourceAsStream("/assets/bbs_photon/textures/photon_texture.png");
                    if (stream != null) {
                        LOGGER.info("VERIFICATION: Icon file found in classpath!");
                        stream.close();
                    } else {
                        LOGGER.error("VERIFICATION: Icon file NOT found in classpath at /assets/bbs_photon/textures/photon_texture.png");
                    }
                } catch (Exception e) {
                    LOGGER.error("VERIFICATION: Error checking icon file", e);
                }

                LOGGER.info("Client started. Injecting PhotonForm into Extra category...");
                if (BBSModClient.getFormCategories() != null && 
                    BBSModClient.getFormCategories().getExtraForms() != null && 
                    BBSModClient.getFormCategories().getExtraForms().getExtraCategory() != null) {
                    
                    BBSModClient.getFormCategories().getExtraForms().getExtraCategory().addForm(new PhotonForm());
                    LOGGER.info("Successfully added PhotonForm to Extra category.");
                } else {
                    LOGGER.error("FormCategories or ExtraForms category is null!");
                }
            } catch (Exception e) {
                LOGGER.error("Failed to add PhotonForm to Extra category", e);
            }
        });
    }
}
