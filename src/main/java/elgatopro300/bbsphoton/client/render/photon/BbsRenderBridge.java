package elgatopro300.bbsphoton.client.render.photon;

import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.utils.pose.Transform;
import mchorse.bbs_mod.ui.framework.UIScreen;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;

import javax.annotation.Nullable;

/** Adapts CML render contexts to Photon's world-space root transform. */
public final class BbsRenderBridge
{
    private BbsRenderBridge()
    {}

    @Nullable
    public static Object currentMenu()
    {
        return UIScreen.getCurrentMenu();
    }

    @Nullable
    public static Level world(IEntity entity)
    {
        Level world = entity == null ? null : entity.getWorld();

        return world == null ? Minecraft.getInstance().level : world;
    }

    public static FxTransform transform(FormRenderingContext context, Transform formTransform)
    {
        PoseStack stack = context.stack;
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3f position = new Vector3f();
        Quaternionf rotation = new Quaternionf();
        Vector3f scale = new Vector3f(1F);

        if (stack != null)
        {
            Matrix4f matrix = new Matrix4f(stack.last().pose());

            if (context.type == FormRenderType.PREVIEW || context.modelRenderer)
            {
                matrix = new Matrix4f().rotation(camera.rotation()).mul(matrix);
            }

            /* CML's FormRenderer already applies the form, overlay and bodypart transforms. */
            matrix.getTranslation(position);
            matrix.getUnnormalizedRotation(rotation);
            matrix.getScale(scale);
            Vec3 origin = camera.position();
            position.add((float) origin.x, (float) origin.y, (float) origin.z);
        }
        else
        {
            IEntity entity = context.entity;

            if (entity != null)
            {
                position.set((float) entity.getX(), (float) entity.getY(), (float) entity.getZ());
                rotation.rotateY((float) Math.toRadians(-entity.getYaw()));
            }

            Vector3f offset = new Vector3f(formTransform.translate);
            rotation.transform(offset);
            position.add(offset);
            rotation.rotateZ((float) Math.toRadians(formTransform.rotate.z))
                .rotateY((float) Math.toRadians(formTransform.rotate.y))
                .rotateX((float) Math.toRadians(formTransform.rotate.x));
            scale.set(formTransform.scale);
        }

        return new FxTransform(position, rotation, scale);
    }

    public record FxTransform(Vector3f position, Quaternionf rotation, Vector3f scale)
    {}
}
