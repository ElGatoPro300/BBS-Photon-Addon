package elgatopro300.bbsphoton.client.render.photon;

import mchorse.bbs_mod.forms.renderers.FormRenderingContext;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.LinkedHashMap;
import java.util.Map;

import com.lowdragmc.photon.client.fx.FXRuntime;
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
    private final NativePostPreview nativePreview = new NativePostPreview(this.previewStack);
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
            this.nativePreview.renderPending();
        }
    }

    public void close()
    {
        this.nativePreview.close();
    }
}
