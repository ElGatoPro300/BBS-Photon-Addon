package com.elgatopro300.bbsphoton.client.render;

import com.elgatopro300.bbsphoton.forms.PhotonForm;
import com.lowdragmc.photon.client.fx.EntityEffect;
import com.lowdragmc.photon.client.fx.FXHelper;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import net.minecraft.util.Identifier;
import net.minecraft.entity.Entity;

import java.util.Objects;

public class PhotonFormRenderer extends FormRenderer<PhotonForm> {
    // Using "bbs_photon" namespace and "textures/photon_texture.png" path
    // This matches assets/bbs_photon/textures/photon_texture.png in the classpath
    public static final Link ICON = new Link("bbs_photon", "textures/photon_texture.png");

    private String lastEffectId = "";
    private EntityEffect currentEffect;
    private boolean loggedDebug = false;

    public PhotonFormRenderer(PhotonForm form) {
        super(form);
    }

    @Override
    public void render3D(FormRenderingContext context) {
        IEntity iEntity = context.entity;
        if (!(iEntity instanceof MCEntity)) return;
        
        Entity entity = ((MCEntity) iEntity).getMcEntity();
        if (entity == null) return;

        String effectId = form.effect.get();
        
        // Detect change in effect ID
        if (!Objects.equals(effectId, lastEffectId)) {
             stopCurrentEffect();
             lastEffectId = effectId;
        }

        if (effectId.isEmpty()) return;

        // Start effect if not running
        if (currentEffect == null || currentEffect.getRuntime() == null || !currentEffect.getRuntime().isAlive()) {
             startEffect(entity, effectId);
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
            if (currentEffect.getRuntime() != null) {
                currentEffect.getRuntime().destroy(true);
            }
            currentEffect = null;
        }
    }

    private void startEffect(Entity entity, String effectId) {
        try {
            Identifier location = new Identifier(effectId);
            var fx = FXHelper.getFX(location);
            if (fx != null) {
                currentEffect = new EntityEffect(fx, entity.getWorld(), entity, EntityEffect.AutoRotate.NONE);
                currentEffect.start();
            }
        } catch (Exception e) {
            // Invalid resource location or other error
        }
    }
}
