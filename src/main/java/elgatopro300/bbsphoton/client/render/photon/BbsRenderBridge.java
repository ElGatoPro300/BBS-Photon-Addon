package elgatopro300.bbsphoton.client.render.photon;

import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.utils.pose.Transform;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;

import javax.annotation.Nullable;

/** Isolates intermediary Minecraft signatures in CML's Fabric JAR from the Mojang addon. */
public final class BbsRenderBridge
{
    private BbsRenderBridge()
    {}

    @Nullable
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

    @Nullable
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

    @Nullable
    private static PoseStack stack(FormRenderingContext context)
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

    public static FxTransform transform(FormRenderingContext context, Transform formTransform)
    {
        PoseStack stack = stack(context);
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
            Vec3 origin = camera.getPosition();
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
