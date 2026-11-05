package elgatopro300.bbsphoton.client.render.photon;

import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel;

import org.joml.Matrix4f;

/** Host adapter for sampled time, viewport mode and entity placement. */
public final class HostRenderAccess
{
    public static boolean exterior()
    {
        return UIModelBlockPanel.toggleRendering;
    }

    public static Matrix4f world(FormRenderingContext context)
    {
        return BaseFilmController.getMatrixForRenderWithRotation(context.entity, 0D, 0D, 0D, context.getTransition());
    }

    public static float preview(FormRenderingContext context)
    {
        return context.previewTick;
    }

    public static float animation(FormRenderingContext context)
    {
        return context.animationTick;
    }
}
