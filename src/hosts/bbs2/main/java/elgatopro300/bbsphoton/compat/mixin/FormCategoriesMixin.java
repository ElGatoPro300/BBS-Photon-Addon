package elgatopro300.bbsphoton.compat.mixin;

import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.sections.FormSection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import elgatopro300.bbsphoton.client.gui.PhotonFormSection;

/** Rebuild addon sections with the host palette, including persisted visibility. */
@Mixin(value = FormCategories.class, remap = false)
public abstract class FormCategoriesMixin
{
    @Shadow
    private List<FormSection> sections;

    @Inject(method = "setup", at = @At("TAIL"))
    private void bbs2$section(CallbackInfo info)
    {
        FormCategories categories = (FormCategories) (Object) this;
        FormSection section = new PhotonFormSection(categories);
        section.initiate();
        this.sections.add(section);
        categories.visibility.read();
        categories.markDirty();
    }
}
