package elgatopro300.bbsphoton.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.ArrayList;
import java.util.List;

import elgatopro300.bbsphoton.client.render.PhotonFormRenderer;

/** Owns client callbacks; BBS feature registration belongs to the addon events. */
public final class BBSPhotonClient
{
    public void initialize()
    {
        ClientTickEvents.START_CLIENT_TICK.register(client ->
        {
            for (PhotonFormRenderer renderer : this.activeRenderers())
            {
                renderer.onClientTickStart();
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client ->
        {
            if (client.level == null)
            {
                this.stopEffects();
                return;
            }

            for (PhotonFormRenderer renderer : this.activeRenderers())
            {
                renderer.checkCleanup();
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> this.stopEffects());
    }

    public void stopEffects()
    {
        for (PhotonFormRenderer renderer : this.activeRenderers())
        {
            renderer.stopCurrentEffect();
        }
    }

    private List<PhotonFormRenderer> activeRenderers()
    {
        synchronized (PhotonFormRenderer.activeRenderers)
        {
            return new ArrayList<>(PhotonFormRenderer.activeRenderers);
        }
    }
}
