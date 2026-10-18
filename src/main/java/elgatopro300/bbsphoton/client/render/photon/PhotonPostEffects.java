package elgatopro300.bbsphoton.client.render.photon;

import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.graphics.RenderPassState;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.LinkedHashMap;
import java.util.Map;

import com.lowdragmc.photon.client.fx.FXRuntime;
import com.lowdragmc.photon.client.render.PhotonFloatTextures;
import com.lowdragmc.photon.client.postfx.PhotonPostFX;
import com.lowdragmc.photon.client.postfx.runtime.PostEffectStack;
import com.lowdragmc.photon.client.postfx.runtime.PostFXTargetPool;
import com.lowdragmc.photon.client.postfx.runtime.RenderGraphExecutor;
import elgatopro300.bbsphoton.forms.PhotonBinding;
import elgatopro300.bbsphoton.forms.PhotonForm;

/** Owns a preview stack and cropped target; world requests use Photon's production pipeline. */
public final class PhotonPostEffects
{
    private final PostEffectStack previewStack = new PostEffectStack();
    private boolean preview;
    private RenderTarget target;
    private int framebuffer;
    private long submittedFrame = -1L;

    public void setPreview(boolean preview)
    {
        this.preview = preview;
    }

    public PostEffectStack sink()
    {
        return this.preview ? this.previewStack : PostEffectStack.GLOBAL;
    }

    public static Object parameter(PhotonBinding entry)
    {
        return switch (entry.kind.get())
        {
            case "INT" -> entry.integer.get();
            case "BOOL" -> entry.flag.get();
            case "COLOR" -> entry.color.get();
            case "VECTOR2" -> new Vector2f(entry.x.get(), entry.y.get());
            case "VECTOR3", "NUMBER_FUNCTION3" -> new Vector3f(entry.x.get(), entry.y.get(), entry.z.get());
            case "VECTOR4" -> new Vector4f(entry.x.get(), entry.y.get(), entry.z.get(), entry.w.get());
            default -> entry.x.get();
        };
    }

    public void render(PhotonForm form, FXRuntime runtime, FormRenderingContext context)
    {
        long frame = PostFXTargetPool.currentFrame();
        if (this.submittedFrame == frame)
        {
            return;
        }
        this.submittedFrame = frame;
        Map<String, Object> parameters = new LinkedHashMap<>();
        for (PhotonBinding entry : form.postParameters.getList())
        {
            if (entry.enabled.get() && !entry.property.get().isBlank())
            {
                parameters.put(entry.property.get(), parameter(entry));
            }
        }
        if (form.postEnabled.get() && !form.postEffect.get().isBlank())
        {
            this.sink().submit(PhotonPostFX.parsePath(form.postEffect.get()), parameters, form.postWeight.get());
        }
        if (this.preview)
        {
            if (this.previewStack.wantsExecution())
            {
                this.renderPreview();
            }
            else
            {
                /* Private stacks are not drained by Photon's global frame-end hook. */
                this.previewStack.onFrameEnd();
            }
        }
    }

    private void renderPreview()
    {
        try (RenderPassState state = new RenderPassState())
        {
            int[] viewport = state.viewport;
            if (viewport[2] <= 0 || viewport[3] <= 0)
            {
                return;
            }
            GlStateManager._disableScissorTest();
            RenderSystem.disableScissorForRenderTypeDraws();
            this.resize(viewport[2], viewport[3]);
            this.attach(this.target.getColorTextureView(), this.target.getDepthTextureView());
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, state.draw);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.framebuffer);
            GL30.glBlitFramebuffer(viewport[0], viewport[1], viewport[0] + viewport[2], viewport[1] + viewport[3],
                0, 0, viewport[2], viewport[3], GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
            state.restore();
            GlStateManager._disableScissorTest();
            RenderSystem.disableScissorForRenderTypeDraws();
            RenderGraphExecutor.FrameInputs inputs = RenderGraphExecutor.FrameInputs.of(
                this.target.getColorTextureView(), this.target.getDepthTextureView());
            GpuTextureView output = this.previewStack.consumeAndExecute(inputs, null);
            if (output != null)
            {
                this.attach(output, null);
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.framebuffer);
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, state.draw);
                GlStateManager._disableScissorTest();
                GL30.glBlitFramebuffer(0, 0, viewport[2], viewport[3], viewport[0], viewport[1],
                    viewport[0] + viewport[2], viewport[1] + viewport[3], GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
                /* Detach pooled output before Photon makes it available to another stack. */
                this.attach(this.target.getColorTextureView(), this.target.getDepthTextureView());
            }
        }
        finally
        {
            this.previewStack.onFrameEnd();
        }
    }

    private void resize(int width, int height)
    {
        if (this.framebuffer == 0)
        {
            this.framebuffer = GL30.glGenFramebuffers();
        }
        if (this.target == null)
        {
            this.target = new PreviewTarget(width, height);
        }
        else if (this.target.width != width || this.target.height != height)
        {
            this.target.resize(width, height);
        }
    }

    private void attach(GpuTextureView color, GpuTextureView depth)
    {
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.framebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D,
            ((GlTexture) color.texture()).glId(), color.baseMipLevel());
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D,
            depth == null ? 0 : ((GlTexture) depth.texture()).glId(), depth == null ? 0 : depth.baseMipLevel());
    }

    public void close()
    {
        this.previewStack.onFrameEnd();
        if (this.framebuffer != 0)
        {
            GL30.glDeleteFramebuffers(this.framebuffer);
            this.framebuffer = 0;
        }
        if (this.target != null)
        {
            this.target.destroyBuffers();
            this.target = null;
        }
    }
    private static final class PreviewTarget extends RenderTarget
    {
        private PreviewTarget(int width, int height)
        {
            super("bbs_photon_preview", true);
            this.resize(width, height);
        }

        @Override
        public void createBuffers(int width, int height)
        {
            super.createBuffers(width, height);
            GpuTexture color = PhotonFloatTextures.createRgba16f("bbs_photon_preview_hdr",
                GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING, width, height);
            if (color == null)
            {
                this.destroyBuffers();
                throw new IllegalStateException("Photon could not allocate the HDR preview target");
            }
            this.colorTextureView.close();
            this.colorTexture.close();
            this.colorTexture = color;
            this.colorTextureView = RenderSystem.getDevice().createTextureView(color);
        }
    }

}
