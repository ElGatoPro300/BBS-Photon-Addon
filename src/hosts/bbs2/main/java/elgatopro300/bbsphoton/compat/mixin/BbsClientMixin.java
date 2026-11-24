package elgatopro300.bbsphoton.compat.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import elgatopro300.bbsphoton.client.BBSPhotonClientAddon;

/** Hook only the upstream lifecycle; other hosts use their existing addon events. */
@Mixin(value = BBSModClient.class, remap = false)
public abstract class BbsClientMixin
{
    @Unique
    private BBSPhotonClientAddon bbs2$registration;

    @Inject(method = "onInitializeClient", at = @At("HEAD"))
    private void bbs2$subscribe(CallbackInfo info)
    {
        this.bbs2$registration = new BBSPhotonClientAddon();
        BBSMod.events.register(this.bbs2$registration);
    }

    @Inject(method = "onInitializeClient", at = @At("TAIL"))
    private void bbs2$initialize(CallbackInfo info)
    {
        this.bbs2$registration.initialize();
    }
}
