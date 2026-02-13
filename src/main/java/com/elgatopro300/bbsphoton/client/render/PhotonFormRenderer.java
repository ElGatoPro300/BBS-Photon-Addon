package com.elgatopro300.bbsphoton.client.render;

import com.elgatopro300.bbsphoton.forms.PhotonForm;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FXHelper;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import java.lang.reflect.Method;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Matrix4f;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PhotonFormRenderer extends FormRenderer<PhotonForm> implements ITickable {
    // Global registry of active renderers to ensure cleanup
    public static final List<PhotonFormRenderer> activeRenderers = Collections.synchronizedList(new ArrayList<>());

    // Using "bbs_photon" namespace and "textures/photon_texture.png" path
    // This matches assets/bbs_photon/textures/photon_texture.png in the classpath
    public static final Link ICON = new Link("bbs_photon", "textures/photon_texture.png");

    private String lastEffectId = "";
    private EntityEffectExecutor currentEffect;
    private Entity dummyEntity;
    private boolean loggedDebug = false;
    private int tickCounter = 0;
    private long lastAttemptTime = 0;

    public PhotonFormRenderer(PhotonForm form) {
        super(form);
    }
    
    // Watchdog to clean up effects when rendering stops (e.g. form closed)
    private long lastRenderTime = 0;

    /**
     * Called by global client tick to clean up abandoned effects
     */
    public boolean checkCleanup() {
        // Increased timeout to 1000ms (1 second) to prevent accidental cleanup during UI transitions
        if (currentEffect != null && System.currentTimeMillis() - lastRenderTime > 1000) {
            stopCurrentEffect();
            return true;
        }
        return false;
    }

    @Override
    public void tick(IEntity iEntity) {
        if (form.paused.get()) return;
        // Local tick watchdog is secondary to global one
    }

    @Override
    public void render3D(FormRenderingContext context) {
        lastRenderTime = System.currentTimeMillis();
        IEntity iEntity = context.entity;
        
        String effectId = form.effect.get();
        
        // Detect change in effect ID
        if (!Objects.equals(effectId, lastEffectId)) {
             stopCurrentEffect();
             lastEffectId = effectId;
        }

        if (effectId.isEmpty()) return;

        // Start effect if not running, with cooldown (2 seconds)
        if (currentEffect == null || currentEffect.getRuntime() == null || !currentEffect.getRuntime().isAlive()) {
             long now = System.currentTimeMillis();
             if (now - lastAttemptTime > 2000) {
                 lastAttemptTime = now;
                 // Schedule start on main thread to avoid concurrency issues
                 final IEntity entityRef = iEntity;
                 final String effectIdRef = effectId;
                 Minecraft.getInstance().execute(() -> {
                     startEffect(entityRef, effectIdRef);
                 });
             }
        }

        // Update position/rotation every frame for smooth rendering
        if (currentEffect != null && currentEffect.getRuntime() != null && currentEffect.getRuntime().isAlive()) {
            try {
                var runtime = currentEffect.getRuntime();
                var root = runtime.getRoot();

                double x = iEntity.getX();
                double y = iEntity.getY();
                double z = iEntity.getZ();
                float yaw = iEntity.getYaw();

                // Use interpolation for MCEntity to prevent jitter
                // Use reflection to avoid class_xxxx errors (BBS Intermediary vs NeoForge Mojang)
                try {
                    Method getMcEntity = iEntity.getClass().getMethod("getMcEntity");
                    Entity mcEntity = (Entity) getMcEntity.invoke(iEntity);
                    if (mcEntity != null) {
                        float pt = context.transition;
                        x = Mth.lerp(pt, mcEntity.xo, mcEntity.getX());
                        y = Mth.lerp(pt, mcEntity.yo, mcEntity.getY());
                        z = Mth.lerp(pt, mcEntity.zo, mcEntity.getZ());
                        yaw = Mth.lerp(pt, mcEntity.yRotO, mcEntity.getYRot());
                    }
                } catch (Exception e) {
                    // Not an MCEntity or method not found
                }

                // Calculate transform using PoseStack (handles both Model Block and Form transforms)
                PoseStack stack = null;
                try {
                    stack = (PoseStack) context.getClass().getField("stack").get(context);
                } catch (Exception e) {
                    // Ignore reflection error
                }
                
                double finalX, finalY, finalZ;
                Quaternionf finalRot;
                Vector3f finalScale = new Vector3f(1.0f, 1.0f, 1.0f);

                if (stack != null) {
                    var matrix = new Matrix4f(stack.last().pose());
                    
                    Transform t = form.transform.get();
                    Vector3f tPos = t.translate;
                    Vector3f tRot = t.rotate;
                    Vector3f tScale = t.scale;

                    matrix.translate(tPos);
                    matrix.rotate(new Quaternionf()
                        .rotateZ((float) Math.toRadians(tRot.z))
                        .rotateY((float) Math.toRadians(tRot.y))
                        .rotateX((float) Math.toRadians(tRot.x)));
                    matrix.scale(tScale);
                    
                    // Extract translation
                    Vector3f trans = new Vector3f();
                    matrix.getTranslation(trans); // relative to camera
                    
                    // Get camera pos
                    Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                    
                    // Absolute position
                    finalX = trans.x + cameraPos.x;
                    finalY = trans.y + cameraPos.y;
                    finalZ = trans.z + cameraPos.z;
                    
                    // Extract rotation
                    finalRot = new Quaternionf();
                    matrix.getUnnormalizedRotation(finalRot);
                    
                    // Extract scale
                    matrix.getScale(finalScale);
                } else {
                    // Fallback to manual calculation if stack is missing
                    Transform t = form.transform.get();
                    Vector3f tPos = t.translate;
                    Vector3f tRot = t.rotate;
                    Vector3f tScale = t.scale;
                    finalScale = tScale;
    
                    // Calculate rotation
                    Quaternionf entityRot = new Quaternionf().rotateY((float) Math.toRadians(-yaw));
                    Quaternionf formRot = new Quaternionf()
                        .rotateZ((float) Math.toRadians(tRot.z))
                        .rotateY((float) Math.toRadians(tRot.y))
                        .rotateX((float) Math.toRadians(tRot.x));
    
                    finalRot = new Quaternionf(entityRot).mul(formRot);
    
                    // Calculate position with offset
                    Vector3f offset = new Vector3f(tPos);
                    entityRot.transform(offset);
    
                    finalX = x + offset.x;
                    finalY = y + offset.y;
                    finalZ = z + offset.z;
                }

                // Update dummy entity position if it exists
                // IMPORTANT: We must update the dummy entity to the OFFSET position so EntityEffectExecutor
                // (which tracks this entity) renders the effect at the correct location.
                if (dummyEntity != null) {
                    dummyEntity.xo = finalX;
                    dummyEntity.yo = finalY;
                    dummyEntity.zo = finalZ;
                    
                    dummyEntity.setPos(finalX, finalY, finalZ);
                    dummyEntity.setYRot(yaw);
                    dummyEntity.setYHeadRot(yaw);
                    
                    // Ensure dummy entity stays in valid world context if world changes
                    Level entityWorld = null;
                    try {
                        Method getWorld = iEntity.getClass().getMethod("getWorld");
                        entityWorld = (Level) getWorld.invoke(iEntity);
                    } catch (Exception e) {}
                    
                    if (dummyEntity.level() != null && entityWorld != null && dummyEntity.level() != entityWorld) {
                         final String idToLog = lastEffectId;
                         Minecraft.getInstance().execute(() -> {
                             System.out.println("BBSPhoton: World changed for " + idToLog + ", scheduling restart");
                             stopCurrentEffect();
                         });
                    }
                }

                // Update root object directly as well (for rotation/scale and immediate position update)
                if (root != null) {
                     root.updatePos(new Vector3f((float) finalX, (float) finalY, (float) finalZ));
                     root.updateRotation(finalRot);
                     root.updateScale(finalScale);
                }
            } catch (Exception e) {
                // Prevent render crash
                System.out.println("BBSPhoton: Render Exception: " + e.getMessage());
            }
        }
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2) {
        Texture texture = context.render.getTextures().getTexture(ICON);
        
        // Debug logging
        if (!loggedDebug) {
            System.out.println("BBSPhoton: FINAL ATTEMPT. Path: " + ICON + ", Size: " + texture.width + "x" + texture.height);
            loggedDebug = true;
        }

        float min = Math.min(texture.width, texture.height);
        // Avoid division by zero
        if (min <= 0) min = 1;

        int ow = (x2 - x1) - 4;
        int oh = (y2 - y1) - 4;

        int w = (int) ((texture.width / min) * ow);
        int h = (int) ((texture.height / min) * ow);

        int x = x1 + (ow - w) / 2 + 2;
        int y = y1 + (oh - h) / 2 + 2;

        context.batcher.fullTexturedBox(texture, x, y, w, h);
    }

    private void stopCurrentEffect() {
        if (currentEffect != null) {
            final var effectToDestroy = currentEffect;
            final var idToLog = lastEffectId;

            // Schedule destruction to avoid ConcurrentModificationException if Photon is iterating
            Minecraft.getInstance().execute(() -> {
                try {
                    if (effectToDestroy.getRuntime() != null) {
                        System.out.println("BBSPhoton: Stopping effect (scheduled) " + idToLog);
                        effectToDestroy.getRuntime().destroy(false);
                    }
                } catch (Exception e) {
                    System.out.println("BBSPhoton: Error stopping effect: " + e.getMessage());
                    e.printStackTrace();
                }
            });

            currentEffect = null;
            
            // Remove dummy entity from world if it exists
            if (dummyEntity != null) {
                System.out.println("BBSPhoton: Removing dummy entity " + dummyEntity.getId());
                dummyEntity.remove(Entity.RemovalReason.DISCARDED);
                dummyEntity = null;
            }
            
            activeRenderers.remove(this);
        }
    }

    private void startEffect(IEntity iEntity, String effectId) {
        try {
            Level world = null;
            try {
                Method getWorld = iEntity.getClass().getMethod("getWorld");
                world = (Level) getWorld.invoke(iEntity);
            } catch (Exception e) {}
             // Cast to Level if BBS returns something else, assuming BBS remapped to Level
            if (world == null) {
                world = Minecraft.getInstance().level;
            }
            
            if (world == null) {
                return;
            }

            System.out.println("BBSPhoton: Starting effect " + effectId);

            // Create a dummy entity for the effect to attach to
            // This prevents it from following the player
            if (dummyEntity == null || dummyEntity.level() != world) {
                if (dummyEntity != null) {
                    dummyEntity.remove(Entity.RemovalReason.DISCARDED);
                }
                
                // If we are in a UI world (often client level), ensure we don't conflict
                // Some UI worlds might not support adding entities normally?
                // But Photon needs the entity to be in the world's entity list to find it?
                
                dummyEntity = new net.minecraft.world.entity.decoration.ArmorStand(world, iEntity.getX(), iEntity.getY(), iEntity.getZ());
                dummyEntity.setInvisible(true);
                dummyEntity.setNoGravity(true);
                dummyEntity.setInvulnerable(true);
                // NoClip prevents collision and interaction
                dummyEntity.noPhysics = true; // noClip -> noPhysics in Mojang? or still noClip but private? noPhysics is usually public
                
                // Add to world to ensure Photon can find/update it
                // ONLY if it's not already added (check by ID or existence)
                if (world instanceof net.minecraft.client.multiplayer.ClientLevel) {
                    net.minecraft.client.multiplayer.ClientLevel clientWorld = (net.minecraft.client.multiplayer.ClientLevel) world;
                    if (clientWorld.getEntity(dummyEntity.getId()) == null) {
                        clientWorld.addEntity(dummyEntity); // addEntity(Entity)
                    }
                }
            }
            
            // Sync initial position
            dummyEntity.setPos(iEntity.getX(), iEntity.getY(), iEntity.getZ());
            dummyEntity.setYRot(iEntity.getYaw());
            dummyEntity.xo = iEntity.getX();
            dummyEntity.yo = iEntity.getY();
            dummyEntity.zo = iEntity.getZ();

            ResourceLocation location = ResourceLocation.parse(effectId);
            var fx = FXHelper.getFX(location);
            if (fx != null) {
                currentEffect = new EntityEffectExecutor(fx, dummyEntity.level(), dummyEntity, EntityEffectExecutor.AutoRotate.NONE);
                currentEffect.start();
                if (!activeRenderers.contains(this)) {
                    activeRenderers.add(this);
                }
            }
        } catch (Exception e) {
            System.out.println("BBSPhoton: Error starting effect " + effectId);
            e.printStackTrace();
        }
    }
}
