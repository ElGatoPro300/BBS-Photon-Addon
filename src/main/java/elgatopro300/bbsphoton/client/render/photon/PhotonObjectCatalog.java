package elgatopro300.bbsphoton.client.render.photon;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import com.lowdragmc.photon.client.fx.timeline.AnimatedPropertyType;
import com.lowdragmc.photon.client.gameobject.FXObject;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import com.lowdragmc.photon.client.gameobject.RuntimeBinding;

/** Reads authored metadata without constructing, emitting or modifying a runtime. */
public final class PhotonObjectCatalog
{
    private PhotonObjectCatalog()
    {}

    public static List<ObjectInfo> inspect(String effectId)
    {
        List<ObjectInfo> result = new ArrayList<>();
        ResourceLocation location = ResourceLocation.tryParse(effectId);
        FX fx = location == null ? null : FXHelper.getFX(location);
        if (fx == null)
        {
            return result;
        }
        for (IFXObject object : fx.getFxData().objects())
        {
            if (!(object instanceof FXObject target))
            {
                continue;
            }
            List<PropertyInfo> properties = new ArrayList<>();
            for (AnimatedPropertyType property : target.getFXObjectType().animatableProperties(target))
            {
                if (property.path().startsWith("config."))
                {
                    String path = property.path().substring(7);
                    RuntimeBinding binding = target.getFXObjectType().resolveRuntimeBinding(target, path);
                    if (binding != null)
                    {
                        properties.add(new PropertyInfo(path, binding.type.name()));
                    }
                }
            }
            result.add(new ObjectInfo(target.id().toString(), target.getName(), List.copyOf(properties)));
        }
        return List.copyOf(result);
    }

    public record PropertyInfo(String path, String kind)
    {}

    public record ObjectInfo(String id, String name, List<PropertyInfo> properties)
    {}
}
