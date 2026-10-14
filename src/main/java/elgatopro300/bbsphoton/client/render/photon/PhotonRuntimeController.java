package elgatopro300.bbsphoton.client.render.photon;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;

import net.minecraft.nbt.CompoundTag;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import com.lowdragmc.photon.client.fx.FXRuntime;
import com.lowdragmc.photon.client.gameobject.FXObject;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import elgatopro300.bbsphoton.client.api.PhotonSignalEvent;
import elgatopro300.bbsphoton.forms.PhotonForm;

/** Per-playback clock and control ownership. No renderer or UI owns simulation details. */
public final class PhotonRuntimeController
{
    private static final int MAX_STEPS_PER_FRAME = 100;
    private final PausableEntityEffectExecutor executor;
    private final PhotonForm form;
    private final PhotonRuntimeParameters parameters = new PhotonRuntimeParameters();
    private final PhotonObjectAnchors anchors = new PhotonObjectAnchors();
    private final PhotonPostEffects postEffects = new PhotonPostEffects();
    private final ArrayDeque<String> signals = new ArrayDeque<>();
    private boolean simulating;
    private boolean dispatchSimulation;
    private boolean sampledBefore;
    private boolean initialized;
    private boolean previousSync;
    private boolean previousSeeded;
    private long previousSeed;
    private float previousOffset;
    private long simulatedTicks;
    private long targetTicks;

    public PhotonRuntimeController(PausableEntityEffectExecutor executor, PhotonForm form)
    {
        this.executor = executor;
        this.form = form;
    }

    public boolean useSeed()
    {
        return this.form.fixedSeed.get() || this.form.filmSync.get();
    }

    public long seed()
    {
        return this.form.seed.get();
    }

    public PhotonPostEffects postEffects()
    {
        return this.postEffects;
    }

    public void beforeEvaluation()
    {
        this.parameters.restore();
        this.anchors.restore();
    }

    public void afterEvaluation()
    {
        FXRuntime runtime = this.executor.getRuntime();
        if (runtime == null)
        {
            return;
        }
        this.parameters.apply(runtime, this.form.parameters.getList());
        if (this.form.speedOverride.get())
        {
            runtime.root.setSelfTimeScale(this.executor.isPaused() ? 0F : this.simulating ? 1F : this.form.speed.get());
        }
        this.anchors.apply(runtime);
    }

    public void render(FormRenderingContext context, boolean paused)
    {
        boolean sync = this.form.filmSync.get();
        float tick = Float.isFinite(context.previewTick) ? context.previewTick : context.animationTick;
        boolean sampled = Float.isFinite(tick);
        float offset = PhotonRuntimeParameters.finite(this.form.startOffset.get());
        float speed = this.form.speedOverride.get() ? this.form.speed.get() : 1F;
        boolean changed = this.initialized && (this.previousSync != sync || this.previousSeeded != this.useSeed()
            || this.previousSeed != this.seed() || this.previousOffset != offset);
        this.postEffects.setPreview(context.ui || context.modelRenderer
            || context.type == FormRenderType.PREVIEW);
        this.anchors.sample(this.form, context);
        if (changed)
        {
            this.reset();
        }
        this.previousSync = sync;
        this.previousSeeded = this.useSeed();
        this.previousSeed = this.seed();
        this.previousOffset = offset;
        this.initialized = true;
        long target = (long) Math.floor(Math.max(0D, (sync && sampled ? tick * speed : 0D) + offset));
        boolean liveStep = this.sampledBefore && !changed && !paused && target - this.simulatedTicks == 1L;
        if (!this.form.paused.get() || !this.sampledBefore)
        {
            this.targetTicks = target;
        }
        if (sync && sampled && this.targetTicks < this.simulatedTicks)
        {
            this.reset();
        }
        boolean catchingUp = this.simulatedTicks < this.targetTicks;
        if (catchingUp)
        {
            this.advance(liveStep);
        }
        FXRuntime runtime = this.executor.getRuntime();
        if (runtime == null)
        {
            return;
        }
        boolean controlled = sync && sampled;
        this.executor.setPaused(paused || controlled || this.simulatedTicks < this.targetTicks);
        if (!this.executor.isPaused())
        {
            runtime.setRate(this.form.speedOverride.get() ? speed : 1F);
        }
        runtime.timelinePlayer.setSignalDispatch(this.form.signals.get() && !catchingUp && !paused);
        runtime.timelinePlayer.setAudioDispatch(!catchingUp && !paused && !controlled);
        this.beforeEvaluation();
        this.afterEvaluation();
        this.postEffects.render(this.form, runtime, context);
        this.sampledBefore = true;
    }

    private void reset()
    {
        FXRuntime previous = this.executor.getRuntime();
        Vector3f position = previous == null ? null : new Vector3f(previous.root.transform().position());
        Quaternionf rotation = previous == null ? null : new Quaternionf(previous.root.transform().rotation());
        Vector3f scale = previous == null ? null : new Vector3f(previous.root.transform().scale());
        this.parameters.restore();
        this.anchors.restore();
        this.simulatedTicks = 0L;
        this.executor.restart();
        FXRuntime current = this.executor.getRuntime();
        if (position != null && current != null)
        {
            current.root.updatePos(position);
            current.root.updateRotation(rotation);
            current.root.updateScale(scale);
        }
    }

    private void advance(boolean live)
    {
        FXRuntime runtime = this.executor.getRuntime();
        if (runtime == null)
        {
            return;
        }
        this.simulating = true;
        this.dispatchSimulation = live;
        runtime.timelinePlayer.setSignalDispatch(live && this.form.signals.get());
        runtime.timelinePlayer.setAudioDispatch(live);
        this.executor.setPaused(false);
        runtime.setRate(1F);
        try
        {
            for (int i = 0; i < MAX_STEPS_PER_FRAME && this.simulatedTicks < this.targetTicks; i++)
            {
                for (IFXObject object : runtime.objects.values())
                {
                    if (object instanceof FXObject fxObject)
                    {
                        fxObject.tick();
                    }
                }
                this.simulatedTicks++;
            }
        }
        finally
        {
            this.simulating = false;
            this.dispatchSimulation = false;
            this.executor.setPaused(true);
        }
    }

    public void signal(String channel, String name, CompoundTag data, double time)
    {
        if (!this.initialized || !this.form.signals.get() || this.simulating && !this.dispatchSimulation)
        {
            return;
        }
        String summary = time + " | " + channel + " | " + name;
        if (this.signals.size() >= 32)
        {
            this.signals.removeFirst();
        }
        this.signals.addLast(summary);
        BBSMod.events.post(new PhotonSignalEvent(this.form, this.form.photon_fx.get(), channel, name, data, time));
    }

    public List<String> diagnostics()
    {
        List<String> result = new ArrayList<>(this.parameters.diagnostics());
        result.addAll(this.anchors.diagnostics());
        result.addAll(this.signals);
        result.add(this.simulatedTicks + " / " + this.targetTicks);
        return List.copyOf(result);
    }

    public void close()
    {
        this.parameters.restore();
        this.anchors.clear();
        this.signals.clear();
        this.postEffects.close();
    }
}
