package com.elgatopro300.bbsphoton.client.render;

import com.elgatopro300.bbsphoton.forms.PhotonForm;
import com.lowdragmc.photon.client.fx.EntityEffect;
import com.lowdragmc.photon.client.fx.FXHelper;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.util.math.MathHelper;

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
    private EntityEffect currentEffect;
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
                 MinecraftClient.getInstance().execute(() -> {
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
                if (iEntity instanceof MCEntity) {
                    Entity mcEntity = ((MCEntity) iEntity).getMcEntity();
                    if (mcEntity != null) {
                        float pt = context.transition;
                        x = MathHelper.lerp(pt, mcEntity.prevX, mcEntity.getX());
                        y = MathHelper.lerp(pt, mcEntity.prevY, mcEntity.getY());
                        z = MathHelper.lerp(pt, mcEntity.prevZ, mcEntity.getZ());
                        yaw = MathHelper.lerp(pt, mcEntity.prevYaw, mcEntity.getYaw());
                    }
                }

                // Update position
                if (root != null) {
                     root.updatePos(new Vector3f((float) x, (float) y, (float) z));
                     // Update rotation
                     Quaternionf q = new Quaternionf().rotateY((float) Math.toRadians(-yaw));
                     root.updateRotation(q);
                }
                
                // Update dummy entity position if it exists
                // We must update prev values to prevent interpolation artifacts or culling issues
                if (dummyEntity != null) {
                    dummyEntity.prevX = x;
                    dummyEntity.prevY = y;
                    dummyEntity.prevZ = z;
                    dummyEntity.lastRenderX = x;
                    dummyEntity.lastRenderY = y;
                    dummyEntity.lastRenderZ = z;
                    
                    dummyEntity.setPos(x, y, z);
                    dummyEntity.setYaw(yaw);
                    dummyEntity.setHeadYaw(yaw);
                    
                    // Ensure dummy entity stays in valid world context if world changes
                    if (dummyEntity.getWorld() != null && iEntity.getWorld() != null && dummyEntity.getWorld() != iEntity.getWorld()) {
                         // World changed, restart effect safely
                         // But do NOT call stopCurrentEffect() here directly to avoid ConcurrentModification
                         // Just schedule it
                         final String idToLog = lastEffectId;
                         MinecraftClient.getInstance().execute(() -> {
                             System.out.println("BBSPhoton: World changed for " + idToLog + ", scheduling restart");
                             stopCurrentEffect();
                         });
                    }
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
            MinecraftClient.getInstance().execute(() -> {
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
            World world = iEntity.getWorld();
            if (world == null) {
                world = MinecraftClient.getInstance().world;
            }
            
            if (world == null) {
                return;
            }

            System.out.println("BBSPhoton: Starting effect " + effectId);

            // Create a dummy entity for the effect to attach to
            // This prevents it from following the player
            if (dummyEntity == null || dummyEntity.getWorld() != world) {
                if (dummyEntity != null) {
                    dummyEntity.remove(Entity.RemovalReason.DISCARDED);
                }
                
                // If we are in a UI world (often client level), ensure we don't conflict
                // Some UI worlds might not support adding entities normally?
                // But Photon needs the entity to be in the world's entity list to find it?
                
                dummyEntity = new net.minecraft.entity.decoration.ArmorStandEntity(world, iEntity.getX(), iEntity.getY(), iEntity.getZ());
                dummyEntity.setInvisible(true);
                dummyEntity.setNoGravity(true);
                dummyEntity.setInvulnerable(true);
                // NoClip prevents collision and interaction
                dummyEntity.noClip = true;
                
                // Add to world to ensure Photon can find/update it
                // ONLY if it's not already added (check by ID or existence)
                if (world instanceof net.minecraft.client.world.ClientWorld) {
                    net.minecraft.client.world.ClientWorld clientWorld = (net.minecraft.client.world.ClientWorld) world;
                    if (clientWorld.getEntityById(dummyEntity.getId()) == null) {
                        clientWorld.addEntity(dummyEntity.getId(), dummyEntity);
                    }
                }
            }
            
            // Sync initial position
            dummyEntity.setPos(iEntity.getX(), iEntity.getY(), iEntity.getZ());
            dummyEntity.setYaw(iEntity.getYaw());
            dummyEntity.prevX = iEntity.getX();
            dummyEntity.prevY = iEntity.getY();
            dummyEntity.prevZ = iEntity.getZ();

            Identifier location = new Identifier(effectId);
            var fx = FXHelper.getFX(location);
            if (fx != null) {
                currentEffect = new EntityEffect(fx, dummyEntity.getWorld(), dummyEntity, EntityEffect.AutoRotate.NONE);
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
