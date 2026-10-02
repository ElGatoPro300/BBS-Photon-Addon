package elgatopro300.bbsphoton.client.render.photon;

import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXRuntime;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import com.lowdragmc.photon.client.gameobject.emitter.Emitter;
import com.lowdragmc.photon.client.gameobject.emitter.particle.ParticleEmitter;
import com.lowdragmc.photon.client.gameobject.particle.IParticle;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Queue;

/**
 * Entity executor with addon-specific pause and position overrides. Extracted from
 * {@code PhotonFormRenderer} during Phase C so the renderer stops reaching into Photon internals.
 *
 * Pause strategy (today): freeze every emitter's age while paused, restore the stored age on
 * subsequent tick updates. This matches the pre-extraction behavior exactly. The alternative
 * ({@code FXRuntime.setRate(0)}) is being explored in Phase C.5 and is intentionally not applied
 * here yet.
 *
 * Position override: Photon's base {@link EntityEffectExecutor#updateFXObjectFrame} uses
 * {@code entity.getEyePosition(partialTicks)}, which adds the host ArmorStand's eye height
 * (~1.7 blocks) and drifts the FX upward. We render at {@code entity.position()} + the executor
 * offset so the FX stays exactly where the renderer puts the dummy entity.
 *
 * Looping particle trim: Photon 2.2.x can keep prior-cycle particles alive on looping emitters
 * (e.g. {@code forcefield}). After each emitter tick/emit we trim back to the first-cycle
 * budget so a second instance never survives into the render frame.
 */
public class PausableEntityEffectExecutor extends EntityEffectExecutor
{
    private final Map<IFXObject, Integer> frozenAges = new IdentityHashMap<>();

    private boolean paused = false;
    private long lastTick = 0L;
    private int loopParticleBudget = 0;

    public PausableEntityEffectExecutor(FX fx, Level level, Entity entity, AutoRotate autoRotate)
    {
        super(fx, level, entity, autoRotate);
    }

    public boolean isPaused()
    {
        return this.paused;
    }

    public void setPaused(boolean paused)
    {
        if (paused && !this.paused && this.getRuntime() != null)
        {
            this.frozenAges.clear();

            for (IFXObject obj : this.getRuntime().objects.values())
            {
                if (obj instanceof Emitter emitter)
                {
                    this.frozenAges.put(obj, emitter.getAge());
                }
            }
        }

        if (!paused && this.paused)
        {
            this.frozenAges.clear();
        }

        this.paused = paused;
    }

    /**
     * Safety-net trim for end-of-tick / render paths. Prefer the per-emitter trim inside
     * {@link #updateFXObjectTick(IFXObject)}, which runs immediately after Photon's emit.
     */
    public boolean trimLoopingParticles()
    {
        FXRuntime runtime = this.getRuntime();

        if (runtime == null)
        {
            return false;
        }

        boolean trimmed = false;

        for (IFXObject obj : runtime.objects.values())
        {
            if (obj instanceof ParticleEmitter emitter)
            {
                trimmed |= this.trimLoopingEmitter(emitter);
            }
        }

        return trimmed;
    }

    @Override
    public void updateFXObjectTick(IFXObject fxObject)
    {
        /* Restore super call to ensure entity death checks are performed.
         * EntityEffectExecutor.updateFXObjectTick checks if entity is alive and, if not,
         * destroys the effect and removes it from CACHE. This is critical to prevent
         * "zombie" effects when the dummy entity is removed. */
        super.updateFXObjectTick(fxObject);

        /* Trim immediately after Photon's tick/emit for this emitter so the extra loop
         * particle never reaches the next render frame. */
        if (!this.paused && fxObject instanceof ParticleEmitter emitter)
        {
            this.trimLoopingEmitter(emitter);
        }

        if (this.paused)
        {
            if (fxObject instanceof Emitter emitter)
            {
                Integer frozenAge = this.frozenAges.get(fxObject);

                if (frozenAge == null)
                {
                    frozenAge = emitter.getAge();
                    this.frozenAges.put(fxObject, frozenAge);
                }

                emitter.setAge(frozenAge);
            }

            return;
        }

        this.lastTick = System.currentTimeMillis();

        if (fxObject instanceof Emitter emitter)
        {
            this.frozenAges.put(fxObject, emitter.getAge());
        }
    }

    private boolean trimLoopingEmitter(ParticleEmitter emitter)
    {
        if (!emitter.isLooping())
        {
            return false;
        }

        Integer durationObj = emitter.runtime().duration.get();

        if (durationObj == null || durationObj <= 0)
        {
            return false;
        }

        int duration = durationObj;
        int age = emitter.getAge();
        int cycle = age / duration;
        int amount = emitter.getParticleAmount();
        int pending = emitter.waitToAdded.size();

        /* Learn the steady particle count from the first duration cycle. */
        if (cycle <= 0)
        {
            this.loopParticleBudget = Math.max(this.loopParticleBudget, amount + pending);

            return false;
        }

        int budget = Math.max(1, this.loopParticleBudget);
        int total = amount + pending;

        if (total <= budget)
        {
            return false;
        }

        int before = total;

        while (amount > 0 && amount + pending > budget)
        {
            boolean removed = false;

            for (Queue<IParticle> queue : emitter.getParticles().values())
            {
                if (!queue.isEmpty())
                {
                    queue.poll();
                    amount--;
                    removed = true;
                    break;
                }
            }

            if (!removed)
            {
                break;
            }
        }

        while (pending > 0 && amount + pending > budget)
        {
            if (emitter.waitToAdded.poll() == null)
            {
                break;
            }

            pending--;
        }

        System.out.println("BBSPhoton DBG loop-prune"
            + " age=" + age
            + " duration=" + duration
            + " cycle=" + cycle
            + " budget=" + budget
            + " particlesBefore=" + before
            + " particlesAfter=" + (emitter.getParticleAmount() + emitter.waitToAdded.size()));

        return true;
    }

    @Override
    public void updateFXObjectFrame(IFXObject fxObject, float partialTicks)
    {
        /* Override frame update to handle a potentially dead entity gracefully and to use
         * entity.position() instead of entity.getEyePosition(), because the dummy ArmorStand's
         * eye height (~1.7 blocks) would offset the FX. The renderer updates the dummy's
         * position every frame to the exact target, so position() is correct here. */
        FXRuntime runtime = this.getRuntime();

        if (runtime != null && fxObject == runtime.root)
        {
            Vec3 position = this.entity.position();

            runtime.root.updatePos(new Vector3f(
                (float) (position.x + this.offset.x),
                (float) (position.y + this.offset.y),
                (float) (position.z + this.offset.z)
            ));

            if (this.autoRotate != AutoRotate.NONE)
            {
                super.updateFXObjectFrame(fxObject, partialTicks);
            }
        }
    }
}
