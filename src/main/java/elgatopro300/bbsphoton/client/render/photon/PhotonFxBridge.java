package elgatopro300.bbsphoton.client.render.photon;

import mchorse.bbs_mod.forms.renderers.FormRenderingContext;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import com.lowdragmc.photon.client.fx.FXRuntime;
import com.lowdragmc.photon.client.fx.fxpack.FXPacks;
import com.lowdragmc.photon.client.gameobject.IFXObject;

/**
 * Photon {@code 2.2.x} adapter — the single place in this addon that talks to
 * {@code com.lowdragmc.photon.client.fx.*}. Introduced in Phase C so the renderer stops owning
 * the Photon API, so pause/scrub cleanup can evolve without touching render math, and so that
 * FX stop is always synchronous on the client thread (prevents the stacking leak documented in
 * {@code docs/architecture/photon2-compat-plan.md}).
 *
 * Leak fixes relative to the pre-Phase-C renderer:
 * - {@link #stop(IPhotonFxHandle)} always issues {@code destroy(true)} AND retires from
 *   {@code EntityEffectExecutor.CACHE}, synchronously when invoked on the client thread. The
 *   old code scheduled destroy via {@code Minecraft.execute(...)} and immediately nulled the
 *   local reference, letting a subsequent start race the destroy and stack a second executor.
 * - {@link PhotonFxHandle#isAlive()} combines Photon {@code 2.2.7} {@code isAlive()} with the
 *   new {@code isValid()} probe, so runtimes silently discarded by the engine (level change,
 *   {@code /photon_client clear_particles}) are reported dead and get a clean restart instead
 *   of an orphaned CACHE entry.
 */
public final class PhotonFxBridge implements IPhotonFxBridge
{
    private static final PhotonFxBridge INSTANCE = new PhotonFxBridge();

    private PhotonFxBridge()
    {
    }

    public static PhotonFxBridge get()
    {
        return INSTANCE;
    }

    @Nullable
    @Override
    public IPhotonFxHandle start(String effectId, Level level, Entity hostEntity)
    {
        return this.start(effectId, level, hostEntity, null);
    }

    @Override
    public IPhotonFxHandle start(String effectId, Level level, Entity hostEntity, elgatopro300.bbsphoton.forms.PhotonForm form)
    {
        if (effectId == null || effectId.isEmpty() || level == null || hostEntity == null)
        {
            return null;
        }

        try
        {
            ResourceLocation location = ResourceLocation.parse(effectId);
            FX fx = FXHelper.getFX(location);

            if (fx == null)
            {
                return null;
            }

            PausableEntityEffectExecutor executor = new PausableEntityEffectExecutor(
                fx, level, hostEntity, EntityEffectExecutor.AutoRotate.NONE
            );

            PhotonRuntimeController controller = form == null ? null : new PhotonRuntimeController(executor, form);
            executor.setController(controller);
            executor.start();

            return new PhotonFxHandle(executor, hostEntity, controller);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    @Override
    public void stop(@Nullable IPhotonFxHandle handle)
    {
        if (!(handle instanceof PhotonFxHandle photonHandle))
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.isSameThread())
        {
            this.destroyAndRetire(photonHandle);
        }
        else
        {
            mc.execute(() -> this.destroyAndRetire(photonHandle));
        }
    }

    @Override
    public List<String> listEffectIds()
    {
        List<String> ids = new ArrayList<>();

        try
        {
            for (ResourceLocation location : FXHelper.listAllFX())
            {
                ids.add(location.toString());
            }
        }
        catch (Exception e)
        {
            /* Fall through with an empty list; picker UX falls back to folder scans. */
        }

        return ids;
    }

    @Override
    public File getFxPacksDir()
    {
        return FXPacks.getFxPacksDir();
    }

    private void destroyAndRetire(PhotonFxHandle handle)
    {
        try
        {
            PausableEntityEffectExecutor executor = handle.executor();
            if (handle.controller != null)
            {
                handle.controller.close();
            }
            FXRuntime runtime = executor.getRuntime();

            if (runtime != null)
            {
                /* force = true drops any visible remnants immediately so a restart cannot
                 * overlap with the previous emitter. */
                runtime.destroy(true);
            }

            /* Failsafe: evict the executor from Photon's per-entity CACHE even if the
             * entity-is-alive branch inside Photon decides to retire late. */
            Entity hostEntity = handle.hostEntity();

            if (hostEntity != null)
            {
                List<EntityEffectExecutor> executors = EntityEffectExecutor.CACHE.get(hostEntity);

                if (executors != null)
                {
                    executors.remove(executor);

                    if (executors.isEmpty())
                    {
                        EntityEffectExecutor.CACHE.remove(hostEntity);
                    }
                }
            }
        }
        catch (Exception e)
        {
            /* Swallow — stop() must never crash the render / tick caller. */
        }
    }

    /**
     * Opaque handle implementation. Package-private accessors give the bridge raw executor /
     * entity references it needs to retire from Photon's CACHE on stop. The renderer only sees
     * the {@link IPhotonFxHandle} surface.
     */
    static final class PhotonFxHandle implements IPhotonFxHandle
    {
        private final PausableEntityEffectExecutor executor;
        private final Entity hostEntity;
        private final PhotonRuntimeController controller;

        PhotonFxHandle(PausableEntityEffectExecutor executor, Entity hostEntity, PhotonRuntimeController controller)
        {
            this.executor = executor;
            this.hostEntity = hostEntity;
            this.controller = controller;
        }

        @Override
        public void renderControls(FormRenderingContext context, boolean paused)
        {
            if (this.controller != null)
            {
                this.controller.render(context, paused);
            }
        }

        @Override
        public List<String> diagnostics()
        {
            return this.controller == null ? List.of() : this.controller.diagnostics();
        }

        @Override
        public boolean isAlive()
        {
            FXRuntime runtime = this.executor.getRuntime();

            if (runtime == null)
            {
                return false;
            }

            /* Photon 2.2.7: isValid() catches engine-side discards (level change,
             * /photon_client clear_particles) that isAlive() alone would miss. Treating
             * either as "not alive" makes the renderer restart cleanly instead of leaking. */
            return runtime.isAlive() && runtime.isValid();
        }

        @Override
        public boolean isFinished()
        {
            FXRuntime runtime = this.executor.getRuntime();

            return runtime != null && runtime.isFinished();
        }

        @Override
        public boolean isPaused()
        {
            return this.executor.isPaused();
        }

        @Override
        public void setPaused(boolean paused)
        {
            this.executor.setPaused(paused);
        }

        @Override
        public void updateRoot(Vector3f position, Quaternionf rotation, Vector3f scale)
        {
            FXRuntime runtime = this.executor.getRuntime();

            if (runtime == null)
            {
                return;
            }

            IFXObject root = runtime.getRoot();

            if (root == null)
            {
                return;
            }

            root.updatePos(position);
            root.updateRotation(rotation);
            root.updateScale(scale);
        }

        @Override
        public void afterClientTick()
        {
            this.executor.afterClientTick();
        }

        PausableEntityEffectExecutor executor()
        {
            return this.executor;
        }

        Entity hostEntity()
        {
            return this.hostEntity;
        }
    }
}
