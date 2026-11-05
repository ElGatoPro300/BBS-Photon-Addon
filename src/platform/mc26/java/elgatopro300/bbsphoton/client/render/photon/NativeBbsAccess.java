package elgatopro300.bbsphoton.client.render.photon;

import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.ui.framework.UIScreen;

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
        return UIScreen.getCurrentMenu();
    }

    public static Level world(IEntity entity)
    {
        Level world = entity == null ? null : entity.getWorld();

        return world == null ? Minecraft.getInstance().level : world;
    }

    static PoseStack stack(FormRenderingContext context)
    {
        return context.stack;
    }

    static Vec3 position(Camera camera)
    {
        return camera.position();
    }
}
