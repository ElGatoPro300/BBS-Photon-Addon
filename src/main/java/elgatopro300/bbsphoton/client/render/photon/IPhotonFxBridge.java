package elgatopro300.bbsphoton.client.render.photon;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

/**
 * Isolates every Photon FX engine call behind one adapter so the renderer / UI never touch
 * {@code com.lowdragmc.photon.*} directly. One concrete implementation is tied to the pinned
 * Photon {@code 2.2.x} line (see {@link PhotonFxBridge}).
 *
 * All methods must be callable from the Minecraft client thread; implementations route to the
 * client executor when necessary.
 */
public interface IPhotonFxBridge
{
    /**
     * Resolve the effect id, create the executor and start the FX attached to {@code hostEntity}.
     * The host entity must already be present in {@code level}; the bridge does not create or
     * remove it.
     *
     * @return an opaque handle, or {@code null} if the id could not be resolved
     */
    @Nullable IPhotonFxHandle start(String effectId, Level level, Entity hostEntity);

    /**
     * Stop and destroy the FX (synchronous when invoked on the client thread). Retires the
     * executor from Photon's per-entity {@code CACHE} so no "zombie" FX keeps ticking after a
     * restart. No-op when {@code handle} is {@code null}.
     */
    void stop(@Nullable IPhotonFxHandle handle);

    /**
     * Every effect id currently loadable through Photon (mounted {@code .fx} and {@code .fxpack}).
     * Used by the picker (C2) alongside the folder-based UX documented in
     * {@code docs/architecture/photon2-compat-plan.md}.
     */
    List<String> listEffectIds();

    /**
     * Photon's user-facing pack folder. Exposed through the bridge so UI code can prefer
     * Photon's public helper without importing Photon classes directly.
     */
    File getFxPacksDir();
}
