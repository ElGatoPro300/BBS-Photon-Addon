package ports.smoke;

import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.categories.FormCategory;
import mchorse.bbs_mod.forms.sections.FormSection;

import java.util.Arrays;
import java.util.List;

import elgatopro300.bbsphoton.client.gui.PhotonFormSection;

/** Isolate the title-screen fixture from vanilla item components bound on world load. */
public final class ModernPaletteChecks
{
    public static List<FormCategory> categories(FormCategories categories)
    {
        return categories.getSections().stream().filter(section -> section instanceof PhotonFormSection)
            .flatMap(section -> section.getCategories().stream()).toList();
    }

    public static void setup(FormCategories categories)
    {
        try
        {
            categories.setup();
            return;
        }
        catch (NullPointerException exception)
        {
            boolean extraSection = Arrays.stream(exception.getStackTrace())
                .anyMatch(frame -> frame.getClassName().equals("mchorse.bbs_mod.forms.sections.ExtraFormSection")
                    && frame.getMethodName().equals("initiate"));
            if (!extraSection || !"Components not bound yet".equals(exception.getMessage()))
            {
                throw exception;
            }
        }

        /* setup registered all sections before the built-in Extra section needed a world. */
        boolean found = false;
        for (FormSection section : categories.getSections())
        {
            if (section instanceof PhotonFormSection)
            {
                section.initiate();
                found = true;
            }
        }
        if (!found)
        {
            throw new AssertionError("Modern addon palette section was not registered");
        }
        System.out.println("[Addon ports] Verified registered addon section; complete built-in palette requires world-bound item components");
    }
}
