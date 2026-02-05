package com.elgatopro300.bbsphoton;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BBSPhoton implements ModInitializer {
    public static final String MOD_ID = "bbs-photon-addon";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("BBS Photon Addon initialized!");
    }
}
