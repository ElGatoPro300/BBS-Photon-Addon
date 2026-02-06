package com.elgatopro300.bbsphoton;

import com.elgatopro300.bbsphoton.client.BBSPhotonClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BBSPhoton.MOD_ID)
public class BBSPhoton {
    public static final String MOD_ID = "bbs_photon_addon";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public BBSPhoton() {
        LOGGER.info("BBS Photon Addon initialized (NeoForge)!");
        
        if (FMLEnvironment.dist == Dist.CLIENT) {
            BBSPhotonClient.init();
        }
    }
}
