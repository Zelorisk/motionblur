package com.chrismod.motionblur.mixin;

import com.chrismod.motionblur.MotionBlurMod;
import com.chrismod.motionblur.render.MotionBlurRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderStart(
        RenderTickCounter tickCounter,
        boolean tick,
        CallbackInfo ci
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        MotionBlurRenderer renderer = MotionBlurMod.getRenderer();
        if (renderer != null) {
            renderer.onFrameStart(client);
        }
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/InGameHud;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"
        )
    )
    private void onPreHud(
        RenderTickCounter tickCounter,
        boolean tick,
        CallbackInfo ci
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        MotionBlurRenderer renderer = MotionBlurMod.getRenderer();
        if (renderer != null) {
            renderer.renderMotionBlur(client);
        }
    }
}
