package com.elgatopro300.bbsphoton.client;

import com.elgatopro300.bbsphoton.forms.PhotonForm;
import mchorse.bbs_mod.BBSModClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.resources.packs.InternalAssetsSourcePack;

public class BBSPhotonClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("bbs-photon-addon-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing BBS Photon Client Addon...");
        
        // Add PhotonForm to Extra category after the client has fully started
        // This ensures that BBSResources.init() has already run and we don't get overwritten
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            try {
                // Register source pack for bbs_photon namespace
                BBSMod.getProvider().register(new InternalAssetsSourcePack("bbs_photon", "assets/bbs_photon", BBSPhotonClient.class));
                LOGGER.info("Registered 'bbs_photon' source pack.");

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
