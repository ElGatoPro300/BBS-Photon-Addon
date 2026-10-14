package elgatopro300.bbsphoton.client.gui;

import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.categories.FormCategory;
import mchorse.bbs_mod.forms.sections.FormSection;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;

import java.util.List;

import elgatopro300.bbsphoton.forms.PhotonForm;

/** Rebuilds the Photon palette entry whenever BBS rebuilds its form sections. */
public final class PhotonFormSection extends FormSection
{
    private List<FormCategory> categories = List.of();

    public PhotonFormSection(FormCategories parent)
    {
        super(parent);
    }

    @Override
    public IKey getTitle()
    {
        return L10n.lang("bbs_photon.ui.panel_title");
    }

    @Override
    public void initiate()
    {
        FormCategory category = new FormCategory(this.getTitle(), this.parent.visibility.get("bbs_photon"));
        category.addForm(new PhotonForm());
        this.categories = List.of(category);
    }

    @Override
    public List<FormCategory> getCategories()
    {
        return this.categories;
    }
}
