package elgatopro300.bbsphoton.client.render.photon;

import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXRuntime;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import com.lowdragmc.photon.client.gameobject.emitter.Emitter;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import java.util.IdentityHashMap;
import java.util.Map;

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
 */
public class PausableEntityEffectExecutor extends EntityEffectExecutor
{
    private final Map<IFXObject, Integer> frozenAges = new IdentityHashMap<>();

    private boolean paused = false;
    private long lastTick = 0L;

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

    @Override
    public void updateFXObjectTick(IFXObject fxObject)
    {
        /* Restore super call to ensure entity death checks are performed.
         * EntityEffectExecutor.updateFXObjectTick checks if entity is alive and, if not,
         * destroys the effect and removes it from CACHE. This is critical to prevent
         * "zombie" effects when the dummy entity is removed. */
        super.updateFXObjectTick(fxObject);

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
