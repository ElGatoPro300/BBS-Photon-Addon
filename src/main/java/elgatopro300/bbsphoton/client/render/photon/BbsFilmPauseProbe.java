package elgatopro300.bbsphoton.client.render.photon;

import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;

import javax.annotation.Nullable;

/** Reads CML 3.0's public panel API, including dashboards hosted inside a UIScreen. */
public final class BbsFilmPauseProbe
{
    private BbsFilmPauseProbe()
    {}

    @Nullable
    public static Boolean readBBSPaused()
    {
        Object menu = BbsRenderBridge.currentMenu();

        if (menu instanceof UIDashboard dashboard
            && dashboard.getPanels().panel instanceof UIFilmPanel film
            && film.getData() != null)
        {
            return !film.isRunning();
        }

        return null;
    }
}
