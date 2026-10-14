package elgatopro300.bbsphoton.client.render;

import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import elgatopro300.bbsphoton.client.render.photon.BbsFilmPauseProbe;
import elgatopro300.bbsphoton.client.render.photon.BbsRenderBridge;
import elgatopro300.bbsphoton.client.render.photon.IPhotonFxBridge;
import elgatopro300.bbsphoton.client.render.photon.IPhotonFxHandle;
import elgatopro300.bbsphoton.client.render.photon.PhotonFxBridge;
import elgatopro300.bbsphoton.forms.PhotonForm;

/**
 * Coordinator for Photon FX forms. Named steps per {@code render3D}:
 * resolve effect id → ensure handle via {@link IPhotonFxBridge} → apply pause →
 * update transform → cleanup when unused.
 *
 * Photon API calls are isolated in {@code PhotonFxBridge}; full-runtime pause lives in
 * {@code PausableEntityEffectExecutor}; CML integration lives in the BBS bridges.
 * See {@code docs/architecture/photon-renderer.md} and {@code photon2-compat-plan.md}.
 */
public class PhotonFormRenderer extends FormRenderer<PhotonForm> implements ITickable
{
    /* Global registry of active renderers to ensure cleanup. Pre-Phase-C debt tracked in
     * docs/architecture/photon-renderer.md §4 "activeRenderers". Do not add more globals. */
    public static final List<PhotonFormRenderer> activeRenderers = Collections.synchronizedList(new ArrayList<>());

    /* Using "bbs_photon" namespace and "textures/photon_texture.png" path.
     * Matches assets/bbs_photon/textures/photon_texture.png in the classpath. */
    public static final Link ICON = new Link("bbs_photon", "textures/photon_texture.png");

    private final IPhotonFxBridge bridge = PhotonFxBridge.get();

    private String lastEffectId = "";
    private IPhotonFxHandle currentHandle;
    private Entity dummyEntity;
    private boolean loggedDebug = false;
    private boolean effectCompleted = false;
    private long lastAttemptTime = 0L;

    /* Watchdog to clean up effects when rendering stops (e.g. form closed) */
    private long lastRenderTime = 0L;
    private long lastTickTime = 0L;
    private FormRenderType lastRenderType = null;
    /* Last seen F7 exterior flag; used to restart FX once on toggle without thrashing on
     * per-frame PREVIEW stencil passes that still run while exterior mode is active. */
    private boolean lastExteriorRendering = false;
    private boolean hasExteriorRenderingSample = false;
    private int lastRenderTick = -1;
    private long lastRenderTickChangeTime = 0L;
    private long lastSeekTime = 0L;
    private boolean desiredPause = false;
    private boolean lastKnownBBSPaused = false;
    private long lastKnownBBSPauseTime = 0L;

    public PhotonFormRenderer(PhotonForm form)
    {
        super(form);
    }

    /**
     * Called by the global client-tick watchdog to clean up abandoned effects.
     * Timeout is 100ms (2 ticks) to clear FX quickly when switching panels; this prevents
     * particle duplication when entering/exiting the Form Editor. The check runs even when
     * {@code currentHandle} is null to drop leaked registry entries.
     */
    public boolean checkCleanup()
    {
        if (System.currentTimeMillis() - this.lastRenderTime > 100L)
        {
            this.stopCurrentEffect();

            return true;
        }

        return false;
    }

    @Override
    public void tick(IEntity iEntity)
    {
        this.lastTickTime = System.currentTimeMillis();
    }

    public void onClientTickStart()
    {
        if (this.currentHandle == null || !this.currentHandle.isAlive())
        {
            return;
        }

        this.currentHandle.setPaused(this.desiredPause);
    }

    public void onClientTickEnd()
    {
        if (this.currentHandle != null)
        {
            this.currentHandle.afterClientTick();
        }
    }

    private boolean isBBSPaused()
    {
        Boolean reading = BbsFilmPauseProbe.readBBSPaused();

        if (reading != null)
        {
            this.lastKnownBBSPaused = reading;
            this.lastKnownBBSPauseTime = System.currentTimeMillis();

            return reading;
        }

        if (System.currentTimeMillis() - this.lastKnownBBSPauseTime < 1000L)
        {
            return this.lastKnownBBSPaused;
        }

        return false;
    }

    @Override
    public void render3D(FormRenderingContext context)
    {
        if (context.isPicking())
        {
            return;
        }

        this.lastRenderTime = System.currentTimeMillis();
        IEntity iEntity = context.entity;

        /* F7 exterior (UIModelBlockPanel.toggleRendering): BBS suppresses the in-UI form draw
         * but UIPickableFormRenderer still runs a PREVIEW pass for stencil picking on the
         * full-screen immersive viewport. That PREVIEW must not drive Photon FX, or it
         * alternates with MODEL_BLOCK every frame (restarts / World-trail stretch). */
        boolean exterior = UIModelBlockPanel.toggleRendering;
        boolean previewPath = context.modelRenderer || context.type == FormRenderType.PREVIEW;

        if (exterior && previewPath)
        {
            return;
        }

        /* Restart once when the user toggles F7, so one-shots replay and World-space trail
         * samples from the previous space are dropped — without reacting to FormRenderType
         * flicker from the stencil PREVIEW pass. */
        boolean exteriorChanged = this.hasExteriorRenderingSample && this.lastExteriorRendering != exterior;

        if (exteriorChanged)
        {
            if (this.currentHandle != null)
            {
                this.stopCurrentEffect();
            }

            this.effectCompleted = false;
            this.lastAttemptTime = 0L;
        }

        this.lastExteriorRendering = exterior;
        this.hasExteriorRenderingSample = true;
        this.lastRenderType = context.type;

        String effectId = this.form.photon_fx.get();

        /* Handle empty effect ID */
        if (effectId.isEmpty())
        {
            if (this.currentHandle != null || !this.lastEffectId.isEmpty())
            {
                this.stopCurrentEffect();
                this.lastEffectId = "";
                this.effectCompleted = false;
            }

            return;
        }

        /* Check if effect ID changed */
        boolean idChanged = !Objects.equals(effectId, this.lastEffectId);

        if (idChanged)
        {
            if (this.currentHandle != null)
            {
                /* Stop is now synchronous on the client thread (PhotonFxBridge), so a
                 * subsequent start cannot race the destroy and stack a second executor. */
                this.stopCurrentEffect();
            }

            /* Update lastEffectId immediately to prevent loop */
            this.lastEffectId = effectId;
            this.effectCompleted = false;

            /* Reset attempt time to allow immediate start (if not paused) */
            this.lastAttemptTime = 0L;
        }

        this.ensurePlayback(context, idChanged, exteriorChanged);

        /* Update position / rotation every frame for smooth rendering */
        if (this.currentHandle != null && this.currentHandle.isAlive())
        {
            try
            {
                if (this.currentHandle.isPaused() != this.desiredPause)
                {
                    this.currentHandle.setPaused(this.desiredPause);
                }

                if (this.dummyEntity != null && this.dummyEntity.isRemoved())
                {
                    /* Fix for FX leak hypothesis #1: previously this path nulled the handle
                     * without destroying the runtime, leaving a CACHE entry behind. */
                    this.stopCurrentEffect();

                    return;
                }

                this.applyTransformAndUpdateRoot(context, iEntity);
            }
            catch (Exception e)
            {
                /* Prevent render crash */
            }
        }
    }

    private void ensurePlayback(FormRenderingContext context, boolean idChanged, boolean exteriorChanged)
    {
        IEntity iEntity = context.entity;
        String effectId = this.form.photon_fx.get();
        long now = System.currentTimeMillis();
        boolean entityContext = context.type == FormRenderType.ENTITY;
        boolean timelineContext = entityContext || Float.isFinite(context.previewTick);
        boolean filmPlaying;
        boolean backwardSeeked = false;
        boolean bbsPaused = timelineContext && this.isBBSPaused();

        if (timelineContext && iEntity != null)
        {
            float timelineTick = Float.isFinite(context.previewTick) ? context.previewTick : context.animationTick;
            int renderTick = Float.isFinite(timelineTick) ? (int) Math.floor(timelineTick) : iEntity.getAge();
            int previousTick = this.lastRenderTick;

            if (previousTick < 0 || renderTick != previousTick)
            {
                this.lastRenderTickChangeTime = now;
            }

            if (previousTick >= 0 && renderTick != previousTick)
            {
                int delta = renderTick - previousTick;

                if (delta != 1)
                {
                    this.lastSeekTime = now;
                }

                if (renderTick < previousTick)
                {
                    backwardSeeked = true;
                    this.effectCompleted = false;
                    this.stopCurrentEffect();
                }
            }

            this.lastRenderTick = renderTick;
            boolean tickProgressing = (now - this.lastRenderTickChangeTime) < 120L;
            filmPlaying = !bbsPaused && tickProgressing;
        }
        else
        {
            this.lastRenderTick = -1;
            this.lastRenderTickChangeTime = now;
            filmPlaying = true;
        }

        boolean seekWindowActive = (now - this.lastSeekTime) < 250L;
        boolean shouldPause = this.form.paused.get() || !filmPlaying || bbsPaused || seekWindowActive;

        if (backwardSeeked)
        {
            shouldPause = true;
        }

        this.desiredPause = shouldPause;

        /* One-shot FX (e.g. hanabi): when Photon marks the runtime finished, stop cleanly and
         * do not auto-restart until the effect id changes or the film seeks backward. */
        if (this.currentHandle != null && this.currentHandle.isFinished())
        {
            this.stopCurrentEffect();
            this.effectCompleted = true;
        }

        boolean isAlive = this.currentHandle != null && this.currentHandle.isAlive();

        /* Start effect if not running, with cooldown (250ms) */
        if (!this.effectCompleted && (this.currentHandle == null || !isAlive))
        {
            if (now - this.lastAttemptTime > 250L)
            {
                if (this.currentHandle == null || !shouldPause || idChanged || backwardSeeked || exteriorChanged)
                {
                    this.lastAttemptTime = now;

                    /* If switching effects or restarting, stop previous just in case */
                    if (this.currentHandle != null)
                    {
                        this.stopCurrentEffect();
                    }

                    /* Schedule start on main thread to avoid concurrency issues */
                    final IEntity entityRef = iEntity;
                    final String effectIdRef = effectId;

                    Minecraft.getInstance().execute(() ->
                    {
                        this.startEffect(entityRef, effectIdRef);
                    });
                }
            }
        }

    }

    private void applyTransformAndUpdateRoot(FormRenderingContext context, IEntity iEntity)
    {
        BbsRenderBridge.FxTransform transform = BbsRenderBridge.transform(context, this.form.transform.get());

        if (this.dummyEntity != null)
        {
            Vector3f position = transform.position();
            this.dummyEntity.xo = position.x;
            this.dummyEntity.yo = position.y;
            this.dummyEntity.zo = position.z;
            this.dummyEntity.setPos(position.x, position.y, position.z);

            Level world = BbsRenderBridge.world(iEntity);

            if (world != null && this.dummyEntity.level() != world)
            {
                this.stopCurrentEffect();
                return;
            }
        }

        this.currentHandle.updateRoot(transform.position(), transform.rotation(), transform.scale());
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        Texture texture = context.render.getTextures().getTexture(ICON);

        /* Debug logging */
        if (!this.loggedDebug)
        {
            this.loggedDebug = true;
        }

        float min = Math.min(texture.width, texture.height);

        /* Avoid division by zero */
        if (min <= 0)
        {
            min = 1;
        }

        int ow = (x2 - x1) - 4;
        int oh = (y2 - y1) - 4;

        int w = (int) ((texture.width / min) * ow);
        int h = (int) ((texture.height / min) * ow);

        int x = x1 + (ow - w) / 2 + 2;
        int y = y1 + (oh - h) / 2 + 2;

        context.batcher.fullTexturedBox(texture, x, y, w, h);
    }

    public void stopCurrentEffect()
    {
        if (this.currentHandle != null)
        {
            /* Bridge.stop is synchronous on the client thread: destroy(force=true) + retire
             * from EntityEffectExecutor.CACHE. This is the leak fix — the previous code
             * scheduled destroy on Minecraft.execute while nulling the local handle, letting
             * a subsequent startEffect race the destroy and stack a second executor. */
            this.bridge.stop(this.currentHandle);
            this.currentHandle = null;
        }

        /* Remove dummy entity from world if it exists */
        if (this.dummyEntity != null)
        {
            this.dummyEntity.remove(Entity.RemovalReason.DISCARDED);

            if (this.dummyEntity.level() instanceof ClientLevel clientWorld)
            {
                clientWorld.removeEntity(this.dummyEntity.getId(), Entity.RemovalReason.DISCARDED);
            }

            this.dummyEntity = null;
        }

        activeRenderers.remove(this);
    }

    private void startEffect(IEntity iEntity, String effectId)
    {
        if (iEntity == null || !Objects.equals(effectId, this.form.photon_fx.get()))
        {
            return;
        }

        try
        {
            Level world = BbsRenderBridge.world(iEntity);

            if (world == null)
            {
                return;
            }

            /* Create / reuse the dummy entity. ArmorStand hosts the FX so it does not follow
             * the player; the renderer updates its position every frame. */
            if (this.dummyEntity == null || this.dummyEntity.level() != world)
            {
                if (this.dummyEntity != null)
                {
                    this.dummyEntity.remove(Entity.RemovalReason.DISCARDED);
                }

                this.dummyEntity = new ArmorStand(world, iEntity.getX(), iEntity.getY(), iEntity.getZ());
                this.dummyEntity.setInvisible(true);
                this.dummyEntity.setNoGravity(true);
                this.dummyEntity.setInvulnerable(true);
                this.dummyEntity.noPhysics = true;

                /* Add to world so Photon can find/update it. */
                if (world instanceof ClientLevel clientWorld)
                {
                    if (clientWorld.getEntity(this.dummyEntity.getId()) == null)
                    {
                        clientWorld.addEntity(this.dummyEntity);
                    }
                }
            }

            /* Sync initial position */
            this.dummyEntity.setPos(iEntity.getX(), iEntity.getY(), iEntity.getZ());
            this.dummyEntity.setYRot(iEntity.getYaw());
            this.dummyEntity.xo = iEntity.getX();
            this.dummyEntity.yo = iEntity.getY();
            this.dummyEntity.zo = iEntity.getZ();

            IPhotonFxHandle handle = this.bridge.start(effectId, this.dummyEntity.level(), this.dummyEntity);

            if (handle != null)
            {
                this.currentHandle = handle;

                if (!activeRenderers.contains(this))
                {
                    activeRenderers.add(this);
                }

                System.out.println("BBSPhoton: Photon particle (" + effectId + ") has been placed successfully.");
            }
            else
            {
                this.stopCurrentEffect();
            }
        }
        catch (Exception e)
        {
            /* Swallow — renderer must not crash on startup failure */
        }
    }
}
