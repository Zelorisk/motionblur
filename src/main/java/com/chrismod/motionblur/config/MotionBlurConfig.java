package com.chrismod.motionblur.config;

import com.chrismod.motionblur.MotionBlurMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public class MotionBlurConfig {

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("motionblur.json");

    public boolean enabled = true;
    public int blurType = 1;
    public float intensity = 0.65f;
    public int sampleCount = 7;
    public boolean refreshRateOptimization = true;
    public boolean cameraOnlyMode = false;
    public boolean glossyMode = false;
    public boolean crtMode = false;
    public boolean showHud = false;

    public static MotionBlurConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            MotionBlurConfig defaults = new MotionBlurConfig();
            save(defaults);
            return defaults;
        }
        try {
            String json = Files.readString(CONFIG_PATH);
            return GSON.fromJson(json, MotionBlurConfig.class);
        } catch (IOException e) {
            MotionBlurMod.LOGGER.error(
                "Failed to load config, using defaults",
                e
            );
            return new MotionBlurConfig();
        }
    }

    public static void save(MotionBlurConfig config) {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(config));
        } catch (IOException e) {
            MotionBlurMod.LOGGER.error("Failed to save config", e);
        }
    }

    public void applyPreset(Preset preset) {
        switch (preset) {
            case PERFORMANCE -> {
                blurType = 1;
                intensity = 0.35f;
                sampleCount = 3;
            }
            case BALANCED -> {
                blurType = 1;
                intensity = 0.55f;
                sampleCount = 7;
            }
            case QUALITY -> {
                blurType = 1;
                intensity = 0.7f;
                sampleCount = 11;
            }
            case ULTRA -> {
                blurType = 1;
                intensity = 0.85f;
                sampleCount = 15;
            }
        }
    }

    public enum Preset {
        PERFORMANCE,
        BALANCED,
        QUALITY,
        ULTRA,
    }
}
