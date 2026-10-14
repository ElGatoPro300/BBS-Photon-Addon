package elgatopro300.bbsphoton.client.render.photon;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.lowdragmc.photon.client.fx.FXRuntime;
import com.lowdragmc.photon.client.fx.timeline.property.ConfigValueType;
import com.lowdragmc.photon.client.gameobject.FXObject;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import com.lowdragmc.photon.client.gameobject.RuntimeBinding;
import com.lowdragmc.photon.client.gameobject.RuntimeValue;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction;
import com.lowdragmc.photon.client.gameobject.emitter.data.number.NumberFunction3;
import elgatopro300.bbsphoton.forms.PhotonBinding;

/** Restores the underlying Photon sample before evaluation, then applies enabled BBS tracks. */
public final class PhotonRuntimeParameters
{
    private final Map<RuntimeValue<?>, SavedValue> underlying = new IdentityHashMap<>();
    private final List<String> diagnostics = new ArrayList<>();

    public static IFXObject find(FXRuntime runtime, String id)
    {
        try
        {
            return runtime.getSceneObject(UUID.fromString(id)) instanceof IFXObject object ? object : null;
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }

    public static Object value(PhotonBinding entry, ConfigValueType type)
    {
        return switch (type)
        {
            case INT -> entry.integer.get();
            case BOOL -> entry.flag.get();
            case FLOAT -> finite(entry.x.get());
            case NUMBER_FUNCTION -> NumberFunction.constant(finite(entry.x.get()));
            case NUMBER_FUNCTION3 -> new NumberFunction3(finite(entry.x.get()), finite(entry.y.get()), finite(entry.z.get()));
            case COLOR -> NumberFunction.color(entry.color.get());
        };
    }

    public static float finite(float value)
    {
        return Float.isFinite(value) ? value : 0F;
    }

    public void restore()
    {
        for (Map.Entry<RuntimeValue<?>, SavedValue> entry : this.underlying.entrySet())
        {
            if (entry.getValue().overridden())
            {
                entry.getKey().setRaw(entry.getValue().value());
            }
            else
            {
                entry.getKey().clear();
            }
        }
        this.underlying.clear();
    }

    public void apply(FXRuntime runtime, List<PhotonBinding> bindings)
    {
        this.diagnostics.clear();
        for (PhotonBinding entry : bindings)
        {
            if (!entry.enabled.get())
            {
                continue;
            }
            IFXObject object = find(runtime, entry.target.get());
            RuntimeBinding binding = object instanceof FXObject target
                ? target.getFXObjectType().resolveRuntimeBinding(target, entry.property.get()) : null;
            if (binding == null)
            {
                this.diagnostics.add(entry.target.get() + "/" + entry.property.get());
                continue;
            }
            RuntimeValue<?> slot = binding.slot((FXObject) object);
            this.underlying.putIfAbsent(slot, new SavedValue(slot.isOverridden(), slot.get()));
            slot.setRaw(value(entry, binding.type));
        }
    }

    public List<String> diagnostics()
    {
        return List.copyOf(this.diagnostics);
    }

    private record SavedValue(boolean overridden, Object value)
    {}
}
