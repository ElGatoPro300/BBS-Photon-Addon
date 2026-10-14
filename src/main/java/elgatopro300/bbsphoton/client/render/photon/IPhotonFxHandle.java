package elgatopro300.bbsphoton.client.render.photon;

import mchorse.bbs_mod.forms.renderers.FormRenderingContext;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * Opaque handle to a running Photon FX. Hides Photon API types from the renderer so that
 * {@code PhotonFormRenderer} stays a coordinator of named steps and never imports
 * {@code com.lowdragmc.photon.*} directly.
 *
 * All methods must be safe to call on the Minecraft client thread. Implementations are
 * expected to be no-ops when the underlying runtime has already finished or been destroyed.
 */
public interface IPhotonFxHandle
{
    void renderControls(FormRenderingContext context, boolean paused);

    List<String> diagnostics();

    /**
     * Whether the FX runtime is still playing and still owned by the particle engine
     * (Photon 2.2.8 {@code isAlive() && isValid()}). Renderers use this to decide whether to
     * restart the effect.
     */
    boolean isAlive();

    /**
     * Whether Photon reports the runtime as naturally finished (one-shot FX reached the end).
     * Distinct from {@link #isAlive()} so renderers can refuse auto-restart for play-once
     * effects without treating engine invalidation the same way.
     */
    boolean isFinished();

    boolean isPaused();

    /**
     * Freeze the runtime simulation and timeline, including lights and fog. Resuming restores
     * its previous playback rate and authored root speed; persistent form data is unchanged.
     */
    void setPaused(boolean paused);

    /**
     * Update the FX root transform in-place. Called every frame from {@code render3D} with
     * world-space position and rotation already resolved by the renderer.
     */
    void updateRoot(Vector3f position, Quaternionf rotation, Vector3f scale);

    /**
     * Maintain paused start delays and executor cleanup after the global Photon tick.
     */
    void afterClientTick();
}
