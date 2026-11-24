package elgatopro300.bbsphoton.compat.mixin;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.editors.KeyframeValueEditors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

import elgatopro300.bbsphoton.client.gui.PhotonKeyframeEditor;

@Mixin(value = KeyframeValueEditors.class, remap = false)
public abstract class KeyframeEditorMixin
{
    @Inject(method = "open", at = @At("HEAD"), cancellable = true)
    private static void bbs2$edit(UIContext context, UIKeyframeSheet sheet, Object value,
        Consumer<Object> callback, CallbackInfo info)
    {
        if (sheet.property != null && "photon_fx".equals(sheet.property.getId()) && value instanceof String effect)
        {
            PhotonKeyframeEditor.open(context, effect, callback);
            info.cancel();
        }
    }
}
