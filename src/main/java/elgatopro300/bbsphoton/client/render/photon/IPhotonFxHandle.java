package elgatopro300.bbsphoton.client.render.photon;

import org.joml.Quaternionf;
import org.joml.Vector3f;

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
    /**
     * Whether the FX runtime is still playing ({@code FXRuntime.isAlive()} only).
     * Do not gate restarts on Photon {@code isValid()} — heartbeat misses can report invalid
     * while the same looping instance is still rendering, which causes stacked restarts.
     */
    boolean isAlive();

    /**
     * Whether Photon reports the runtime as naturally finished (one-shot FX reached the end).
     * Distinct from {@link #isAlive()} so renderers can refuse auto-restart without treating
     * heartbeat / wipe invalidation the same way.
     */
    boolean isFinished();

    boolean isPaused();

    /**
     * Freeze or unfreeze the FX. Freeze semantics are owned by the adapter (today: emitter age
     * freeze; may be revisited against {@code FXRuntime.setRate(0)} after Phase C.5 conclusion).
     */
    void setPaused(boolean paused);

    /**
     * Update the FX root transform in-place. Called every frame from {@code render3D} with
     * world-space position and rotation already resolved by the renderer.
     */
    void updateRoot(Vector3f position, Quaternionf rotation, Vector3f scale);

    /**
     * Set the per-tick delay on every runtime object. Used by the renderer to coordinate the
     * current pause workaround: 0 during live render3D so Photon renders the frame; 1 during
     * the client tick when paused so Photon does not advance the simulation.
     */
    void setDelayAll(int delay);

    /**
     * For looping particle emitters, clear leftover particles/trails when the emitter age
     * crosses a duration cycle. Photon 2.2.x can keep prior-cycle particles alive, which
     * stacks visually every {@code duration} ticks (e.g. forcefield).
     *
     * @return {@code true} if any particle queues were pruned this call
     */
    boolean pruneLoopingParticles();

    /**
     * Short runtime summary for temporary lifecycle diagnostics. Kept behind the handle so the
     * renderer does not import Photon classes.
     */
    String getDiagnostics();
}
