package com.elgatopro300.bbsphoton;

import com.elgatopro300.bbsphoton.forms.PhotonForm;
import mchorse.bbs_mod.addons.BBSAddon;
import mchorse.bbs_mod.events.Subscribe;
import mchorse.bbs_mod.events.register.RegisterFormsEvent;
import mchorse.bbs_mod.resources.Link;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BBSPhotonAddon extends BBSAddon {

    private static final Logger LOGGER = LoggerFactory.getLogger("BBS Photon Addon");

    public BBSPhotonAddon() {
        LOGGER.info("BBSPhotonAddon instantiated!");
    }

    @Override
    @Subscribe
    public void registerForms(RegisterFormsEvent event) {
        LOGGER.info("Registering Photon form...");
        event.getForms().register(Link.create("bbs_photon:photon"), PhotonForm.class);
        LOGGER.info("Registered Photon form: " + Link.create("bbs_photon:photon"));
    }
}
