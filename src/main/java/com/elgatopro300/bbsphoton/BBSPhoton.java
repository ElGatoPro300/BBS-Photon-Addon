package com.elgatopro300.bbsphoton;

import com.elgatopro300.bbsphoton.client.BBSPhotonClient;
import com.elgatopro300.bbsphoton.forms.PhotonForm;
import mchorse.bbs_mod.BBS;
import mchorse.bbs_mod.resources.Link;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BBSPhoton.MOD_ID)
public class BBSPhoton
{
    public static final String MOD_ID = "bbs_photon_addon";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public BBSPhoton(IEventBus modEventBus)
    {
        LOGGER.info("BBS Photon Addon initialized (NeoForge)!");
        
        modEventBus.addListener(this::commonSetup);

        try
        {
            /* Register addon to EventBus (for other events) */
            BBS.getEvents().register(new BBSPhotonAddon());
            LOGGER.info("Registered BBSPhotonAddon to BBS EventBus");

            /* Manual registration fallback (in case EventBus event was missed) */
            if (BBS.getForms() != null)
            {
                LOGGER.info("Attempting manual registration of PhotonForm...");
                BBS.getForms().register(Link.create("bbs:photon"), PhotonForm.class);
                LOGGER.info("Manually registered PhotonForm to BBS FactoryForms");
            }
            else
            {
                LOGGER.warn("BBS FactoryForms is null during construction, skipping manual registration");
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to register BBSPhotonAddon or PhotonForm", e);
        }

        if (FMLEnvironment.dist == Dist.CLIENT)
        {
            BBSPhotonClient.init();
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("BBSPhoton commonSetup...");
        
        try
        {
            if (BBS.getForms() != null)
            {
                LOGGER.info("Attempting manual registration of PhotonForm in commonSetup...");
                BBS.getForms().register(Link.create("bbs:photon"), PhotonForm.class);
                LOGGER.info("Manually registered PhotonForm to BBS FactoryForms in commonSetup");
            }
            else
            {
                LOGGER.warn("BBS FactoryForms is still null in commonSetup!");
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to register PhotonForm in commonSetup", e);
        }
    }
}
