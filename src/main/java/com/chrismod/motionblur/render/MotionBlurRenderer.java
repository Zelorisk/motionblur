package com.chrismod.motionblur.render;

import com.chrismod.motionblur.MotionBlurMod;
import com.chrismod.motionblur.config.MotionBlurConfig;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.joml.Vector2f;

public class MotionBlurRenderer {

    private static final Identifier VERT = Identifier.of(
        MotionBlurMod.MOD_ID,
        "mb_accum"
    );
    private static final Identifier ACCUM_FRAG = Identifier.of(
        MotionBlurMod.MOD_ID,
        "mb_accum"
    );
    private static final Identifier VEL_FRAG = Identifier.of(
        MotionBlurMod.MOD_ID,
        "mb_velocity"
    );
    private static final Identifier ENH_FRAG = Identifier.of(
        MotionBlurMod.MOD_ID,
        "mb_enhanced"
    );
    private static final Identifier COPY_FRAG = Identifier.of(
        MotionBlurMod.MOD_ID,
        "mb_copy"
    );
    private static final Identifier GLOSSY_FRAG = Identifier.of(
        MotionBlurMod.MOD_ID,
        "mb_glossy"
    );
    private static final Identifier CRT_FRAG = Identifier.of(
        MotionBlurMod.MOD_ID,
        "mb_crt"
    );

    private final MotionBlurConfig config;
    private final VelocityCalculator velocityCalculator;
    private final FrameAccumulator frameAccumulator;

    private RenderPipeline accumPipeline;
    private RenderPipeline velocityPipeline;
    private RenderPipeline enhancedPipeline;
    private RenderPipeline copyPipeline;
    private RenderPipeline glossyPipeline;
    private RenderPipeline crtPipeline;

    private boolean inGui;
    private long lastDebugLog = 0;
    private float blendAlpha = 0.0f;
    private float crtTime = 0.0f;
    private long lastCrtNanos = System.nanoTime();

    private static final float VELOCITY_THRESHOLD = 0.0001f;
    private static final float FADE_SPEED = 8.0f;

    public MotionBlurRenderer(MotionBlurConfig config) {
        this.config = config;
        this.velocityCalculator = new VelocityCalculator();
        this.frameAccumulator = new FrameAccumulator();
    }

    public void onFrameStart(MinecraftClient client) {
        inGui = client.currentScreen != null;
        velocityCalculator.update(client);
        long now = System.nanoTime();
        crtTime += (now - lastCrtNanos) * 1e-9f;
        lastCrtNanos = now;
    }

    public void renderMotionBlur(MinecraftClient client) {
        float fd = velocityCalculator.getFrameDelta();
        boolean active = shouldRender(client);
        blendAlpha +=
            ((active ? 1.0f : 0.0f) - blendAlpha) *
            Math.min(1.0f, FADE_SPEED * fd);

        Framebuffer fb = client.getFramebuffer();
        int w = fb.textureWidth;
        int h = fb.textureHeight;

        GpuTextureView mainView = fb.getColorAttachmentView();
        if (mainView == null) return;

        boolean glossyActive = config.glossyMode && client.world != null && client.player != null;
        boolean crtActive = config.crtMode && client.world != null && client.player != null;
        boolean needsAccumulator = blendAlpha >= 0.005f || glossyActive || crtActive;
        if (!needsAccumulator) {
            if (!active) {
                long now = System.currentTimeMillis();
                if (now - lastDebugLog > 10000) {
                    lastDebugLog = now;
                    MotionBlurMod.LOGGER.debug(
                        "shouldRender=false enabled={} world={} player={} inGui={} hasVelocity={} blurType={}",
                        config.enabled,
                        client.world != null,
                        client.player != null,
                        inGui,
                        velocityCalculator.hasMeaningfulVelocity(
                            VELOCITY_THRESHOLD
                        ),
                        config.blurType
                    );
                }
            }
            return;
        }

        frameAccumulator.ensureSize(w, h);
        if (!frameAccumulator.isReady()) {
            MotionBlurMod.LOGGER.warn("frameAccumulator not ready");
            return;
        }

        ensurePipelines();

        if (blendAlpha >= 0.005f) {
            switch (config.blurType) {
                case 1 -> renderAccum(fb, mainView);
                case 2 -> renderVelocity(client, fb, mainView, w, h, false);
                case 3 -> renderVelocity(client, fb, mainView, w, h, true);
            }
        }

        if (glossyActive) {
            renderGlossy(fb);
        }

        if (crtActive) {
            renderCrt(fb);
        }
    }

    private void renderCrt(Framebuffer fb) {
        if (crtPipeline == null) return;

        GpuTextureView srcView = fb.getColorAttachmentView();
        if (srcView == null) return;

        frameAccumulator.ensureSize(fb.textureWidth, fb.textureHeight);
        if (!frameAccumulator.isReady()) return;

        GpuBuffer strengthBuf = ubo(std140Float(config.intensity));
        GpuBuffer timeBuf = ubo(std140Float(crtTime));

        CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
        try (
            RenderPass pass = enc.createRenderPass(
                () -> "mb_crt",
                frameAccumulator.getTextureView(),
                OptionalInt.empty()
            )
        ) {
            pass.setPipeline(crtPipeline);
            pass.bindTexture(
                "DiffuseSampler",
                srcView,
                frameAccumulator.getSampler()
            );
            pass.setUniform("CrtStrength", strengthBuf);
            pass.setUniform("CrtTime", timeBuf);
            pass.draw(0, 3);
        }

        copyToFramebuffer(enc, fb);

        strengthBuf.close();
        timeBuf.close();
    }

    private void renderGlossy(Framebuffer fb) {
        if (glossyPipeline == null) return;

        GpuTextureView srcView = fb.getColorAttachmentView();
        if (srcView == null) return;

        frameAccumulator.ensureSize(fb.textureWidth, fb.textureHeight);
        if (!frameAccumulator.isReady()) return;

        GpuBuffer strengthBuf = ubo(std140Float(config.intensity));

        CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
        try (
            RenderPass pass = enc.createRenderPass(
                () -> "mb_glossy",
                frameAccumulator.getTextureView(),
                OptionalInt.empty()
            )
        ) {
            pass.setPipeline(glossyPipeline);
            pass.bindTexture(
                "DiffuseSampler",
                srcView,
                frameAccumulator.getSampler()
            );
            pass.setUniform("GlossyStrength", strengthBuf);
            pass.draw(0, 3);
        }

        copyToFramebuffer(enc, fb);

        strengthBuf.close();
    }

    private void renderAccum(Framebuffer fb, GpuTextureView mainView) {
        if (accumPipeline == null) return;

        float blend = Math.max(
            0.0f,
            Math.min(0.95f, config.intensity * 0.95f * blendAlpha)
        );

        GpuBuffer blendBuf = ubo(std140Float(blend));
        GpuTextureView prevView = RenderSystem.getDevice().createTextureView(
            frameAccumulator.getTexture()
        );

        CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
        try (
            RenderPass pass = enc.createRenderPass(
                () -> "mb_accum",
                frameAccumulator.getTextureView(),
                OptionalInt.empty()
            )
        ) {
            pass.setPipeline(accumPipeline);
            pass.bindTexture(
                "CurrentFrame",
                mainView,
                frameAccumulator.getSampler()
            );
            pass.bindTexture(
                "PrevFrame",
                prevView,
                frameAccumulator.getSampler()
            );
            pass.setUniform("BlendFactor", blendBuf);
            pass.draw(0, 3);
        }

        copyToFramebuffer(enc, fb);

        blendBuf.close();
        prevView.close();
    }

    private void renderVelocity(
        MinecraftClient client,
        Framebuffer fb,
        GpuTextureView mainView,
        int w,
        int h,
        boolean gaussian
    ) {
        RenderPipeline pipeline = gaussian
            ? enhancedPipeline
            : velocityPipeline;
        if (pipeline == null) return;

        float fov = client.options.getFov().getValue().floatValue();
        Vector2f vel = velocityCalculator.getScreenSpaceVelocity(w, h, fov);

        float speed = vel.length();
        if (speed < VELOCITY_THRESHOLD) {
            MotionBlurMod.LOGGER.debug("velocity too small: speed={}", speed);
            return;
        }

        float maxBlur = 0.008f + config.intensity * 0.02f;
        if (speed > maxBlur) vel.mul(maxBlur / speed);
        vel.mul(blendAlpha);

        GpuBuffer velBuf = ubo(std140Vec2(vel.x, vel.y));
        GpuBuffer samplesBuf = ubo(std140Int(getEffectiveSampleCount()));
        GpuBuffer intBuf = ubo(std140Float(config.intensity));

        CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
        try (
            RenderPass pass = enc.createRenderPass(
                () -> "mb_velocity",
                frameAccumulator.getTextureView(),
                OptionalInt.empty()
            )
        ) {
            pass.setPipeline(pipeline);
            pass.bindTexture(
                "DiffuseSampler",
                mainView,
                frameAccumulator.getSampler()
            );
            pass.setUniform("Velocity", velBuf);
            pass.setUniform("Samples", samplesBuf);
            pass.setUniform("Intensity", intBuf);
            pass.draw(0, 3);
        }

        copyToFramebuffer(enc, fb);

        velBuf.close();
        samplesBuf.close();
        intBuf.close();
    }

    private void copyToFramebuffer(CommandEncoder enc, Framebuffer dest) {
        if (copyPipeline == null) return;
        GpuTextureView destView = dest.getColorAttachmentView();
        if (destView == null) return;

        try (
            RenderPass pass = enc.createRenderPass(
                () -> "mb_copy",
                destView,
                OptionalInt.empty()
            )
        ) {
            pass.setPipeline(copyPipeline);
            pass.bindTexture(
                "Tex",
                frameAccumulator.getTextureView(),
                frameAccumulator.getSampler()
            );
            pass.draw(0, 3);
        }
    }

    private void ensurePipelines() {
        if (accumPipeline != null) return;
        MotionBlurMod.LOGGER.info("Building render pipelines");

        accumPipeline = RenderPipeline.builder(
            RenderPipelines.POST_EFFECT_PROCESSOR_SNIPPET
        )
            .withLocation(
                Identifier.of(MotionBlurMod.MOD_ID, "pipeline/mb_accum")
            )
            .withVertexShader(VERT)
            .withFragmentShader(ACCUM_FRAG)
            .withSampler("CurrentFrame")
            .withSampler("PrevFrame")
            .withUniform("BlendFactor", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withoutBlend()
            .build();

        velocityPipeline = RenderPipeline.builder(
            RenderPipelines.POST_EFFECT_PROCESSOR_SNIPPET
        )
            .withLocation(
                Identifier.of(MotionBlurMod.MOD_ID, "pipeline/mb_velocity")
            )
            .withVertexShader(VERT)
            .withFragmentShader(VEL_FRAG)
            .withSampler("DiffuseSampler")
            .withUniform("Velocity", UniformType.UNIFORM_BUFFER)
            .withUniform("Samples", UniformType.UNIFORM_BUFFER)
            .withUniform("Intensity", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withoutBlend()
            .build();

        enhancedPipeline = RenderPipeline.builder(
            RenderPipelines.POST_EFFECT_PROCESSOR_SNIPPET
        )
            .withLocation(
                Identifier.of(MotionBlurMod.MOD_ID, "pipeline/mb_enhanced")
            )
            .withVertexShader(VERT)
            .withFragmentShader(ENH_FRAG)
            .withSampler("DiffuseSampler")
            .withUniform("Velocity", UniformType.UNIFORM_BUFFER)
            .withUniform("Samples", UniformType.UNIFORM_BUFFER)
            .withUniform("Intensity", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withoutBlend()
            .build();

        copyPipeline = RenderPipeline.builder(
            RenderPipelines.POST_EFFECT_PROCESSOR_SNIPPET
        )
            .withLocation(
                Identifier.of(MotionBlurMod.MOD_ID, "pipeline/mb_copy")
            )
            .withVertexShader(VERT)
            .withFragmentShader(COPY_FRAG)
            .withSampler("Tex")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withoutBlend()
            .build();

        glossyPipeline = RenderPipeline.builder(
            RenderPipelines.POST_EFFECT_PROCESSOR_SNIPPET
        )
            .withLocation(
                Identifier.of(MotionBlurMod.MOD_ID, "pipeline/mb_glossy")
            )
            .withVertexShader(VERT)
            .withFragmentShader(GLOSSY_FRAG)
            .withSampler("DiffuseSampler")
            .withUniform("GlossyStrength", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withoutBlend()
            .build();

        crtPipeline = RenderPipeline.builder(
            RenderPipelines.POST_EFFECT_PROCESSOR_SNIPPET
        )
            .withLocation(
                Identifier.of(MotionBlurMod.MOD_ID, "pipeline/mb_crt")
            )
            .withVertexShader(VERT)
            .withFragmentShader(CRT_FRAG)
            .withSampler("DiffuseSampler")
            .withUniform("CrtStrength", UniformType.UNIFORM_BUFFER)
            .withUniform("CrtTime", UniformType.UNIFORM_BUFFER)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withoutBlend()
            .build();

        MotionBlurMod.LOGGER.info(
            "Pipelines built: accum={} velocity={} enhanced={} copy={} glossy={} crt={}",
            accumPipeline != null,
            velocityPipeline != null,
            enhancedPipeline != null,
            copyPipeline != null,
            glossyPipeline != null,
            crtPipeline != null
        );
    }

    private GpuBuffer ubo(ByteBuffer data) {
        return RenderSystem.getDevice().createBuffer(
            () -> "mb_ubo",
            GpuBuffer.USAGE_UNIFORM,
            data
        );
    }

    private ByteBuffer std140Float(float v) {
        ByteBuffer buf = ByteBuffer.allocateDirect(16).order(
            ByteOrder.nativeOrder()
        );
        buf.putFloat(v).putFloat(0).putFloat(0).putFloat(0);
        buf.flip();
        return buf;
    }

    private ByteBuffer std140Int(int v) {
        ByteBuffer buf = ByteBuffer.allocateDirect(16).order(
            ByteOrder.nativeOrder()
        );
        buf.putInt(v).putInt(0).putInt(0).putInt(0);
        buf.flip();
        return buf;
    }

    private ByteBuffer std140Vec2(float x, float y) {
        ByteBuffer buf = ByteBuffer.allocateDirect(16).order(
            ByteOrder.nativeOrder()
        );
        buf.putFloat(x).putFloat(y).putFloat(0).putFloat(0);
        buf.flip();
        return buf;
    }

    private boolean shouldRender(MinecraftClient client) {
        if (!config.enabled) return false;
        if (client.world == null || client.player == null) return false;
        if (inGui) return false;
        if (client.getDebugHud().shouldShowDebugHud()) return false;
        if (
            config.blurType != 1 &&
            !velocityCalculator.hasMeaningfulVelocity(VELOCITY_THRESHOLD)
        ) return false;
        return true;
    }

    private int getEffectiveSampleCount() {
        return config.sampleCount;
    }

    public void onTeleport() {
        velocityCalculator.onTeleport();
        blendAlpha = 0.0f;
    }

    public void onWorldUnload() {
        velocityCalculator.reset();
        blendAlpha = 0.0f;
        frameAccumulator.cleanup();
        accumPipeline = null;
        velocityPipeline = null;
        enhancedPipeline = null;
        copyPipeline = null;
        glossyPipeline = null;
        crtPipeline = null;
    }

    public VelocityCalculator getVelocityCalculator() {
        return velocityCalculator;
    }
}
