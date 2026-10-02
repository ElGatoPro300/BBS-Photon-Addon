package elgatopro300.bbsphoton.client.render.photon;

import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import com.lowdragmc.photon.client.fx.FXRuntime;
import com.lowdragmc.photon.client.fx.fxpack.FXPacks;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import com.lowdragmc.photon.client.gameobject.emitter.Emitter;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * - {@link PhotonFxHandle#isAlive()} uses Photon {@code isAlive()} only. Gating restarts on
 *   {@code isValid()} caused false restarts when Photon's heartbeat briefly marked a still-
 *   playing looping FX invalid (seen as {@code valid=false} + {@code cacheSize=0} while
 *   {@code alive=true}). {@code isValid()} remains in diagnostics only.
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

            executor.start();

            return new PhotonFxHandle(executor, hostEntity);
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

        PhotonFxHandle(PausableEntityEffectExecutor executor, Entity hostEntity)
        {
            this.executor = executor;
            this.hostEntity = hostEntity;
        }

        @Override
        public boolean isAlive()
        {
            FXRuntime runtime = this.executor.getRuntime();

            if (runtime == null)
            {
                return false;
            }

            /* Restart gating uses isAlive() only. isValid() is a heartbeat probe and can
             * flicker false on MODEL_BLOCK sparse ticks without the FX actually ending. */
            return runtime.isAlive();
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
        public void setDelayAll(int delay)
        {
            FXRuntime runtime = this.executor.getRuntime();

            if (runtime == null)
            {
                return;
            }

            for (IFXObject obj : runtime.objects.values())
            {
                obj.setDelay(delay);
            }
        }

        @Override
        public boolean pruneLoopingParticles()
        {
            return this.executor.trimLoopingParticles();
        }

        @Override
        public String getDiagnostics()
        {
            FXRuntime runtime = this.executor.getRuntime();
            List<EntityEffectExecutor> cached = this.hostEntity == null
                ? null
                : EntityEffectExecutor.CACHE.get(this.hostEntity);
            int cacheSize = cached == null ? 0 : cached.size();

            if (runtime == null)
            {
                return "runtime=null cacheSize=" + cacheSize;
            }

            Map<String, Integer> objectTypes = new LinkedHashMap<>();
            int emitterCount = 0;
            int particleAmount = 0;
            int minEmitterAge = Integer.MAX_VALUE;
            int maxEmitterAge = Integer.MIN_VALUE;

            for (IFXObject obj : runtime.objects.values())
            {
                String type = obj.getClass().getSimpleName();
                objectTypes.put(type, objectTypes.getOrDefault(type, 0) + 1);

                if (obj instanceof Emitter emitter)
                {
                    emitterCount++;
                    int age = emitter.getAge();
                    minEmitterAge = Math.min(minEmitterAge, age);
                    maxEmitterAge = Math.max(maxEmitterAge, age);
                    particleAmount += emitter.getParticleAmount();
                }
            }

            String emitterAges = emitterCount == 0
                ? "none"
                : minEmitterAge + "-" + maxEmitterAge;

            return "alive=" + runtime.isAlive()
                + " valid=" + runtime.isValid()
                + " finished=" + runtime.isFinished()
                + " objects=" + runtime.objects.size()
                + " objectTypes=" + objectTypes
                + " emitters=" + emitterCount
                + " particles=" + particleAmount
                + " emitterAges=" + emitterAges
                + " cacheSize=" + cacheSize;
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
