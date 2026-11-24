package elgatopro300.bbsphoton.mixin.cml;

import mchorse.bbs_mod.api.replays.TrackPresentation;
import mchorse.bbs_mod.api.replays.TrackPresentationRegistry;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeTrackLayout;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import java.util.List;

import elgatopro300.bbsphoton.compat.cml.CmlTrackGroups;

/** Supply form-scoped metadata while CML retains ownership of groups and folding. */
@Mixin(value = UIKeyframeTrackLayout.class, remap = false)
public abstract class CmlTimelineMixin
{
    @WrapOperation(method = "arrange(Ljava/util/List;Ljava/util/Map;Z)Ljava/util/List;",
        at = @At(value = "INVOKE", target = "Lmchorse/bbs_mod/api/replays/TrackPresentationRegistry;get(Ljava/lang/String;)Lmchorse/bbs_mod/api/replays/TrackPresentation;"))
    private static TrackPresentation presentation(TrackPresentationRegistry registry, String id,
        Operation<TrackPresentation> original, @Local UIKeyframeSheet sheet)
    {
        TrackPresentation presentation = CmlTrackGroups.presentation(sheet);
        return presentation == null ? original.call(registry, id) : presentation;
    }

    @Inject(method = "arrange(Ljava/util/List;Ljava/util/Map;Z)Ljava/util/List;", at = @At("RETURN"))
    private static void arrange(CallbackInfoReturnable<List<UIKeyframeElement>> callback)
    {
        CmlTrackGroups.finish(callback.getReturnValue());
    }
}
