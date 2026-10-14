package elgatopro300.bbsphoton.client.gui;

import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.base.BaseValueNumber;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.lowdragmc.photon.client.postfx.PhotonPostFX;
import com.lowdragmc.photon.client.postfx.runtime.CompiledEffect;
import com.lowdragmc.photon.client.postfx.runtime.PostEffectStack;
import elgatopro300.bbsphoton.client.render.PhotonFormRenderer;
import elgatopro300.bbsphoton.client.render.photon.PhotonObjectCatalog;
import elgatopro300.bbsphoton.forms.PhotonAnchor;
import elgatopro300.bbsphoton.forms.PhotonBinding;
import elgatopro300.bbsphoton.forms.PhotonForm;
import elgatopro300.bbsphoton.forms.PhotonValueList;

/** Panels edit Values only. Runtime discovery exposes metadata, never playback ownership. */
public class UIPhotonRuntimePanel extends UIFormPanel<PhotonForm>
{
    private final String section;

    public UIPhotonRuntimePanel(UIForm editor, String section)
    {
        super(editor);
        this.section = section;
    }

    private static IKey key(String key)
    {
        return L10n.lang("bbs_photon.runtime." + key);
    }

    @Override
    public void startEdit(PhotonForm form)
    {
        super.startEdit(form);
        this.rebuild();
    }

    private void rebuild()
    {
        this.options.removeAll();
        switch (this.section)
        {
            case "playback" -> this.playback();
            case "anchors" -> this.anchors();
            case "post" -> this.post();
            default -> this.bindings(this.form.parameters, false);
        }
        this.resize();
    }

    private void button(String label, Runnable action)
    {
        this.options.add(new UIButton(key(label), button -> action.run()));
    }

    private void field(String label, UIElement control)
    {
        this.options.add(UI.label(key(label)), control);
    }

    private void toggle(String label, ValueBoolean value)
    {
        UIToggle control = new UIToggle(key(label), value.get(), button -> value.set(button.getValue()));
        if ("film_sync".equals(label))
        {
            control.tooltip(key("film_sync_help"));
        }
        this.options.add(control);
    }

    private void number(String label, BaseValueNumber<?> value)
    {
        UITrackpad control = new UITrackpad(value::setNumber);
        control.setValue(value.get().doubleValue());
        control.limit(value.getMin().doubleValue(), value.getMax().doubleValue());
        this.field(label, control);
    }

    private void text(String label, ValueString value)
    {
        UITextbox control = new UITextbox(512, value::set);
        control.setText(value.get());
        this.field(label, control);
    }

    private void choose(String title, List<String> choices, Consumer<String> callback)
    {
        UIListOverlayPanel panel = new UIListOverlayPanel(key(title), callback);
        panel.addValues(choices);
        UIOverlay.addOverlay(this.getContext(), panel, 0.65F, 0.75F);
    }

    private void playback()
    {
        this.toggle("film_sync", this.form.filmSync);
        this.toggle("speed_override", this.form.speedOverride);
        this.number("speed", this.form.speed);
        this.number("offset", this.form.startOffset);
        this.toggle("fixed_seed", this.form.fixedSeed);
        UITextbox seed = new UITextbox(32, value ->
        {
            try { this.form.seed.set(Long.parseLong(value)); }
            catch (NumberFormatException e) { /* Keep the last valid seed while editing. */ }
        });
        seed.setText(this.form.seed.get().toString());
        this.field("seed", seed);
        this.toggle("signals", this.form.signals);
        this.button("diagnostics", () ->
        {
            PhotonFormRenderer renderer = (PhotonFormRenderer) FormUtilsClient.getRenderer(this.form);
            List<String> rows = renderer.diagnostics();
            IKey message = rows.isEmpty() ? key("no_diagnostics") : IKey.constant(String.join("\n", rows));
            UIOverlay.addOverlay(this.getContext(), new UIMessageOverlayPanel(key("diagnostics"), message), 0.8F, 0.8F);
        });
    }

    private void pickObject(ValueString target, Runnable changed)
    {
        List<PhotonObjectCatalog.ObjectInfo> objects = PhotonObjectCatalog.inspect(this.form.photon_fx.get());
        List<String> labels = objects.stream().map(object -> object.name() + " | " + object.id()).toList();
        this.choose("object", labels, label ->
        {
            int index = labels.indexOf(label);
            if (index >= 0)
            {
                target.set(objects.get(index).id());
                changed.run();
                this.rebuild();
            }
        });
    }

    private void bindings(PhotonValueList<PhotonBinding> entries, boolean post)
    {
        this.button("add", () -> { entries.append(); this.rebuild(); });
        for (PhotonBinding entry : entries.getList())
        {
            this.toggle("enabled", entry.enabled);
            if (!post)
            {
                this.text("object", entry.target);
                this.button("pick_object", () -> this.pickObject(entry.target, () -> entry.property.set("")));
            }
            this.text("property", entry.property);
            this.button("pick_property", () -> this.pickProperty(entry, post));
            this.values(entry);
            this.button("remove", () -> { entries.delete(entry); this.rebuild(); });
        }
    }

    private void pickProperty(PhotonBinding entry, boolean post)
    {
        List<PhotonObjectCatalog.PropertyInfo> properties = new ArrayList<>();
        if (post)
        {
            CompiledEffect effect = PostEffectStack.resolveEffect(PhotonPostFX.parsePath(this.form.postEffect.get()));
            if (effect != null)
            {
                for (CompiledEffect.ParamSpec spec : effect.schema())
                {
                    Object value = spec.defaultValue();
                    String kind = value instanceof Vector4f ? "VECTOR4" : value instanceof Vector3f ? "VECTOR3"
                        : value instanceof Vector2f ? "VECTOR2" : value instanceof Boolean ? "BOOL"
                        : value instanceof Integer ? "INT" : value instanceof Number ? "FLOAT" : "";
                    if (!kind.isEmpty())
                    {
                        properties.add(new PhotonObjectCatalog.PropertyInfo(spec.name(), kind));
                    }
                }
            }
        }
        else
        {
            for (PhotonObjectCatalog.ObjectInfo object : PhotonObjectCatalog.inspect(this.form.photon_fx.get()))
            {
                if (object.id().equals(entry.target.get()))
                {
                    properties.addAll(object.properties());
                }
            }
        }
        this.choose("property", properties.stream().map(PhotonObjectCatalog.PropertyInfo::path).toList(), path ->
        {
            for (PhotonObjectCatalog.PropertyInfo property : properties)
            {
                if (property.path().equals(path))
                {
                    entry.property.set(path);
                    entry.kind.set(property.kind());
                }
            }
            this.rebuild();
        });
    }

    private void values(PhotonBinding entry)
    {
        switch (entry.kind.get())
        {
            case "BOOL" -> this.toggle("value", entry.flag);
            case "INT" -> this.number("value", entry.integer);
            case "COLOR" ->
            {
                UIColor color = new UIColor(entry.color::set).withAlpha();
                color.setColor(entry.color.get());
                this.field("value", color);
            }
            default ->
            {
                this.number("x", entry.x);
                if (List.of("NUMBER_FUNCTION3", "VECTOR2", "VECTOR3", "VECTOR4").contains(entry.kind.get()))
                {
                    this.number("y", entry.y);
                }
                if (List.of("NUMBER_FUNCTION3", "VECTOR3", "VECTOR4").contains(entry.kind.get()))
                {
                    this.number("z", entry.z);
                }
                if ("VECTOR4".equals(entry.kind.get()))
                {
                    this.number("w", entry.w);
                }
            }
        }
    }

    private void anchors()
    {
        this.button("add", () -> { this.form.anchors.append(); this.rebuild(); });
        for (PhotonAnchor entry : this.form.anchors.getList())
        {
            this.toggle("enabled", entry.enabled);
            this.text("object", entry.target);
            this.button("pick_object", () -> this.pickObject(entry.target, () -> {}));
            this.text("attachment", entry.attachment);
            this.button("pick_attachment", () ->
            {
                List<String> names = new ArrayList<>(FormUtilsClient.getRenderer(FormUtils.getRoot(this.form))
                    .collectMatrices(this.editor.editor.renderer.getTargetEntity(), 0F).keySet());
                names.sort(String::compareToIgnoreCase);
                this.choose("attachment", names, name -> { entry.attachment.set(name); this.rebuild(); });
            });
            UIPropTransform transform = new UIPropTransform().callbacks(() -> entry.offset);
            transform.setTransform(entry.offset.get());
            this.options.add(transform);
            this.button("remove", () -> { this.form.anchors.delete(entry); this.rebuild(); });
        }
    }

    private void post()
    {
        this.toggle("enabled", this.form.postEnabled);
        this.text("effect", this.form.postEffect);
        this.button("pick_effect", () -> this.choose("effect", PhotonPostFX.listEffectPaths().stream()
            .map(path -> path.replace(" (fullscreen)", "")).toList(), path ->
            { this.form.postEffect.set(path); this.rebuild(); }));
        this.number("weight", this.form.postWeight);
        this.bindings(this.form.postParameters, true);
    }
}
