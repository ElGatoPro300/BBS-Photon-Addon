package elgatopro300.bbsphoton.client.render.photon;

import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;

final class NativeBbsAccess
{
    private NativeBbsAccess()
    {
    }

    public static Object currentMenu()
    {
        Object screen = Minecraft.getInstance().screen;

        if (screen == null)
        {
            return null;
        }

        try
        {
            return screen.getClass().getMethod("getMenu").invoke(screen);
        }
        catch (ReflectiveOperationException e)
        {
            return null;
        }
    }

    public static Level world(IEntity entity)
    {
        if (entity != null)
        {
            try
            {
                Object world = entity.getClass().getMethod("getWorld").invoke(entity);

                if (world instanceof Level level)
                {
                    return level;
                }
            }
            catch (ReflectiveOperationException e)
            {
                /* Synthetic previews may not expose an accessible world. */
            }
        }

        return Minecraft.getInstance().level;
    }

    static PoseStack stack(FormRenderingContext context)
    {
        try
        {
            return (PoseStack) context.getClass().getField("stack").get(context);
        }
        catch (ReflectiveOperationException | ClassCastException e)
        {
            return null;
        }
    }

    static Vec3 position(Camera camera)
    {
        return camera.getPosition();
    }
}
