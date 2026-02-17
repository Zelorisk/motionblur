package com.chrismod.motionblur.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.util.OptionalDouble;
import net.minecraft.client.gl.GpuSampler;

public class FrameAccumulator {

    private GpuTexture texture;
    private GpuTextureView textureView;
    private GpuSampler sampler;
    private int width;
    private int height;

    public void ensureSize(int width, int height) {
        if (
            this.width == width && this.height == height && texture != null
        ) return;
        cleanup();
        this.width = width;
        this.height = height;

        texture = RenderSystem.getDevice().createTexture(
            "motionblur_accum",
            GpuTexture.USAGE_RENDER_ATTACHMENT |
                GpuTexture.USAGE_TEXTURE_BINDING,
            TextureFormat.RGBA8,
            width,
            height,
            1,
            1
        );
        textureView = RenderSystem.getDevice().createTextureView(texture);
        sampler = RenderSystem.getDevice().createSampler(
            AddressMode.CLAMP_TO_EDGE,
            AddressMode.CLAMP_TO_EDGE,
            FilterMode.LINEAR,
            FilterMode.LINEAR,
            1,
            OptionalDouble.of(1.0)
        );
    }

    public GpuTexture getTexture() {
        return texture;
    }

    public GpuTextureView getTextureView() {
        return textureView;
    }

    public GpuSampler getSampler() {
        return sampler;
    }

    public boolean isReady() {
        return texture != null && !texture.isClosed();
    }

    public void cleanup() {
        if (textureView != null) {
            textureView.close();
            textureView = null;
        }
        if (texture != null) {
            texture.close();
            texture = null;
        }
        if (sampler != null) {
            sampler.close();
            sampler = null;
        }
        width = 0;
        height = 0;
    }
}
