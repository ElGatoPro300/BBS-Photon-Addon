package elgatopro300.bbsphoton.client.gui;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.resources.Link;

import net.fabricmc.loader.api.FabricLoader;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import elgatopro300.bbsphoton.client.render.photon.PhotonFxBridge;

/**
 * Shared effect discovery / folder UX for the form panel and film keyframe factory.
 *
 * Listing is intentionally hybrid:
 * - primary source: Photon {@code FXHelper.listAllFX()} through {@link PhotonFxBridge}, which
 *   includes loose FX, resource packs, mods and mounted {@code .fxpack}s;
 * - fallback / compatibility source: old BBS provider + LDLib2 loose-file scan.
 *
 * The UI does not keep its own long-lived cache. Photon already caches {@code listAllFX()}, and
 * removing the extra UI cache means ids appear after F3+T without restarting the panel.
 */
public final class PhotonFxDiscovery
{
    private PhotonFxDiscovery()
    {
    }

    public static List<String> listEffectIds()
    {
        Set<String> ids = new LinkedHashSet<>();

        ids.addAll(PhotonFxBridge.get().listEffectIds());
        addBbsResourceEffects(ids);
        addLooseFileEffects(ids);

        List<String> sorted = new ArrayList<>(ids);
        Collections.sort(sorted);

        return sorted;
    }

    public static File getLooseFxDir()
    {
        return new File(FabricLoader.getInstance().getGameDir().toFile(), "ldlib2/assets/photon/fx");
    }

    public static File getFxPacksDir()
    {
        return PhotonFxBridge.get().getFxPacksDir();
    }

    public static void openFolder(File folder)
    {
        try
        {
            if (!folder.exists())
            {
                folder.mkdirs();
            }

            if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win"))
            {
                Runtime.getRuntime().exec("explorer.exe \"" + folder.getAbsolutePath() + "\"");
            }
            else
            {
                Desktop.getDesktop().open(folder);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void addBbsResourceEffects(Set<String> ids)
    {
        try
        {
            Collection<String> sources = BBSMod.getProvider().getSourceKeys();

            for (String source : sources)
            {
                addBbsResourceEffects(ids, source);
            }
        }
        catch (Exception e)
        {
            /* Resource provider may be unavailable while the client is starting. */
        }
    }

    private static void addBbsResourceEffects(Set<String> ids, String source)
    {
        try
        {
            Collection<Link> links = BBSMod.getProvider().getLinksFromPath(new Link(source, "photon"), true);

            for (Link link : links)
            {
                if (!link.path.endsWith(".fx"))
                {
                    continue;
                }

                String path = link.path;

                if (path.startsWith("photon/"))
                {
                    path = path.substring(7);
                }

                if (path.endsWith(".fx"))
                {
                    path = path.substring(0, path.length() - 3);
                }

                if (path.startsWith("fx/"))
                {
                    path = path.substring(3);
                }

                ids.add(source + ":" + path);
            }
        }
        catch (Exception e)
        {
            /* Ignore errors for individual sources. */
        }
    }

    private static void addLooseFileEffects(Set<String> ids)
    {
        File photonFxDir = getLooseFxDir();

        if (!photonFxDir.exists() || !photonFxDir.isDirectory())
        {
            return;
        }

        Path root = photonFxDir.toPath();

        try (Stream<Path> files = Files.walk(root))
        {
            files.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".fx"))
                .forEach(path -> addLooseFileEffect(ids, root, path));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static void addLooseFileEffect(Set<String> ids, Path root, Path path)
    {
        String relative = root.relativize(path).toString().replace(File.separatorChar, '/');

        if (relative.endsWith(".fx"))
        {
            relative = relative.substring(0, relative.length() - 3);
        }

        ids.add("photon:" + relative);
    }
}
