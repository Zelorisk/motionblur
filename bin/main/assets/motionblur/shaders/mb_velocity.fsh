#version 330

uniform sampler2D DiffuseSampler;

layout(std140) uniform Velocity {
    vec2 velocity;
};

layout(std140) uniform Samples {
    int samples;
};

layout(std140) uniform Intensity {
    float intensity;
};

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    float speed = length(velocity);
    if (speed < 0.0001) {
        fragColor = texture(DiffuseSampler, texCoord);
        return;
    }

    int count = max(samples, 2);
    float jitter = hash(texCoord) - 0.5;

    vec4 color = vec4(0.0);
    float totalWeight = 0.0;

    for (int i = 0; i < count; i++) {
        float t = (float(i) + 0.5 + jitter) / float(count);
        float w = 1.0 - t;
        vec2 offset = velocity * -t;
        color += texture(DiffuseSampler, clamp(texCoord + offset, 0.0, 1.0)) * w;
        totalWeight += w;
    }

    fragColor = color / totalWeight;
}
