package ports.smoke;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.forms.forms.BillboardForm;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeTrackLayout;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.HashMap;
import java.util.List;

import elgatopro300.bbsphoton.forms.PhotonBinding;
import elgatopro300.bbsphoton.forms.PhotonForm;

/** Exercise the published host layout and installed distribution's mixin in a real client. */
public final class CmlTrackGroupsChecks
{
    public static void check(PhotonForm form)
    {
        UIKeyframeSheet later = sheet("photon_speed", form.speed);
        UIKeyframeSheet paused = sheet("paused", form.paused);
        KeyframeChannel channel = paused.channel;
        List<UIKeyframeElement> elements = UIKeyframeTrackLayout.arrange(List.of(later, paused), new HashMap<>(), false);
        UIKeyframeTrackLayout.TrackGroup group = (UIKeyframeTrackLayout.TrackGroup) elements.get(0);
        require(elements.size() == 1 && group.id.equals(":bbs_photon:playback"), "form-scoped category");
        require(group.children.get(0) == paused, "explicit track ordering");
        require(paused.channel == channel && paused.id.equals("paused"), "existing channel identity");
        group.collapsed = true;
        HashMap<String, Boolean> collapse = new HashMap<>();
        UIKeyframeTrackLayout.remember(elements, collapse);
        require(((UIKeyframeTrackLayout.TrackGroup) UIKeyframeTrackLayout.arrange(List.of(paused), collapse, false).get(0)).collapsed,
            "folding survives layout rebuild");
        for (String language : List.of("es_es", "en_us"))
        {
            BBSModClient.reloadLanguage(language);
            require(group.title.get().equals(L10n.lang("bbs_photon.runtime.playback").get()), "localized category switches with language");
        }
        BillboardForm owner = new BillboardForm();
        BodyPart part = new BodyPart("0");
        part.setForm(form);
        owner.parts.addBodyPart(part);
        UIKeyframeSheet nested = sheet("0/paused", form.paused);
        UIKeyframeTrackLayout.TrackGroup body = (UIKeyframeTrackLayout.TrackGroup)
            UIKeyframeTrackLayout.arrange(List.of(nested), new HashMap<>(), false).get(0);
        require(body.id.equals("body:0"), "bodypart hierarchy retained");
        require(((UIKeyframeTrackLayout.TrackGroup) body.children.get(0)).id.equals("0:bbs_photon:playback"), "category inside owning bodypart");
        UIKeyframeSheet unrelated = sheet("paused", owner.visible);
        UIKeyframeTrackLayout.TrackGroup generic = (UIKeyframeTrackLayout.TrackGroup)
            UIKeyframeTrackLayout.arrange(List.of(unrelated), new HashMap<>(), false).get(0);
        require(!generic.id.contains("bbs_photon:playback"), "generic paused track is not captured by addon");

        PhotonBinding binding = form.parameters.append();
        UIKeyframeSheet parameter = sheet("photon_parameters/0/x", binding.x);
        List<UIKeyframeElement> parameterGroups = UIKeyframeTrackLayout.arrange(List.of(parameter), new HashMap<>(), false);
        require(((UIKeyframeTrackLayout.TrackGroup) parameterGroups.get(0)).id.equals(":bbs_photon:parameters"), "dynamic parameter category");
        require(parameter.title.get().contains("0"), "binding index in label");
        require(parameter.channel.getId().equals("photon_parameters/0/x"), "dynamic stored path preserved");

        UIKeyframeSheet effect = sheet("photon_fx", form.photon_fx);
        UIKeyframeSheet post = sheet("photon_post_weight", form.postWeight);
        UIKeyframeSheet anchor = sheet("photon_anchors/0/enabled", form.anchors.append().enabled);
        UIKeyframeSheet postParameter = sheet("photon_post_parameters/0/x", form.postParameters.append().x);
        List<UIKeyframeElement> categories = UIKeyframeTrackLayout.arrange(
            List.of(effect, paused, parameter, anchor, post, postParameter), new HashMap<>(), false);
        require(categories.size() == 5, "all five Photon categories, including repeated post parameters");
        require(categories.stream().allMatch(element -> element instanceof UIKeyframeTrackLayout.TrackGroup track
            && !track.id.contains("custom")), "addon properties do not fall into Custom");
        System.out.println("[Addon ports] CML TIMELINE CATEGORIES PASSED");
    }

    private static UIKeyframeSheet sheet(String id, BaseValueBasic value)
    {
        return new UIKeyframeSheet(0, false, new KeyframeChannel<>(id, KeyframeFactories.FLOAT), value);
    }

    private static void require(boolean condition, String message)
    {
        if (!condition)
        {
            throw new AssertionError(message);
        }
    }
}
