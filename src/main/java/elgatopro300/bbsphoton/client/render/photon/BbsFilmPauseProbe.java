package elgatopro300.bbsphoton.client.render.photon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Isolates the reflection used to detect the BBS film / dashboard pause state. Extracted from
 * {@code PhotonFormRenderer} during Phase C so the renderer stays a coordinator and the brittle
 * BBS-internal access lives in one named place.
 *
 * This probe is intentionally stateless. Callers keep their own last-known-paused cache (e.g.
 * because the dashboard might be hidden for a frame) and treat {@code null} returns as "no
 * reading available — reuse the previous value if it is still fresh".
 */
public final class BbsFilmPauseProbe
{
    private BbsFilmPauseProbe()
    {
    }

    /**
     * Try to read the current BBS pause state by walking the dashboard's panels looking for a
     * {@code UIFilmPanel} whose {@code runner} is not running. Returns {@code null} when the
     * state cannot be determined (no dashboard open, panels not reachable, reflection failed).
     */
    @Nullable
    public static Boolean readBBSPaused()
    {
        try
        {
            Screen screen = Minecraft.getInstance().screen;

            if (screen == null || !screen.getClass().getSimpleName().contains("UIDashboard"))
            {
                return null;
            }

            Field panelsField = findDeclaredField(screen.getClass(), "panels");

            if (panelsField == null)
            {
                return null;
            }

            panelsField.setAccessible(true);
            Object panelsObj = panelsField.get(screen);

            if (!(panelsObj instanceof List<?> panels))
            {
                return null;
            }

            for (Object panel : panels)
            {
                if (panel == null)
                {
                    continue;
                }

                if (!panel.getClass().getSimpleName().contains("UIFilmPanel"))
                {
                    continue;
                }

                Field runnerField = panel.getClass().getDeclaredField("runner");
                runnerField.setAccessible(true);
                Object runner = runnerField.get(panel);

                if (runner == null)
                {
                    continue;
                }

                Method isRunning = runner.getClass().getMethod("isRunning");
                boolean running = (boolean) isRunning.invoke(runner);

                return !running;
            }
        }
        catch (Exception e)
        {
            /* Fall through — caller treats null as "no fresh reading". */
        }

        return null;
    }

    @Nullable
    private static Field findDeclaredField(Class<?> clazz, String name)
    {
        Class<?> current = clazz;

        while (current != null)
        {
            try
            {
                return current.getDeclaredField(name);
            }
            catch (NoSuchFieldException e)
            {
                current = current.getSuperclass();
            }
        }

        return null;
    }
}
