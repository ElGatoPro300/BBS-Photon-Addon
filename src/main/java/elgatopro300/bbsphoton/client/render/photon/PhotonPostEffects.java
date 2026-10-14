package elgatopro300.bbsphoton.client.render.photon;

import mchorse.bbs_mod.forms.renderers.FormRenderingContext;

import net.minecraft.client.renderer.ShaderInstance;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.LinkedHashMap;
import java.util.Map;

import com.lowdragmc.lowdraglib2.client.shader.HDRTarget;
import com.lowdragmc.photon.client.fx.FXRuntime;
import com.lowdragmc.photon.client.gameobject.emitter.renderpipeline.RenderPassPipeline;
import com.lowdragmc.photon.client.postfx.PhotonPostFX;
import com.lowdragmc.photon.client.postfx.runtime.PostEffectStack;
import com.lowdragmc.photon.client.postfx.runtime.PostFXTargetPool;
import elgatopro300.bbsphoton.forms.PhotonBinding;
import elgatopro300.bbsphoton.forms.PhotonForm;

/** Owns a preview stack and cropped target; world requests use Photon's production pipeline. */
public final class PhotonPostEffects
{
    private final PostEffectStack previewStack = new PostEffectStack();
    private boolean preview;
    private HDRTarget target;
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
        if (this.preview && this.previewStack.hasPending())
        {
            this.renderPreview();
        }
    }

    private void renderPreview()
    {
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        PreviewState state = PreviewState.capture();
        if (viewport[2] <= 0 || viewport[3] <= 0)
        {
            this.previewStack.onFrameEnd();
            return;
        }
        try
        {
            RenderSystem.disableScissor();
            this.target = RenderPassPipeline.resize(this.target, viewport[2], viewport[3], true);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, draw);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.target.frameBufferId);
            GL30.glBlitFramebuffer(viewport[0], viewport[1], viewport[0] + viewport[2], viewport[1] + viewport[3],
                0, 0, viewport[2], viewport[3], GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
            RenderTarget output = this.previewStack.consumeAndExecute(this.target, false);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, output.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
            GL30.glBlitFramebuffer(0, 0, viewport[2], viewport[3], viewport[0], viewport[1],
                viewport[0] + viewport[2], viewport[1] + viewport[3], GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        }
        finally
        {
            this.previewStack.onFrameEnd();
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            state.restore();
        }
    }

    private record PreviewState(boolean depth, boolean blend, boolean cull, boolean depthMask,
                                boolean scissor, int[] scissorBox, int srcRgb, int dstRgb,
                                int srcAlpha, int dstAlpha, int program, int activeTexture,
                                ShaderInstance shader, int[] textures)
    {
        private static PreviewState capture()
        {
            int[] box = new int[4];
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, box);
            int[] textures = new int[12];
            for (int i = 0; i < textures.length; i++)
            {
                textures[i] = RenderSystem.getShaderTexture(i);
            }
            return new PreviewState(GL11.glIsEnabled(GL11.GL_DEPTH_TEST), GL11.glIsEnabled(GL11.GL_BLEND),
                GL11.glIsEnabled(GL11.GL_CULL_FACE), GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                GL11.glIsEnabled(GL11.GL_SCISSOR_TEST), box, GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM), GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE),
                RenderSystem.getShader(), textures);
        }

        private void restore()
        {
            if (this.depth)
            {
                RenderSystem.enableDepthTest();
            }
            else
            {
                RenderSystem.disableDepthTest();
            }
            if (this.blend)
            {
                RenderSystem.enableBlend();
            }
            else
            {
                RenderSystem.disableBlend();
            }
            if (this.cull)
            {
                RenderSystem.enableCull();
            }
            else
            {
                RenderSystem.disableCull();
            }
            RenderSystem.depthMask(this.depthMask);
            RenderSystem.blendFuncSeparate(this.srcRgb, this.dstRgb, this.srcAlpha, this.dstAlpha);
            if (this.scissor)
            {
                RenderSystem.enableScissor(this.scissorBox[0], this.scissorBox[1], this.scissorBox[2], this.scissorBox[3]);
            }
            else
            {
                RenderSystem.disableScissor();
            }
            for (int i = 0; i < this.textures.length; i++)
            {
                RenderSystem.setShaderTexture(i, this.textures[i]);
            }
            RenderSystem.setShader(() -> this.shader);
            GlStateManager._glUseProgram(this.program);
            GlStateManager._activeTexture(this.activeTexture);
        }
    }

    public void close()
    {
        this.previewStack.onFrameEnd();
        if (this.target != null)
        {
            this.target.destroyBuffers();
            this.target = null;
        }
    }
}
