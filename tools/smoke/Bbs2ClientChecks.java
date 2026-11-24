package ports.smoke;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;

import java.util.Map;

import elgatopro300.bbsphoton.client.gui.HostEditorAccess;
import elgatopro300.bbsphoton.client.render.photon.HostRenderAccess;
import elgatopro300.bbsphoton.forms.PhotonForm;

/** Exercise host linkage and clocks with independent controllers in a running client. */
public final class Bbs2ClientChecks
{
    public static void check(PhotonForm form)
    {
        Film film = new Film();
        Replay replay = film.replays.addReplay();
        replay.looping.set(10);
        StubEntity entity = new StubEntity();
        entity.setForm(form);
        ClockController controller = new ClockController(film);
        controller.getEntities().put(0, entity);
        BBSModClient.getFilms().getControllers().add(controller);
        try
        {
            FormRenderingContext context = new FormRenderingContext();
            context.entity = entity;
            context.transition = 0.5F;
            context.modelRenderer = true;
            entity.setAge(7);
            require(HostRenderAccess.preview(context) == 7.5F, "preview clock");
            context.modelRenderer = false;
            require(Float.isNaN(HostRenderAccess.preview(context)), "world is not a preview");
            require(HostRenderAccess.animation(context) == 3.5F, "owning replay local loop time");
            controller.paused = true;
            require(HostRenderAccess.animation(context) == 3F, "paused film removes partial tick");
            controller.tick = 4;
            require(HostRenderAccess.animation(context) == 4F, "backward seek clock");
            HostEditorAccess.setNumber(form.speed, 2.5D);
            HostEditorAccess.setNumber(form.seed, 42D);
            require(form.speed.get() == 2.5F && form.seed.get() == 42L, "typed numeric editor binding");
        }
        finally
        {
            BBSModClient.getFilms().getControllers().remove(controller);
        }
    }

    private static void require(boolean condition, String check)
    {
        if (!condition)
        {
            throw new AssertionError(check);
        }
    }

    private static final class ClockController extends BaseFilmController
    {
        private int tick = 23;

        private ClockController(Film film)
        {
            super(film);
        }

        @Override
        public Map<String, Integer> getActors()
        {
            return Map.of();
        }

        @Override
        public int getTick()
        {
            return this.tick;
        }
    }
}
