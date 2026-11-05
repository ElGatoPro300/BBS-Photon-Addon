package elgatopro300.bbsphoton.client.render.photon;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.interps.Lerps;

import org.joml.Matrix4f;

/** Host adapter for sampled time, viewport mode and entity placement. */
public final class HostRenderAccess
{
    public static boolean exterior()
    {
        Object menu = BbsRenderBridge.currentMenu();
        return menu instanceof UIDashboard dashboard
            && dashboard.getPanels().panel instanceof UIModelBlockPanel panel && panel.isRenderingToggled();
    }

    public static Matrix4f world(FormRenderingContext context)
    {
        IEntity entity = context.entity;
        float transition = context.getTransition();
        double x = Lerps.lerp(entity.getPrevX(), entity.getX(), transition);
        double y = Lerps.lerp(entity.getPrevY(), entity.getY(), transition);
        double z = Lerps.lerp(entity.getPrevZ(), entity.getZ(), transition);
        float yaw = Lerps.lerp(entity.getPrevBodyYaw(), entity.getBodyYaw(), transition);
        return new Matrix4f().translation((float) x, (float) y, (float) z).rotateY(MathUtils.toRad(-yaw));
    }

    public static float preview(FormRenderingContext context)
    {
        return context.modelRenderer ? context.modelRendererTick + context.getTransition() : Float.NaN;
    }

    public static float animation(FormRenderingContext context)
    {
        Object menu = BbsRenderBridge.currentMenu();
        if (menu instanceof UIDashboard dashboard && dashboard.getPanels().panel instanceof UIFilmPanel panel)
        {
            float tick = replayTick(panel.getController().editorController, context);
            if (Float.isFinite(tick))
            {
                return tick;
            }
        }
        for (BaseFilmController controller : BBSModClient.getFilms().getControllers())
        {
            float tick = replayTick(controller, context);
            if (Float.isFinite(tick))
            {
                return tick;
            }
        }
        return context.entity == null ? Float.NaN : context.entity.getAge() + context.getTransition();
    }

    private static float replayTick(BaseFilmController controller, FormRenderingContext context)
    {
        if (controller != null && controller.film != null && context.entity != null)
        {
            for (Replay replay : controller.film.replays.getList())
            {
                if (controller.getEntities().get(replay.getId()) == context.entity)
                {
                    return replay.getTick(controller.getTick()) + (controller.paused ? 0F : context.getTransition());
                }
            }
        }
        return Float.NaN;
    }
}
