package elgatopro300.bbsphoton.client.render;

import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.pose.Transform;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import elgatopro300.bbsphoton.client.render.photon.BbsFilmPauseProbe;
import elgatopro300.bbsphoton.client.render.photon.IPhotonFxBridge;
import elgatopro300.bbsphoton.client.render.photon.IPhotonFxHandle;
import elgatopro300.bbsphoton.client.render.photon.PhotonFxBridge;
import elgatopro300.bbsphoton.forms.PhotonForm;

/**
 * Coordinator for Photon FX forms. Named steps per {@code render3D}:
 * resolve effect id → ensure handle via {@link IPhotonFxBridge} → apply pause →
 * update transform → cleanup when unused.
 *
 * Photon API calls are isolated in {@code PhotonFxBridge}; emitter age-freeze lives in
 * {@code PausableEntityEffectExecutor}; BBS pause reflection lives in {@code BbsFilmPauseProbe}.
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

        boolean shouldPause = this.desiredPause;

        this.currentHandle.setPaused(shouldPause);

        if (shouldPause)
        {
            /* Set delay=1 while paused so Photon's global tick does not advance the
             * simulation. The companion setDelayAll(0) call in render3D re-enables the render
             * pass each frame. */
            this.currentHandle.setDelayAll(1);
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
        this.lastRenderTime = System.currentTimeMillis();
        this.lastRenderType = context.type;
        IEntity iEntity = context.entity;

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

        long now = System.currentTimeMillis();
        boolean entityContext = context.type == FormRenderType.ENTITY;
        boolean filmPlaying;
        boolean backwardSeeked = false;
        boolean bbsPaused = entityContext && this.isBBSPaused();

        if (entityContext && iEntity != null)
        {
            int renderTick = iEntity.getAge();
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
                if (this.currentHandle == null || !shouldPause || idChanged || backwardSeeked)
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

        /* Update position / rotation every frame for smooth rendering */
        if (this.currentHandle != null && this.currentHandle.isAlive())
        {
            try
            {
                if (this.currentHandle.isPaused() != shouldPause)
                {
                    this.currentHandle.setPaused(shouldPause);
                }

                /* Always reset delay to 0 in render3D so Photon's render pass can draw the
                 * effect this frame. The pause stop-tick is applied in onClientTickStart by
                 * raising the delay back to 1. */
                this.currentHandle.setDelayAll(0);

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

    private void applyTransformAndUpdateRoot(FormRenderingContext context, IEntity iEntity)
    {
        double x = iEntity.getX();
        double y = iEntity.getY();
        double z = iEntity.getZ();
        float yaw = iEntity.getYaw();

        /* Use interpolation for MCEntity to prevent jitter.
         * Reflection avoids class_xxxx errors (BBS Intermediary vs NeoForge Mojang). */
        try
        {
            Method getMcEntity = iEntity.getClass().getMethod("getMcEntity");
            Entity mcEntity = (Entity) getMcEntity.invoke(iEntity);

            if (mcEntity != null)
            {
                float pt = context.transition;

                x = Mth.lerp(pt, mcEntity.xo, mcEntity.getX());
                y = Mth.lerp(pt, mcEntity.yo, mcEntity.getY());
                z = Mth.lerp(pt, mcEntity.zo, mcEntity.getZ());
                yaw = Mth.lerp(pt, mcEntity.yRotO, mcEntity.getYRot());
            }
        }
        catch (Exception e)
        {
            /* Not an MCEntity or method not found */
        }

        PoseStack stack = null;

        try
        {
            Field stackField = context.getClass().getField("stack");
            stack = (PoseStack) stackField.get(context);
        }
        catch (Exception e)
        {
            /* Ignore */
        }

        double finalX;
        double finalY;
        double finalZ;
        Quaternionf finalRot;
        Vector3f finalScale = new Vector3f(1.0F, 1.0F, 1.0F);

        if (stack != null)
        {
            try
            {
                Object last = stack.getClass().getMethod("last").invoke(stack);
                Matrix4f pose = (Matrix4f) last.getClass().getMethod("pose").invoke(last);
                Matrix4f matrix;

                try
                {
                    boolean isPreview = false;

                    try
                    {
                        Field typeField = context.getClass().getField("type");
                        Object typeObj = typeField.get(context);

                        if (typeObj != null && typeObj.toString().contains("PREVIEW"))
                        {
                            isPreview = true;
                        }
                    }
                    catch (Exception e)
                    {
                        /* Ignore */
                    }

                    if (isPreview)
                    {
                        Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
                        matrix = new Matrix4f().rotation(cam.rotation());
                        matrix.mul(pose);
                    }
                    else
                    {
                        matrix = new Matrix4f(pose);
                    }
                }
                catch (Exception e)
                {
                    matrix = new Matrix4f(pose);
                }

                Transform t = this.form.transform.get();
                Vector3f tPos = t.translate;
                Vector3f tRot = t.rotate;
                Vector3f tScale = t.scale;

                matrix.translate(tPos);
                matrix.rotate(new Quaternionf()
                    .rotateZ((float) Math.toRadians(tRot.z))
                    .rotateY((float) Math.toRadians(tRot.y))
                    .rotateX((float) Math.toRadians(tRot.x)));
                matrix.scale(tScale);

                Vector3f trans = new Vector3f();
                matrix.getTranslation(trans);

                Vec3 mcPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                finalX = trans.x + mcPos.x;
                finalY = trans.y + mcPos.y;
                finalZ = trans.z + mcPos.z;

                finalRot = new Quaternionf();
                matrix.getUnnormalizedRotation(finalRot);

                matrix.getScale(finalScale);
            }
            catch (Exception e)
            {
                /* Fallback below if reflection into PoseStack fails */
                return;
            }
        }
        else
        {
            /* Fallback to manual calculation if stack is missing */
            Transform t = this.form.transform.get();
            Vector3f tPos = t.translate;
            Vector3f tRot = t.rotate;
            Vector3f tScale = t.scale;
            finalScale = tScale;

            Quaternionf entityRot = new Quaternionf().rotateY((float) Math.toRadians(-yaw));
            Quaternionf formRot = new Quaternionf()
                .rotateZ((float) Math.toRadians(tRot.z))
                .rotateY((float) Math.toRadians(tRot.y))
                .rotateX((float) Math.toRadians(tRot.x));

            finalRot = new Quaternionf(entityRot).mul(formRot);

            Vector3f offset = new Vector3f(tPos);
            entityRot.transform(offset);

            finalX = x + offset.x;
            finalY = y + offset.y;
            finalZ = z + offset.z;
        }

        /* Update dummy entity position so PausableEntityEffectExecutor renders the effect at
         * the correct world location (it reads entity.position()). */
        if (this.dummyEntity != null)
        {
            this.dummyEntity.xo = finalX;
            this.dummyEntity.yo = finalY;
            this.dummyEntity.zo = finalZ;

            this.dummyEntity.setPos(finalX, finalY, finalZ);
            this.dummyEntity.setYRot(yaw);
            this.dummyEntity.setYHeadRot(yaw);

            /* Ensure dummy entity stays in valid world context if world changes */
            Level entityWorld = null;

            try
            {
                Method getWorld = iEntity.getClass().getMethod("getWorld");
                entityWorld = (Level) getWorld.invoke(iEntity);
            }
            catch (Exception e)
            {
                /* Ignore */
            }

            if (this.dummyEntity.level() != null && entityWorld != null && this.dummyEntity.level() != entityWorld)
            {
                Minecraft.getInstance().execute(this::stopCurrentEffect);
            }
        }

        /* Update FX root transform via handle (keeps renderer free of Photon API types). */
        this.currentHandle.updateRoot(
            new Vector3f((float) finalX, (float) finalY, (float) finalZ),
            finalRot,
            finalScale
        );
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

    private void stopCurrentEffect()
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
        try
        {
            Level world = null;

            try
            {
                Method getWorld = iEntity.getClass().getMethod("getWorld");
                world = (Level) getWorld.invoke(iEntity);
            }
            catch (Exception e)
            {
                /* Fallback below */
            }

            if (world == null)
            {
                world = Minecraft.getInstance().level;
            }

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
        }
        catch (Exception e)
        {
            /* Swallow — renderer must not crash on startup failure */
        }
    }
}
