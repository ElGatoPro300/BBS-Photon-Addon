package ports.smoke;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.resources.Link;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;

import elgatopro300.bbsphoton.forms.PhotonForm;

/** Dedicated-server verification must not resolve any client addon types. */
public final class AddonServerSmoke implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
        {
            System.out.println("[Addon host version] " + FabricLoader.getInstance().getModContainer("bbs").orElseThrow().getMetadata().getVersion().getFriendlyString());
            PhotonForm form = new PhotonForm();
            if (!(FormUtils.fromData(FormUtils.toData(form)) instanceof PhotonForm))
            {
                throw new AssertionError("Server form serialization");
            }
            try (InputStream stream = BBSMod.getProvider().getAsset(Link.create("bbs_photon:strings/es_es.json")))
            {
                if (stream == null || stream.read() < 0)
                {
                    throw new AssertionError("Server addon resource registration");
                }
            }
            catch (Exception exception)
            {
                throw new AssertionError(exception);
            }
            System.out.println("[Addon ports] DEDICATED SERVER PASSED");
            server.stop(false);
        });
    }
}
