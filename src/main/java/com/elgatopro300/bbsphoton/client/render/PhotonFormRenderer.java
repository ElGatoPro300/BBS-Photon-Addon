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

import java.util.Objects;

public class PhotonFormRenderer extends FormRenderer<PhotonForm> implements ITickable {
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
    
    @Override
    public void tick(IEntity iEntity) {
        if (form.paused.get()) return;

        // Note: EntityEffect usually hooks into the world/entity tick.
        // For StubEntity (editor), the game might be paused or the entity not in world list.
        // We cannot manually tick EntityEffect as it doesn't expose a public tick() method.
        // However, updating position in render3D should ensure it renders at the correct location.
    }

    @Override
    public void render3D(FormRenderingContext context) {
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
                 startEffect(iEntity, effectId);
             }
        }

        // Update position/rotation every frame for smooth rendering
        if (currentEffect != null && currentEffect.getRuntime() != null && currentEffect.getRuntime().isAlive()) {
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
            root.updatePos(new Vector3f((float) x, (float) y, (float) z));

            // Update rotation
            Quaternionf q = new Quaternionf().rotateY((float) Math.toRadians(-yaw));
            root.updateRotation(q);
            
            // If using dummy entity or player override, update its position too
            // Note: We don't use dummyEntity anymore, but if we did, or if we need to sync something else
            if (dummyEntity != null) {
                dummyEntity.setPos(x, y, z);
                dummyEntity.setYaw(yaw);
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

    @Override
    protected void finalize() throws Throwable {
        // Schedule cleanup on main thread to avoid ConcurrentModificationException
        // caused by modifying particle lists from the Finalizer thread while Render thread iterates them.
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.execute(() -> {
                    stopCurrentEffect();
                });
            }
        } catch (Exception e) {
            // Ignore errors if client is already shut down
        }
        super.finalize();
    }

    private void stopCurrentEffect() {
        if (currentEffect != null) {
            try {
                if (currentEffect.getRuntime() != null) {
                    currentEffect.getRuntime().destroy(true);
                }
            } catch (Exception e) {
                // Ignore errors during destruction
            }
            currentEffect = null;
        }
    }

    private void startEffect(IEntity iEntity, String effectId) {
        try {
            Entity entity = null;
            if (iEntity instanceof MCEntity) {
                entity = ((MCEntity) iEntity).getMcEntity();
            } else {
                // For editor/preview (StubEntity), use the client player as the host
                // This ensures the entity is valid and in the world, preventing Photon crashes.
                // We will manually override the position in render3D.
                entity = MinecraftClient.getInstance().player;
            }

            if (entity == null) {
                if (tickCounter++ % 100 == 0) {
                     System.out.println("BBSPhoton: Could not get entity for effect (Player is null?).");
                }
                return;
            }

            Identifier location = new Identifier(effectId);
            // System.out.println("BBSPhoton: Attempting to load effect: " + location);
            var fx = FXHelper.getFX(location);
            if (fx != null) {
                // System.out.println("BBSPhoton: Starting effect " + effectId + " on " + entity.getName().getString());
                currentEffect = new EntityEffect(fx, entity.getWorld(), entity, EntityEffect.AutoRotate.NONE);
                currentEffect.start();
            } else {
                // System.out.println("BBSPhoton: Failed to find effect " + effectId);
            }
        } catch (Exception e) {
            System.out.println("BBSPhoton: Error starting effect " + effectId);
            e.printStackTrace();
        }
    }
}
