package com.chrismod.motionblur.render;

import net.minecraft.client.MinecraftClient;
import org.joml.Matrix4f;
import org.joml.Vector2f;

public class VelocityCalculator {

    private float prevYaw;
    private float prevPitch;

    private float deltaYaw;
    private float deltaPitch;

    private float smoothYaw;
    private float smoothPitch;

    private boolean firstFrame = true;
    private boolean teleportFlag = false;
    private int teleportCooldown = 0;

    private long prevFrameTime = System.nanoTime();
    private float frameDelta = 1.0f;

    private static final float VELOCITY_WINDOW = 0.04f;

    public void update(MinecraftClient client) {
        long now = System.nanoTime();
        float rawDelta = (now - prevFrameTime) / 1_000_000_000.0f;
        prevFrameTime = now;
        frameDelta = Math.max(0.001f, Math.min(rawDelta, 0.1f));

        if (teleportCooldown > 0) {
            teleportCooldown--;
            deltaYaw = 0;
            deltaPitch = 0;
            smoothYaw = 0;
            smoothPitch = 0;
            return;
        }

        if (client.player == null || client.getCameraEntity() == null) {
            deltaYaw = 0;
            deltaPitch = 0;
            firstFrame = true;
            return;
        }

        float currentYaw = client.getCameraEntity().getYaw();
        float currentPitch = client.getCameraEntity().getPitch();

        if (firstFrame) {
            prevYaw = currentYaw;
            prevPitch = currentPitch;
            firstFrame = false;
            deltaYaw = 0;
            deltaPitch = 0;
            return;
        }

        float rawDeltaYaw = currentYaw - prevYaw;
        float rawDeltaPitch = currentPitch - prevPitch;

        if (rawDeltaYaw > 180) rawDeltaYaw -= 360;
        if (rawDeltaYaw < -180) rawDeltaYaw += 360;

        deltaYaw = rawDeltaYaw;
        deltaPitch = rawDeltaPitch;

        float decay = (float) Math.exp(-frameDelta / VELOCITY_WINDOW);
        smoothYaw = smoothYaw * decay + rawDeltaYaw;
        smoothPitch = smoothPitch * decay + rawDeltaPitch;

        prevYaw = currentYaw;
        prevPitch = currentPitch;
    }

    public void onTeleport() {
        teleportFlag = true;
        teleportCooldown = 2;
        deltaYaw = 0;
        deltaPitch = 0;
        smoothYaw = 0;
        smoothPitch = 0;
    }

    public void reset() {
        firstFrame = true;
        deltaYaw = 0;
        deltaPitch = 0;
        smoothYaw = 0;
        smoothPitch = 0;
    }

    public float getDeltaYaw() {
        return deltaYaw;
    }

    public float getDeltaPitch() {
        return deltaPitch;
    }

    public float getFrameDelta() {
        return frameDelta;
    }

    public boolean hasMeaningfulVelocity(float threshold) {
        return (
            Math.abs(smoothYaw) > threshold || Math.abs(smoothPitch) > threshold
        );
    }

    public Vector2f getScreenSpaceVelocity(
        int screenWidth,
        int screenHeight,
        float fov
    ) {
        float aspectRatio = (float) screenWidth / screenHeight;
        float fovRad = (float) Math.toRadians(fov);
        float fovScale = (float) (1.0f / Math.tan(fovRad * 0.5f));

        float vx = (-smoothYaw / fov) * fovScale * 0.5f;
        float vy = (smoothPitch / (fov / aspectRatio)) * fovScale * 0.5f;

        return new Vector2f(vx, vy);
    }
}
