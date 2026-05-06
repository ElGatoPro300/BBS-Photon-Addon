package elgatopro300.bbsphoton.client;

import mchorse.bbs_mod.BBS;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.addons.AddonInfo;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.resources.packs.InternalAssetsSourcePack;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import elgatopro300.bbsphoton.client.gui.UIPhotonForm;
import elgatopro300.bbsphoton.client.render.PhotonFormRenderer;
import elgatopro300.bbsphoton.forms.PhotonForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BBSPhotonClient
{
    public static final Logger LOGGER = LoggerFactory.getLogger("bbs-photon-addon-client");

    public static void init()
    {
        LOGGER.info("Initializing BBS Photon Client Addon...");
        
        try
        {
            BBS.getEvents().register(new BBSPhotonClientAddon());
            LOGGER.info("Registered BBSPhotonClientAddon to BBS EventBus");
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to register BBSPhotonClientAddon", e);
        }

        /* Register custom keyframe factory override for photon_fx timeline tracks */
        try
        {
            LOGGER.info("Registering UIPhotonFxKeyframeFactory...");
            UIKeyframeFactory.IUIKeyframeFactoryFactory<String> originalStringFactory = 
                UIKeyframeFactory.FACTORIES.get(KeyframeFactories.STRING);
                
            UIKeyframeFactory.FACTORIES.put(
                KeyframeFactories.STRING,
                (keyframe, editor) -> {
                    UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);
                    if (sheet != null && ("photon_fx".equals(sheet.id) || sheet.id.endsWith("/photon_fx")))
                    {
                        return new elgatopro300.bbsphoton.client.gui.UIPhotonFxKeyframeFactory(keyframe, editor);
                    }
                    return originalStringFactory == null ? null : originalStringFactory.create(keyframe, editor);
                }
            );
            LOGGER.info("Successfully registered UIPhotonFxKeyframeFactory");
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to register UIPhotonFxKeyframeFactory", e);
        }

        /* Register track color and icon for photon_fx */
        try
        {
            Field colorsField = UIReplaysEditor.class.getDeclaredField("COLORS");
            colorsField.setAccessible(true);
            Map<String, Integer> colors = (Map<String, Integer>) colorsField.get(null);
            colors.put("photon_fx", 0xFF00B2);

            Field iconsField = UIReplaysEditor.class.getDeclaredField("ICONS");
            iconsField.setAccessible(true);
            Map<String, Icon> icons = (Map<String, Icon>) iconsField.get(null);
            icons.put("photon_fx", Icons.PARTICLE);
            
            LOGGER.info("Successfully registered photon_fx track color (0xFF00B2) and icon (PARTICLE).");
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to register photon_fx track color and icon", e);
        }

        /* Register global cleanup watchdog for Photon effects
         * This ensures effects are stopped when the form renderer is no longer active (e.g. UI closed)
         * preventing global Photon engine corruption */
        ClientTickEvents.END_CLIENT_TICK.register(client ->
        {
            if (!PhotonFormRenderer.activeRenderers.isEmpty())
            {
                List<PhotonFormRenderer> renderers = new ArrayList<>(PhotonFormRenderer.activeRenderers);

                for (PhotonFormRenderer renderer : renderers)
                {
                    renderer.checkCleanup();
                }
            }
        });

        ClientTickEvents.START_CLIENT_TICK.register(client ->
        {
            if (!PhotonFormRenderer.activeRenderers.isEmpty())
            {
                List<PhotonFormRenderer> renderers = new ArrayList<>(PhotonFormRenderer.activeRenderers);

                for (PhotonFormRenderer renderer : renderers)
                {
                    renderer.onClientTickStart();
                }
            }
        });

        /* Add PhotonForm to Extra category after the client has fully started
         * This ensures that BBSResources.init() has already run and we don't get overwritten */
        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
        {
            try
            {
                /* Manual registration of Renderers and Panels (fallback) */
                try
                {
                     LOGGER.info("Attempting manual registration of Photon renderers...");
                     /* Manually register renderer */
                     FormUtilsClient.register(PhotonForm.class, PhotonFormRenderer::new);
                     /* Manually register panel */
                     UIFormEditor.register(PhotonForm.class, UIPhotonForm::new);
                     LOGGER.info("Manually registered PhotonForm renderers and panels");
                }
                catch (Exception e)
                {
                    LOGGER.error("Failed to manually register renderers/panels (might already be registered or API mismatch)", e);
                }

                /* Register source pack for bbs_photon namespace */
                BBSMod.getProvider().register(new InternalAssetsSourcePack("bbs_photon", "assets/bbs_photon", BBSPhotonClient.class));
                BBSMod.getProvider().register(new InternalAssetsSourcePack("bbs_photon_icons", "assets", BBSPhotonClient.class));
                LOGGER.info("Registered 'bbs_photon' source pack.");

                /* Register L10n links directly and reload after packs are in place */
                try
                {
                    BBSModClient.getL10n().register((lang) -> List.of(
                        new Link("bbs_photon", "strings/" + L10n.DEFAULT_LANGUAGE + ".json"),
                        new Link("bbs_photon", "strings/" + lang + ".json")
                    ));
                    BBSModClient.getL10n().reload();
                    LOGGER.info("Registered and reloaded BBS L10n for bbs_photon.");
                }
                catch (Exception e)
                {
                    LOGGER.error("Failed to register/reload L10n", e);
                }

                /* Manual registration for Addons Panel (Fix for Sinytra/Connector) */
                try
                {
                     Link iconLink = new Link("bbs_photon_icons", "bbs_photon/icon.png");
                     
                     AddonInfo info = new AddonInfo(
                        "bbs-photon-addon", 
                        "BBS Photon Addon", 
                        "1.0.0", 
                        "Integration between BBS and Photon particle engine.", 
                        List.of("ElGatoPro300"), 
                        iconLink, 
                        "https://discord.gg/MAHVQBSce6",
                        "",
                        "https://github.com/ElGatoPro300/bbs-photon-addon"
                     );
                     BBSModClient.registerAddon(info);
                     LOGGER.info("Manually registered BBS Photon Addon to BBS Addons Panel.");
                }
                catch (Exception e)
                {
                    LOGGER.error("Failed to manually register addon info", e);
                }

                /* Verify icon resource existence */
                try
                {
                    InputStream stream = BBSPhotonClient.class.getResourceAsStream("/assets/bbs_photon/textures/photon_texture.png");
                    
                    if (stream != null)
                    {
                        LOGGER.info("VERIFICATION: Icon file found in classpath!");
                        stream.close();
                    }
                    else
                    {
                        LOGGER.error("VERIFICATION: Icon file NOT found in classpath at /assets/bbs_photon/textures/photon_texture.png");
                    }
                }
                catch (Exception e)
                {
                    LOGGER.error("VERIFICATION: Error checking icon file", e);
                }

                LOGGER.info("Client started. Injecting PhotonForm into Extra category...");
                
                if (BBSModClient.getFormCategories() != null && 
                    BBSModClient.getFormCategories().getExtraForms() != null && 
                    BBSModClient.getFormCategories().getExtraForms().getExtraCategory() != null)
                {
                    BBSModClient.getFormCategories().getExtraForms().getExtraCategory().addForm(new PhotonForm());
                    LOGGER.info("Successfully added PhotonForm to Extra category.");
                }
                else
                {
                    LOGGER.error("FormCategories or ExtraForms category is null!");
                }
            }
            catch (Exception e)
            {
                LOGGER.error("Failed to add PhotonForm to Extra category", e);
            }
        });
    }
}
