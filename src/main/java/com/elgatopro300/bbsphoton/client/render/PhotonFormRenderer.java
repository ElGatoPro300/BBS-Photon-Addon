package com.elgatopro300.bbsphoton.client.render;

import com.elgatopro300.bbsphoton.forms.PhotonForm;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FXHelper;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Matrix4f;

import net.minecraft.util.Mth;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PhotonFormRenderer extends FormRenderer<PhotonForm> implements ITickable
{
    /* Global registry of active renderers to ensure cleanup */
    public static final List<PhotonFormRenderer> activeRenderers = Collections.synchronizedList(new ArrayList<>());

    /* Using "bbs_photon" namespace and "textures/photon_texture.png" path
     * This matches assets/bbs_photon/textures/photon_texture.png in the classpath */
    public static final Link ICON = new Link("bbs_photon", "textures/photon_texture.png");

    private String lastEffectId = "";
    private PausableEntityEffectExecutor currentEffect;
    private Entity dummyEntity;
    private boolean loggedDebug = false;
    private long lastAttemptTime = 0;
    
    /* Watchdog to clean up effects when rendering stops (e.g. form closed) */
    private long lastRenderTime = 0;
    private long lastTickTime = 0;

    public PhotonFormRenderer(PhotonForm form)
    {
        super(form);
    }

    /**
     * Called by global client tick to clean up abandoned effects
     */
    public boolean checkCleanup()
    {
        /* Timeout reduced to 100ms (2 ticks) to ensure quick cleanup when switching panels
         * This prevents particle duplication when entering/exiting the Form Editor.
         * We check stagnation regardless of whether currentEffect is null to prevent
         * leaking PhotonFormRenderer instances in activeRenderers. */
        if (System.currentTimeMillis() - this.lastRenderTime > 100)
        {
            this.stopCurrentEffect();
            
            return true;
        }
        
        return false;
    }

    @Override
    public void tick(IEntity iEntity)
    {
        /* Standard ticking handled by Photon's global system
         * We only manage lifecycle (start/stop) in render3D */
        if (this.currentEffect != null)
        {
             this.currentEffect.setPaused(this.form.paused.get());
        }
        
        this.lastTickTime = System.currentTimeMillis();
    }
    
    private boolean isBBSPaused()
    {
        try
        {
            /* Use reflection to check if we are in a dashboard and if the film runner is paused
             * This avoids compilation errors with mapped/unmapped class names and private fields */
            net.minecraft.client.gui.screens.Screen screen = Minecraft.getInstance().screen;
            
            if (screen != null && screen.getClass().getSimpleName().contains("UIDashboard"))
            {
                /* Find 'panels' field */
                java.lang.reflect.Field panelsField = null;
                Class<?> clazz = screen.getClass();
                
                while (clazz != null && panelsField == null)
                {
                    try { panelsField = clazz.getDeclaredField("panels"); } catch (Exception e) { clazz = clazz.getSuperclass(); }
                }
                
                if (panelsField != null)
                {
                    panelsField.setAccessible(true);
                    List<?> panels = (List<?>) panelsField.get(screen);
                    
                    for (Object panel : panels)
                    {
                        if (panel.getClass().getSimpleName().contains("UIFilmPanel"))
                        {
                            /* Check runner paused state via reflection */
                            java.lang.reflect.Field runnerField = panel.getClass().getDeclaredField("runner");
                            runnerField.setAccessible(true);
                            Object runner = runnerField.get(panel);
                            
                            if (runner != null)
                            {
                                Method isRunning = runner.getClass().getMethod("isRunning");
                                return !(boolean) isRunning.invoke(runner);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            /* Ignore */
        }
        
        return false;
    }

    @Override
    public void render3D(FormRenderingContext context)
    {
        this.lastRenderTime = System.currentTimeMillis();
        IEntity iEntity = context.entity;
        
        String effectId = this.form.effect.get();

        /* Handle empty effect ID */
        if (effectId.isEmpty())
        {
             if (this.currentEffect != null || !this.lastEffectId.isEmpty())
             {
                 this.stopCurrentEffect();
                 this.lastEffectId = "";
             }
             
             return;
        }

        /* Check if effect ID changed */
        boolean idChanged = !Objects.equals(effectId, this.lastEffectId);
        
        if (idChanged)
        {
             if (this.currentEffect != null)
             {
                 final String idToLog = this.lastEffectId;
                 
                 Minecraft.getInstance().execute(() ->
                 {
                     System.out.println("BBSPhoton: Effect changed from " + idToLog + " to " + effectId);
                     this.stopCurrentEffect();
                 });
             }
             
             /* Update lastEffectId immediately to prevent loop */
             this.lastEffectId = effectId;
             
             /* Reset attempt time to allow immediate start (if not paused) */
             this.lastAttemptTime = 0; 
        }

        /* Check if effect needs restart (Loop logic)
         * If currentEffect is dead (finished), we check if we should restart it.
         * Paused = TRUE -> Do NOT restart (Play Once).
         * Paused = FALSE -> Restart (Loop). */
        boolean isAlive = this.currentEffect != null && this.currentEffect.getRuntime() != null && this.currentEffect.getRuntime().isAlive();
        
        /* Check for tick stagnation (Entity Pause)
         * If tick hasn't run for > 100ms, assume entity is paused by BBS */
        boolean tickStagnated = (System.currentTimeMillis() - this.lastTickTime > 100);
        
        /* Start effect if not running, with cooldown (2 seconds) */
        if (this.currentEffect == null || !isAlive)
        {
             long now = System.currentTimeMillis();
             
             if (now - this.lastAttemptTime > 2000)
             {
                 /* Only start if Paused is FALSE.
                  * This prevents auto-start on form load if Paused is enabled.
                  * It also prevents looping if Paused is enabled. */
                 if (!this.form.paused.get() && !tickStagnated)
                 {
                     this.lastAttemptTime = now;
                     
                     /* If switching effects or restarting, stop previous just in case */
                     if (this.currentEffect != null)
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

        /* Update position/rotation every frame for smooth rendering */
        if (this.currentEffect != null && this.currentEffect.getRuntime() != null && this.currentEffect.getRuntime().isAlive())
        {
            try
            {
                boolean bbsPaused = this.isBBSPaused();
                boolean formPaused = this.form.paused.get();
                boolean shouldPause = bbsPaused || formPaused || tickStagnated;
                
                if (this.currentEffect.isPaused() != shouldPause)
                {
                     System.out.println("BBSPhoton: Pause state changed to " + shouldPause + " (BBS: " + bbsPaused + ", Form: " + formPaused + ", Stagnated: " + tickStagnated + ")");
                     this.currentEffect.setPaused(shouldPause);
                }
                
                /* Always reset delay to 0 in render3D to ensure the global render loop can render the effect.
                 * The "Pause" logic (stopping the tick) is handled in PausableEntityEffectExecutor.updateFXObjectFrame
                 * by setting delay to > 0 after the render setup but before the next tick. */
                com.lowdragmc.photon.client.fx.FXRuntime runtime = this.currentEffect.getRuntime();
                
                if (runtime != null)
                {
                    for (IFXObject obj : runtime.objects.values())
                    {
                        obj.setDelay(0);
                    }
                }
                
                if (this.dummyEntity != null && this.dummyEntity.isRemoved())
                {
                     System.out.println("BBSPhoton: WARNING - Dummy entity removed! Restarting effect.");
                     this.currentEffect = null;
                     this.dummyEntity = null;
                     
                     return;
                }

                // com.lowdragmc.photon.client.fx.FXRuntime runtime = this.currentEffect.getRuntime(); // already defined
                IFXObject root = runtime.getRoot();

                double x = iEntity.getX();
                double y = iEntity.getY();
                double z = iEntity.getZ();
                float yaw = iEntity.getYaw();

                /* Use interpolation for MCEntity to prevent jitter
                 * Use reflection to avoid class_xxxx errors (BBS Intermediary vs NeoForge Mojang) */
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

                /* Calculate transform using PoseStack (handles both Model Block and Form transforms) */
                PoseStack stack = null;
                boolean isUI = false;
                
                try
                {
                    /* Use reflection to access fields to avoid mapping issues */
                    
                    /* 1. Check 'ui' field */
                    try
                    {
                        java.lang.reflect.Field uiField = context.getClass().getField("ui");
                        isUI = uiField.getBoolean(context);
                    }
                    catch (Exception e)
                    {
                        /* Ignore */
                    }

                    /* 2. Get stack */
                    try
                    {
                        java.lang.reflect.Field stackField = context.getClass().getField("stack");
                        stack = (PoseStack) stackField.get(context);
                    }
                    catch (Exception e)
                    {
                         /* Ignore */
                    }
                }
                catch (Exception e)
                {
                    /* Ignore */
                }

                double finalX, finalY, finalZ;
                Quaternionf finalRot;
                Vector3f finalScale = new Vector3f(1.0f, 1.0f, 1.0f);

                if (stack != null)
                {
                    Object last = stack.getClass().getMethod("last").invoke(stack);
                    org.joml.Matrix4f pose = (org.joml.Matrix4f) last.getClass().getMethod("pose").invoke(last);

                    org.joml.Matrix4f matrix;

                    try
                    {
                        boolean isPreview = false;

                        try
                        {
                            java.lang.reflect.Field typeField = context.getClass().getField("type");
                            Object typeObj = typeField.get(context);

                            if (typeObj != null)
                            {
                                String typeName = typeObj.toString();

                                if (typeName.contains("PREVIEW"))
                                {
                                    isPreview = true;
                                }
                            }
                        }
                        catch (Exception e)
                        {
                        }

                        if (isPreview)
                        {
                            net.minecraft.client.Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
                            matrix = new org.joml.Matrix4f().rotation(cam.rotation());
                            matrix.mul(pose);
                        }
                        else
                        {
                            matrix = new org.joml.Matrix4f(pose);
                        }
                    }
                    catch (Exception e)
                    {
                        matrix = new org.joml.Matrix4f(pose);
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
                else
                {
                    /* Fallback to manual calculation if stack is missing */
                    Transform t = this.form.transform.get();
                    Vector3f tPos = t.translate;
                    Vector3f tRot = t.rotate;
                    Vector3f tScale = t.scale;
                    finalScale = tScale;
    
                    /* Calculate rotation */
                    Quaternionf entityRot = new Quaternionf().rotateY((float) Math.toRadians(-yaw));
                    Quaternionf formRot = new Quaternionf()
                        .rotateZ((float) Math.toRadians(tRot.z))
                        .rotateY((float) Math.toRadians(tRot.y))
                        .rotateX((float) Math.toRadians(tRot.x));
    
                    finalRot = new Quaternionf(entityRot).mul(formRot);
    
                    /* Calculate position with offset */
                    Vector3f offset = new Vector3f(tPos);
                    entityRot.transform(offset);
    
                    finalX = x + offset.x;
                    finalY = y + offset.y;
                    finalZ = z + offset.z;
                }

                /* Update dummy entity position if it exists
                 * IMPORTANT: We must update the dummy entity to the OFFSET position so EntityEffectExecutor
                 * (which tracks this entity) renders the effect at the correct location. */
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
                    catch (Exception e) {}
                    
                    if (this.dummyEntity.level() != null && entityWorld != null && this.dummyEntity.level() != entityWorld)
                    {
                         final String idToLog = this.lastEffectId;
                         
                         Minecraft.getInstance().execute(() ->
                         {
                             System.out.println("BBSPhoton: World changed for " + idToLog + ", scheduling restart");
                             this.stopCurrentEffect();
                         });
                    }
                }

                /* Update root object directly as well (for rotation/scale and immediate position update) */
                if (root != null)
                {
                     root.updatePos(new Vector3f((float) finalX, (float) finalY, (float) finalZ));
                     root.updateRotation(finalRot);
                     root.updateScale(finalScale);
                     
                     /* We rely on Photon's global render loop to call updateFXObjectFrame.
                      * Do NOT call it manually here, as it may conflict with the pause logic (delay trap). */
                }
            }
            catch (Exception e)
            {
                /* Prevent render crash */
                System.out.println("BBSPhoton: Render Exception: " + e.getMessage());
            }
        }
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        Texture texture = context.render.getTextures().getTexture(ICON);
        
        /* Debug logging */
        if (!this.loggedDebug)
        {
            System.out.println("BBSPhoton: FINAL ATTEMPT. Path: " + ICON + ", Size: " + texture.width + "x" + texture.height);
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
        if (this.currentEffect != null)
        {
            final String idToLog = this.lastEffectId;
            final com.lowdragmc.photon.client.fx.EntityEffectExecutor effectToRemove = this.currentEffect;
            final Entity entityToRemove = this.dummyEntity;
            
            Minecraft.getInstance().execute(() ->
            {
                System.out.println("BBSPhoton: Stopping effect " + idToLog);
                try
                {
                    /* Force destroy the runtime */
                    if (effectToRemove.getRuntime() != null)
                    {
                        effectToRemove.getRuntime().destroy(true);
                    }
                    
                    /* Force remove from CACHE explicitly to ensure it stops ticking
                     * This is a failsafe in case the entity death check is delayed or fails */
                    if (entityToRemove != null)
                    {
                        java.util.List<com.lowdragmc.photon.client.fx.EntityEffectExecutor> executors = 
                            com.lowdragmc.photon.client.fx.EntityEffectExecutor.CACHE.get(entityToRemove);
                        
                        if (executors != null)
                        {
                            executors.remove(effectToRemove);
                            if (executors.isEmpty())
                            {
                                com.lowdragmc.photon.client.fx.EntityEffectExecutor.CACHE.remove(entityToRemove);
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    System.out.println("BBSPhoton: Error stopping effect: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            this.currentEffect = null;
            
            /* Remove dummy entity from world if it exists */
            if (this.dummyEntity != null)
            {
                System.out.println("BBSPhoton: Removing dummy entity " + this.dummyEntity.getId());
                this.dummyEntity.remove(Entity.RemovalReason.DISCARDED);
                
                /* Double check: remove from client world list if possible */
                if (this.dummyEntity.level() instanceof net.minecraft.client.multiplayer.ClientLevel)
                {
                    net.minecraft.client.multiplayer.ClientLevel clientWorld = (net.minecraft.client.multiplayer.ClientLevel) this.dummyEntity.level();
                    clientWorld.removeEntity(this.dummyEntity.getId(), Entity.RemovalReason.DISCARDED);
                }
                
                this.dummyEntity = null;
            }
            
            activeRenderers.remove(this);
        }
        else
        {
             /* Even if currentEffect is null, ensure we are removed from activeRenderers */
             activeRenderers.remove(this);
        }
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
            catch (Exception e) {}
            
            /* Cast to Level if BBS returns something else, assuming BBS remapped to Level */
            if (world == null)
            {
                world = Minecraft.getInstance().level;
            }
            
            if (world == null)
            {
                return;
            }

            System.out.println("BBSPhoton: Starting effect " + effectId);

            /* Create a dummy entity for the effect to attach to
             * This prevents it from following the player */
            if (this.dummyEntity == null || this.dummyEntity.level() != world)
            {
                if (this.dummyEntity != null)
                {
                    this.dummyEntity.remove(Entity.RemovalReason.DISCARDED);
                }
                
                /* If we are in a UI world (often client level), ensure we don't conflict
                 * Some UI worlds might not support adding entities normally?
                 * But Photon needs the entity to be in the world's entity list to find it? */
                
                this.dummyEntity = new net.minecraft.world.entity.decoration.ArmorStand(world, iEntity.getX(), iEntity.getY(), iEntity.getZ());
                this.dummyEntity.setInvisible(true);
                this.dummyEntity.setNoGravity(true);
                this.dummyEntity.setInvulnerable(true);
                /* NoClip prevents collision and interaction */
                this.dummyEntity.noPhysics = true; 
                
                /* Add to world to ensure Photon can find/update it
                 * ONLY if it's not already added (check by ID or existence) */
                if (world instanceof net.minecraft.client.multiplayer.ClientLevel)
                {
                    net.minecraft.client.multiplayer.ClientLevel clientWorld = (net.minecraft.client.multiplayer.ClientLevel) world;
                    
                    if (clientWorld.getEntity(this.dummyEntity.getId()) == null)
                    {
                        clientWorld.addEntity(this.dummyEntity); /* addEntity(Entity) */
                    }
                }
            }
            
            /* Sync initial position */
            this.dummyEntity.setPos(iEntity.getX(), iEntity.getY(), iEntity.getZ());
            this.dummyEntity.setYRot(iEntity.getYaw());
            this.dummyEntity.xo = iEntity.getX();
            this.dummyEntity.yo = iEntity.getY();
            this.dummyEntity.zo = iEntity.getZ();

            ResourceLocation location = ResourceLocation.parse(effectId);
            FX fx = FXHelper.getFX(location);
            
            if (fx != null)
            {
                this.currentEffect = new PausableEntityEffectExecutor(fx, this.dummyEntity.level(), this.dummyEntity, EntityEffectExecutor.AutoRotate.NONE);
                this.currentEffect.start();
                
                /* IMPORTANT: We do NOT remove from global CACHE anymore.
                 * This allows Photon to tick the effect normally.
                 * "Paused" functionality is now "No Loop" (Play Once). */

                if (!activeRenderers.contains(this))
                {
                    activeRenderers.add(this);
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("BBSPhoton: Error starting effect " + effectId);
            e.printStackTrace();
        }
    }
    
    /* Custom executor that allows pausing tick updates while keeping the effect alive */
    public static class PausableEntityEffectExecutor extends EntityEffectExecutor
    {
        private boolean paused = false;
        private long lastTick = 0;
        
        public PausableEntityEffectExecutor(FX fx, Level level, Entity entity, AutoRotate autoRotate)
        {
             super(fx, level, entity, autoRotate);
        }
        
        public void setPaused(boolean paused)
        {
            this.paused = paused;
        }

        public boolean isPaused()
        {
             return this.paused;
        }
        
        @Override
        public void updateFXObjectTick(IFXObject fxObject)
        {
            /* Restore super call to ensure entity death checks are performed.
             * EntityEffectExecutor.updateFXObjectTick checks if entity is alive,
             * and if not, destroys the effect and removes it from CACHE.
             * This is critical to prevent "zombie" effects when dummyEntity is removed. */
            super.updateFXObjectTick(fxObject);
            
            if (!this.paused)
            {
                /* We track tick time for stagnation detection */
                this.lastTick = System.currentTimeMillis();
            }
        }

        @Override
        public void updateFXObjectFrame(IFXObject fxObject, float partialTicks)
        {
            /* If paused, we set a high delay on all objects.
             * We set this directly to ensure it applies before the next tick.
             * The render3D() method will reset delay to 0 before the next render pass. */
            if (this.paused && this.getRuntime() != null && fxObject == this.getRuntime().getRoot())
            {
                 com.lowdragmc.photon.client.fx.FXRuntime rt = this.getRuntime();
                 
                 for (IFXObject obj : rt.objects.values())
                 {
                     obj.setDelay(100);
                 }
            }

            /* Override frame update to handle potentially dead entity gracefully
             * and ensure position updates correctly. */
            com.lowdragmc.photon.client.fx.FXRuntime runtime = this.getRuntime();
            
            if (runtime != null && fxObject == runtime.root)
            {
                /* If entity is dead, we might still want to render at last known position?
                 * But for now, let's just use the entity position if available.
                 * We don't check !entity.isAlive() to avoid stopping updates if entity is glitchy. */
                
                /* Use position() instead of getEyePosition() because dummyEntity is an ArmorStand
                 * and getEyePosition() adds height offset (~1.7 blocks) which causes particles to spawn too high.
                 * Since we update dummyEntity pos manually in render3D to the exact target location,
                 * position() gives the correct coordinate. */
                Vec3 position = this.entity.position();
                
                /* Also apply offset */
                runtime.root.updatePos(new org.joml.Vector3f((float) (position.x + this.offset.x), (float) (position.y + this.offset.y), (float) (position.z + this.offset.z)));
                
                if (this.autoRotate != AutoRotate.NONE)
                {
                    super.updateFXObjectFrame(fxObject, partialTicks);
                }
            }
        }
    }
}
