package com.chrismod.motionblur.mixin;

import com.chrismod.motionblur.MotionBlurMod;
import com.chrismod.motionblur.config.MotionBlurConfig;
import com.chrismod.motionblur.render.MotionBlurRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onHudRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        MotionBlurConfig config = MotionBlurMod.getConfig();
        MotionBlurRenderer renderer = MotionBlurMod.getRenderer();

        if (config == null || renderer == null || !config.showHud || !config.enabled) return;

        String typeNames = switch (config.blurType) {
            case 1 -> "Accum";
            case 2 -> "Vel";
            case 3 -> "Enh";
            default -> "?";
        };

        String hudText = "MB: " + typeNames + " | " + String.format("%.0f%%", config.intensity * 100)
                + " | S" + config.sampleCount;

        context.drawTextWithShadow(
                net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                hudText,
                2, 2,
                0xFFFFD700
        );
    }
}
