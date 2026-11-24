package elgatopro300.bbsphoton.compat.cml;

import mchorse.bbs_mod.api.replays.TrackPresentation;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeGroup;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;

import java.util.Comparator;
import java.util.List;

import elgatopro300.bbsphoton.forms.PhotonForm;

/** Presentation metadata only: stored paths, channels and host layout remain unchanged. */
public final class CmlTrackGroups
{
    private record Entry(String id, String label, String group, String groupLabel, int order) {}

    private static final List<Entry> ENTRIES = List.of(
        new Entry("photon_fx", "bbs.keyframe.photon_fx", "effect", "bbs_photon.ui.panel_title", 0),
        new Entry("paused", "bbs_photon.ui.paused", "playback", "bbs_photon.runtime.playback", 1),
        new Entry("photon_film_sync", "bbs_photon.runtime.film_sync", "playback", "bbs_photon.runtime.playback", 2),
        new Entry("photon_speed_override", "bbs_photon.runtime.speed_override", "playback", "bbs_photon.runtime.playback", 3),
        new Entry("photon_speed", "bbs_photon.runtime.speed", "playback", "bbs_photon.runtime.playback", 4),
        new Entry("photon_start_offset", "bbs_photon.runtime.offset", "playback", "bbs_photon.runtime.playback", 5),
        new Entry("photon_fixed_seed", "bbs_photon.runtime.fixed_seed", "playback", "bbs_photon.runtime.playback", 6),
        new Entry("photon_seed", "bbs_photon.runtime.seed", "playback", "bbs_photon.runtime.playback", 7),
        new Entry("photon_signals", "bbs_photon.runtime.signals", "playback", "bbs_photon.runtime.playback", 8),
        new Entry("photon_post_enabled", "bbs_photon.runtime.enabled", "post", "bbs_photon.runtime.post", 9),
        new Entry("photon_post_effect", "bbs_photon.runtime.effect", "post", "bbs_photon.runtime.post", 10),
        new Entry("photon_post_weight", "bbs_photon.runtime.weight", "post", "bbs_photon.runtime.post", 11),
        new Entry("photon_parameters", "bbs_photon.runtime.value", "parameters", "bbs_photon.runtime.parameters", 12),
        new Entry("photon_anchors", "bbs_photon.runtime.value", "anchors", "bbs_photon.runtime.anchors", 13),
        new Entry("photon_post_parameters", "bbs_photon.runtime.value", "post", "bbs_photon.runtime.post", 14));

    public static TrackPresentation presentation(UIKeyframeSheet sheet)
    {
        if (sheet.property == null || !(FormUtils.getForm(sheet.property) instanceof PhotonForm))
        {
            return null;
        }
        String[] parts = sheet.id.replaceFirst("^(?:[0-9]+/)+", "").split("/");
        Entry entry = ENTRIES.stream().filter(candidate -> candidate.id().equals(parts[0])).findFirst().orElse(null);
        if (entry == null)
        {
            return null;
        }
        String label = entry.label();

        if (parts.length > 1)
        {
            String field = parts[parts.length - 1];
            label = switch (field)
            {
                case "enabled", "property", "attachment", "x", "y", "z", "w" -> "bbs_photon.runtime." + field;
                case "target" -> "bbs_photon.runtime.object";
                case "integer" -> "bbs.ui.forms.editors.modifiers.variables.type_int";
                case "color" -> "bbs.ui.forms.editors.modifiers.variables.type_color";
                case "flag" -> "bbs.ui.forms.editors.modifiers.variables.type_bool";
                case "kind" -> "bbs.ui.add.type";
                case "offset" -> "bbs.ui.camera.panels.offset";
                default -> "bbs_photon.runtime.value";
            };
        }

        int color = switch (entry.group())
        {
            case "playback" -> 0x66BB6A;
            case "parameters" -> 0x42A5F5;
            case "anchors" -> 0xFFB74D;
            case "post" -> 0xAB47BC;
            default -> 0xFF00B2;
        };
        return new TrackPresentation(sheet.id, label, color,
            "bbs_photon:" + entry.group(), entry.groupLabel(), "", entry.order());
    }

    public static void finish(List<UIKeyframeElement> elements)
    {
        for (UIKeyframeElement element : elements)
        {
            if (element instanceof UIKeyframeGroup group)
            {
                finish(group.children);
                if (!group.children.isEmpty() && group.children.stream().allMatch(child ->
                    child instanceof UIKeyframeSheet sheet && presentation(sheet) != null))
                {
                    /* Stable ordering retains the host's order within repeated list entries. */
                    group.children.sort(Comparator.comparingInt(child -> presentation((UIKeyframeSheet) child).order()));
                }
            }
            else if (element instanceof UIKeyframeSheet sheet && presentation(sheet) != null)
            {
                String[] parts = sheet.id.replaceFirst("^(?:[0-9]+/)+", "").split("/");
                if (parts.length > 2)
                {
                    TrackPresentation metadata = presentation(sheet);
                    sheet.title = () -> parts[1] + " \u00b7 " + L10n.lang(metadata.labelKey()).get();
                }
            }
        }
    }
}
