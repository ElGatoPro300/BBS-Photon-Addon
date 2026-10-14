package elgatopro300.bbsphoton.client.render.photon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXRuntime;
import com.lowdragmc.photon.client.gameobject.FXObject;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import com.lowdragmc.photon.client.postfx.runtime.PostEffectStack;

/** Full-runtime pause for Photon 2.2.8, including timeline, lights and fog objects. */
public class PausableEntityEffectExecutor extends EntityEffectExecutor
{
    private final Map<FXObject, Integer> pausedDelays = new IdentityHashMap<>();

    private boolean paused;
    private float resumeRate = 1F;
    private float resumeRootScale = 1F;
    private PhotonRuntimeController controller;
    private RandomSource seededRandom;

    public PausableEntityEffectExecutor(FX fx, Level level, Entity entity, AutoRotate autoRotate)
    {
        super(fx, level, entity, autoRotate);
    }

    @Override
    public void start()
    {
        if (this.controller != null)
        {
            this.seededRandom = this.controller.useSeed() ? RandomSource.create(this.controller.seed()) : null;
        }
        FXRuntime previous = this.getRuntime();
        super.start();
        FXRuntime runtime = this.getRuntime();

        if (runtime == null || runtime == previous || !(runtime.getRoot() instanceof FXObject root))
        {
            return;
        }

        Runnable tick = root.getOnUpdateTick();
        Consumer<Float> frame = root.getOnUpdateFrame();

        root.setOnUpdateTick(() ->
        {
            if (this.paused)
            {
                /* Do not evaluate authored root-speed tracks while the BBS clock is frozen. */
                root.setSelfTimeScale(0F);
            }
            else if (tick != null)
            {
                if (this.controller != null)
                {
                    this.controller.beforeEvaluation();
                }
                tick.run();
                if (this.controller != null)
                {
                    this.controller.afterEvaluation();
                }
            }
        });
        root.setOnUpdateFrame(partialTicks ->
        {
            if (frame != null)
            {
                if (this.controller != null)
                {
                    this.controller.beforeEvaluation();
                    frame.accept(this.paused ? 0F : partialTicks);
                    this.controller.afterEvaluation();
                    if (this.paused)
                    {
                        root.setSelfTimeScale(0F);
                    }
                }
                else if (!this.paused)
                {
                    frame.accept(partialTicks);
                }
            }
        });

        if (this.paused)
        {
            this.pauseRuntime(runtime);
        }
    }

    public boolean isPaused()
    {
        return this.paused;
    }

    public void setController(PhotonRuntimeController controller)
    {
        this.controller = controller;
    }

    public void restart()
    {
        if (this.runtime != null)
        {
            this.runtime.destroy(true);
        }
        List<EntityEffectExecutor> cached = CACHE.get(this.entity);
        if (cached != null)
        {
            cached.remove(this);
        }
        this.runtime = null;
        this.pausedDelays.clear();
        this.start();
    }

    @Override
    public RandomSource getRandomSource()
    {
        return this.seededRandom == null ? super.getRandomSource() : this.seededRandom;
    }

    @Override
    public void onTimelineSignal(String channel, String name, CompoundTag data, double time)
    {
        if (this.controller != null)
        {
            this.controller.signal(channel, name, data, time);
        }
    }

    @Override
    public PostEffectStack postEffectSink()
    {
        return this.controller == null ? super.postEffectSink() : this.controller.postEffects().sink();
    }

    public void setPaused(boolean paused)
    {
        if (this.paused == paused)
        {
            return;
        }

        this.paused = paused;
        FXRuntime runtime = this.getRuntime();

        if (runtime == null)
        {
            return;
        }

        if (paused)
        {
            this.pauseRuntime(runtime);
        }
        else
        {
            this.restoreDelays();
            runtime.setRate(this.resumeRate);
            runtime.getRoot().setSelfTimeScale(this.resumeRootScale);
            this.pausedDelays.clear();
        }
    }

    private void pauseRuntime(FXRuntime runtime)
    {
        this.resumeRate = runtime.getRate();
        this.resumeRootScale = runtime.getRoot().getSelfTimeScale();
        this.pausedDelays.clear();

        for (IFXObject object : runtime.objects.values())
        {
            if (object instanceof FXObject fxObject)
            {
                this.pausedDelays.put(fxObject, fxObject.getDelay());
            }
        }

        runtime.setRate(0F);
    }

    /** Runs after Photon's engine tick, before rendering, without changing authored delays. */
    public void afterClientTick()
    {
        FXRuntime runtime = this.getRuntime();

        if (!this.paused || runtime == null)
        {
            return;
        }

        /* A zero simulation rate skips the executor's normal entity-liveness callback. */
        super.updateFXObjectTick(runtime.getRoot());
        this.restoreDelays();
    }

    private void restoreDelays()
    {
        for (Map.Entry<FXObject, Integer> entry : this.pausedDelays.entrySet())
        {
            entry.getKey().setDelay(entry.getValue());
        }
    }

    @Override
    public void updateFXObjectFrame(IFXObject fxObject, float partialTicks)
    {
        FXRuntime runtime = this.getRuntime();

        if (runtime != null && fxObject == runtime.root && this.entity.isAlive())
        {
            Vec3 position = this.entity.position();

            /* Keep the form origin instead of adding the dummy ArmorStand's eye height. */
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
