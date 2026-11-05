package ports.smoke;

import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.Arrays;

import com.lowdragmc.photon.PhotonConfig;
import com.lowdragmc.photon.client.postfx.PhotonPostFX;
import com.lowdragmc.photon.client.postfx.runtime.PostEffectStack;
import elgatopro300.bbsphoton.client.gui.UIPhotonForm;
import elgatopro300.bbsphoton.client.render.PhotonFormRenderer;
import elgatopro300.bbsphoton.client.render.photon.PhotonPostEffects;
import elgatopro300.bbsphoton.forms.PhotonForm;

/** Opt-in startup contract check, excluded from distribution artifacts. */
public final class AddonClientSmoke implements ClientModInitializer
{
    private int ticks;

    @Override
    public void onInitializeClient()
    {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.getOverlay() != null || client.currentScreen == null)
            {
                return;
            }
            this.ticks++;
            if (this.ticks == 60)
            {
                try
                {
                    this.check();
                    System.out.println("[Addon ports] REGISTRATION, EDITOR AND SERIALIZATION PASSED");
                }
                catch (Throwable failure)
                {
                    throw new IllegalStateException("Addon port smoke failed", failure);
                }
            }
            if (this.ticks == 100)
            {
                client.scheduleStop();
            }
        });
    }

    private void check()
    {
        PhotonForm form = new PhotonForm();
        System.out.println("[Addon host] " + form.getClass().getProtectionDomain().getCodeSource().getLocation());
        form.photon_fx.set("photon:validation");
        form.paused.set(true);
        form.filmSync.set(true);
        form.seed.set(42L);
        PhotonForm copy = (PhotonForm) FormUtils.fromData(FormUtils.toData(form));
        require("bbs:photon".equals(form.getFormId()), "canonical form id");
        require(copy.paused.get() && copy.filmSync.get() && copy.seed.get() == 42L, "runtime serialization");
        require(form.photon_fx.get().equals(copy.photon_fx.get()), "effect serialization");
        require(FormUtilsClient.getRenderer(form) instanceof PhotonFormRenderer, "renderer registration");
        require(new UIPhotonForm() != null, "editor construction");
        boolean postEnabled = PhotonConfig.INSTANCE.enableCustomEffects.get();
        PhotonConfig.INSTANCE.enableCustomEffects.set(true);
        require(PostEffectStack.resolveEffect(
            PhotonPostFX.parsePath("invert_effect")) != null, "post graph resolution");
        int framebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        form.postEnabled.set(true);
        form.postEffect.set("invert_effect");
        PhotonPostEffects post = new PhotonPostEffects();
        post.setPreview(true);
        try
        {
            post.render(form, null, new FormRenderingContext());
            require(framebuffer == GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
                "preview framebuffer restoration");
            int[] restored = new int[4];
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, restored);
            require(Arrays.equals(viewport, restored), "preview viewport restoration");
            require(scissor == GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),
                "preview scissor restoration");
        }
        finally
        {
            post.close();
            PhotonConfig.INSTANCE.enableCustomEffects.set(postEnabled);
        }
    }

    private static void require(boolean condition, String check)
    {
        if (!condition)
        {
            throw new AssertionError(check);
        }
    }
}
