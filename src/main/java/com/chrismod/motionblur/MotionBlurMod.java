package com.chrismod.motionblur;

import com.chrismod.motionblur.config.MotionBlurConfig;
import com.chrismod.motionblur.gui.MotionBlurScreen;
import com.chrismod.motionblur.render.MotionBlurRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MotionBlurMod implements ClientModInitializer {

    public static final String MOD_ID = "motionblur";
    public static final String MOD_VERSION = "1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static MotionBlurConfig config;
    private static MotionBlurRenderer renderer;
    public static KeyBinding toggleKeybind;
    public static KeyBinding guiKeybind;

    @Override
    public void onInitializeClient() {
        config = MotionBlurConfig.load();
        applyRefreshRateDefaults(config);
        renderer = new MotionBlurRenderer(config);

        KeyBinding.Category category = KeyBinding.Category.create(
            Identifier.of(MOD_ID, "keys")
        );

        toggleKeybind = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "motionblur.keybind.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
            )
        );

        guiKeybind = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "motionblur.keybind.gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                category
            )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKeybind.wasPressed()) {
                config.enabled = !config.enabled;
                MotionBlurConfig.save(config);
            }
            while (guiKeybind.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new MotionBlurScreen(null, config));
                }
            }
        });

        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register(
            (client, world) -> {
                if (renderer != null) renderer.onWorldUnload();
            }
        );

        ResourceManagerHelper.get(
            ResourceType.CLIENT_RESOURCES
        ).registerReloadListener(
            new SimpleSynchronousResourceReloadListener() {
                @Override
                public Identifier getFabricId() {
                    return Identifier.of(MOD_ID, "pipeline_invalidate");
                }

                @Override
                public void reload(ResourceManager manager) {
                    if (renderer != null) renderer.onWorldUnload();
                }
            }
        );

        LOGGER.info("MotionBlur initialized");
    }

    private static void applyRefreshRateDefaults(MotionBlurConfig cfg) {
        if (!cfg.refreshRateOptimization) return;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (
                client != null &&
                client.getWindow() != null &&
                client.getWindow().getMonitor() != null
            ) {
                int refreshRate = client
                    .getWindow()
                    .getMonitor()
                    .getCurrentVideoMode()
                    .getRefreshRate();
                if (refreshRate >= 120) {
                    cfg.sampleCount = Math.max(cfg.sampleCount, 9);
                    cfg.intensity = Math.min(cfg.intensity, 0.55f);
                } else {
                    cfg.sampleCount = Math.min(cfg.sampleCount, 7);
                }
            }
        } catch (Exception e) {
            LOGGER.warn(
                "Could not detect monitor refresh rate, using config defaults"
            );
        }
    }

    public static MotionBlurConfig getConfig() {
        return config;
    }

    public static MotionBlurRenderer getRenderer() {
        return renderer;
    }
}
