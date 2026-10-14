package elgatopro300.bbsphoton.client.render.photon;

import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.lowdragmc.photon.client.fx.FXRuntime;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import elgatopro300.bbsphoton.forms.PhotonAnchor;
import elgatopro300.bbsphoton.forms.PhotonForm;

/** Resolves public CML attachment matrices; UUIDs identify the destination Photon objects. */
public final class PhotonObjectAnchors
{
    private final Map<String, Matrix4f> samples = new LinkedHashMap<>();
    private final List<String> diagnostics = new ArrayList<>();
    private final Map<IFXObject, LocalTransform> underlying = new IdentityHashMap<>();

    public void restore()
    {
        for (Map.Entry<IFXObject, LocalTransform> entry : this.underlying.entrySet())
        {
            entry.getKey().transform().localPosition(entry.getValue().position());
            entry.getKey().transform().localRotation(entry.getValue().rotation());
            entry.getKey().transform().localScale(entry.getValue().scale());
        }
        this.underlying.clear();
    }

    public void sample(PhotonForm form, FormRenderingContext context)
    {
        this.samples.clear();
        this.diagnostics.clear();
        if (form.anchors.getList().isEmpty() || context.entity == null)
        {
            return;
        }
        MatrixCache matrices = FormUtilsClient.getRenderer(FormUtils.getRoot(form)).collectMatrices(context.entity, context.getTransition());
        Matrix4f world = BaseFilmController.getMatrixForRenderWithRotation(context.entity, 0D, 0D, 0D, context.getTransition());
        MatrixCacheEntry own = matrices.get(FormUtils.getPath(form));
        if (own.matrix() != null && Math.abs(own.matrix().determinant()) > 0.000001F)
        {
            BbsRenderBridge.FxTransform placement = BbsRenderBridge.transform(context, form.transform.get());
            world = new Matrix4f().translationRotateScale(placement.position(), placement.rotation(), placement.scale())
                .mul(new Matrix4f(own.matrix()).invert());
        }
        for (PhotonAnchor anchor : form.anchors.getList())
        {
            if (!anchor.enabled.get())
            {
                continue;
            }
            MatrixCacheEntry entry = matrices.get(anchor.attachment.get());
            if (entry.matrix() == null)
            {
                this.diagnostics.add(anchor.attachment.get());
                continue;
            }
            this.samples.put(anchor.target.get(), new Matrix4f(world).mul(entry.matrix()).mul(anchor.offset.get().createMatrix()));
        }
    }

    public void apply(FXRuntime runtime)
    {
        for (Map.Entry<String, Matrix4f> entry : this.samples.entrySet())
        {
            IFXObject object = PhotonRuntimeParameters.find(runtime, entry.getKey());
            if (object == null || object == runtime.root)
            {
                continue;
            }
            Matrix4f matrix = entry.getValue();
            this.underlying.putIfAbsent(object, new LocalTransform(new Vector3f(object.transform().localPosition()),
                new Quaternionf(object.transform().localRotation()), new Vector3f(object.transform().localScale())));
            object.updatePos(matrix.getTranslation(new Vector3f()));
            object.updateRotation(matrix.getUnnormalizedRotation(new Quaternionf()).normalize());
            object.updateScale(matrix.getScale(new Vector3f()));
        }
    }

    public List<String> diagnostics()
    {
        return List.copyOf(this.diagnostics);
    }

    public void clear()
    {
        this.restore();
        this.samples.clear();
        this.diagnostics.clear();
    }

    private record LocalTransform(Vector3f position, Quaternionf rotation, Vector3f scale)
    {}
}
