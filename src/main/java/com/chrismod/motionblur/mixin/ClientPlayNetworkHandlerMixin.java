package com.chrismod.motionblur.mixin;

import com.chrismod.motionblur.MotionBlurMod;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onPlayerPositionLook", at = @At("HEAD"))
    private void onTeleport(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        if (MotionBlurMod.getRenderer() != null) {
            MotionBlurMod.getRenderer().onTeleport();
        }
    }
}
