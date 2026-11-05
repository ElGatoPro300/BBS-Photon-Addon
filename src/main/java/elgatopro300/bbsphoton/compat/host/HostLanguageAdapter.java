package elgatopro300.bbsphoton.compat.host;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;

/** Select the host before resolving any host-specific method signatures. */
public final class HostLanguageAdapter implements LanguageAdapter
{
    public static String implementation(String canonical)
    {
        ModContainer host = FabricLoader.getInstance().getModContainer("bbs")
            .orElseThrow(() -> new IllegalStateException("Missing BBS host"));
        CustomValue marker = host.getMetadata().getCustomValue("bbs:fork_id");

        if (marker != null && marker.getType() == CustomValue.CvType.STRING && "CML".equals(marker.getAsString()))
        {
            return canonical;
        }

        String version = host.getMetadata().getVersion().getFriendlyString();
        if ("BBS FS mod".equals(host.getMetadata().getName()) && ("2.7-1.21.1".equals(version)))
        {
            return canonical.replace("elgatopro300.bbsphoton.", "elgatopro300.bbsphoton.fs.");
        }

        throw new IllegalStateException("Unsupported BBS host: " + host.getMetadata().getName() + " " + version);
    }

    @Override
    public <T> T create(ModContainer container, String value, Class<T> type) throws LanguageAdapterException
    {
        try
        {
            Class<?> implementation = Class.forName(implementation(value), true, this.getClass().getClassLoader());
            return type.cast(implementation.getDeclaredConstructor().newInstance());
        }
        catch (ReflectiveOperationException | RuntimeException exception)
        {
            throw new LanguageAdapterException("Cannot initialize addon for selected BBS host", exception);
        }
    }
}
