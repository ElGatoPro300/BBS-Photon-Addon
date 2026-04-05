package elgatopro300.bbsphoton.client.render;

import elgatopro300.bbsphoton.forms.PhotonForm;
import com.mojang.blaze3d.vertex.PoseStack;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FXHelper;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXRuntime;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import com.lowdragmc.photon.client.gameobject.emitter.Emitter;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Matrix4f;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.lang.reflect.Field;
import java.util.List;


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
    private FormRenderType lastRenderType = null;
    private int lastRenderTick = -1;
    private long lastRenderTickChangeTime = 0;
    private long lastSeekTime = 0;
    private boolean desiredPause = false;
    private boolean lastKnownBBSPaused = false;
    private long lastKnownBBSPauseTime = 0;

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
        this.lastTickTime = System.currentTimeMillis();
    }

    public void onClientTickStart()
    {
        if (this.currentEffect == null || this.currentEffect.getRuntime() == null || !this.currentEffect.getRuntime().isAlive())
        {
            return;
        }

        boolean shouldPause = this.desiredPause;

        this.currentEffect.setPaused(shouldPause);

        if (shouldPause)
        {
            FXRuntime runtime = this.currentEffect.getRuntime();
            for (IFXObject obj : runtime.objects.values())
            {
                obj.setDelay(1);
            }
        }
    }

    private boolean isBBSPaused()
    {
        try
        {
            Screen screen = Minecraft.getInstance().screen;

            if (screen != null && screen.getClass().getSimpleName().contains("UIDashboard"))
            {
                Field panelsField = null;
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
                            Field runnerField = panel.getClass().getDeclaredField("runner");
                            runnerField.setAccessible(true);
                            Object runner = runnerField.get(panel);

                            if (runner != null)
                            {
                                Method isRunning = runner.getClass().getMethod("isRunning");
                                boolean paused = !(boolean) isRunning.invoke(runner);
                                this.lastKnownBBSPaused = paused;
                                this.lastKnownBBSPauseTime = System.currentTimeMillis();
                                return paused;
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
        }

        if (System.currentTimeMillis() - this.lastKnownBBSPauseTime < 1000)
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
                    this.stopCurrentEffect();
                }
            }

            this.lastRenderTick = renderTick;
            boolean tickProgressing = (now - this.lastRenderTickChangeTime) < 120;
            filmPlaying = !bbsPaused && tickProgressing;
        }
        else
        {
            this.lastRenderTick = -1;
            this.lastRenderTickChangeTime = now;
            filmPlaying = true;
        }

        boolean seekWindowActive = (now - this.lastSeekTime) < 250;
        boolean shouldPause = this.form.paused.get() || !filmPlaying || bbsPaused || seekWindowActive;
        if (backwardSeeked)
        {
            shouldPause = true;
        }
        this.desiredPause = shouldPause;

        boolean isAlive = this.currentEffect != null && this.currentEffect.getRuntime() != null && this.currentEffect.getRuntime().isAlive();
        
        /* Start effect if not running, with cooldown (2 seconds) */
        if (this.currentEffect == null || !isAlive)
        {
             if (now - this.lastAttemptTime > 250)
             {
                 if (this.currentEffect == null || !shouldPause || idChanged || backwardSeeked)
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
                if (this.currentEffect.isPaused() != shouldPause)
                {
                     System.out.println("BBSPhoton: Pause state changed to " + shouldPause + " (Film playing: " + filmPlaying + ")");
                     this.currentEffect.setPaused(shouldPause);
                }
                
                /* Always reset delay to 0 in render3D to ensure the global render loop can render the effect.
                 * The "Pause" logic (stopping the tick) is handled in PausableEntityEffectExecutor.updateFXObjectFrame
                 * by setting delay to > 0 after the render setup but before the next tick. */
                FXRuntime runtime = this.currentEffect.getRuntime();
                
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
                        Field uiField = context.getClass().getField("ui");
                        isUI = uiField.getBoolean(context);
                    }
                    catch (Exception e)
                    {
                        /* Ignore */
                    }

                    /* 2. Get stack */
                    try
                    {
                        Field stackField = context.getClass().getField("stack");
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
                    Matrix4f pose = (Matrix4f) last.getClass().getMethod("pose").invoke(last);

                    Matrix4f matrix;

                    try
                    {
                        boolean isPreview = false;

                        try
                        {
                            Field typeField = context.getClass().getField("type");
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
            final EntityEffectExecutor effectToRemove = this.currentEffect;
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
                        List<EntityEffectExecutor> executors = 
                            EntityEffectExecutor.CACHE.get(entityToRemove);
                        
                        if (executors != null)
                        {
                            executors.remove(effectToRemove);
                            if (executors.isEmpty())
                            {
                                EntityEffectExecutor.CACHE.remove(entityToRemove);
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
                if (this.dummyEntity.level() instanceof ClientLevel)
                {
                    ClientLevel clientWorld = (ClientLevel) this.dummyEntity.level();
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
                
                this.dummyEntity = new ArmorStand(world, iEntity.getX(), iEntity.getY(), iEntity.getZ());
                this.dummyEntity.setInvisible(true);
                this.dummyEntity.setNoGravity(true);
                this.dummyEntity.setInvulnerable(true);
                /* NoClip prevents collision and interaction */
                this.dummyEntity.noPhysics = true; 
                
                /* Add to world to ensure Photon can find/update it
                 * ONLY if it's not already added (check by ID or existence) */
                if (world instanceof ClientLevel)
                {
                    ClientLevel clientWorld = (ClientLevel) world;
                    
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
        private final Map<IFXObject, Integer> frozenAges = new IdentityHashMap<>();
        
        public PausableEntityEffectExecutor(FX fx, Level level, Entity entity, AutoRotate autoRotate)
        {
             super(fx, level, entity, autoRotate);
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

            if (!this.paused)
            {
                /* We track tick time for stagnation detection */
                this.lastTick = System.currentTimeMillis();
            }

            if (fxObject instanceof Emitter emitter)
            {
                this.frozenAges.put(fxObject, emitter.getAge());
            }
        }

        @Override
        public void updateFXObjectFrame(IFXObject fxObject, float partialTicks)
        {
            /* Override frame update to handle potentially dead entity gracefully
             * and ensure position updates correctly. */
            FXRuntime runtime = this.getRuntime();
            
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
                runtime.root.updatePos(new Vector3f((float) (position.x + this.offset.x), (float) (position.y + this.offset.y), (float) (position.z + this.offset.z)));
                
                if (this.autoRotate != AutoRotate.NONE)
                {
                    super.updateFXObjectFrame(fxObject, partialTicks);
                }
            }
        }
    }
}
