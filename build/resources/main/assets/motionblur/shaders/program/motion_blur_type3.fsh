#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 Velocity;
uniform int Samples;
uniform float Intensity;

in vec2 TexCoord;

out vec4 fragColor;

float gaussian(float x, float sigma) {
    return exp(-(x * x) / (2.0 * sigma * sigma));
}

void main() {
    float speed = length(Velocity);

    if (speed < 0.0001) {
        fragColor = texture(DiffuseSampler, TexCoord);
        return;
    }

    vec4 color = vec4(0.0);
    float totalWeight = 0.0;
    int count = max(Samples, 1);
    float sigma = float(count) / 4.0;

    for (int i = 0; i < count; i++) {
        float t = float(i) / float(count - 1) - 0.5;
        float weight = gaussian(t * float(count), sigma);
        vec2 offset = Velocity * t;
        vec2 sampleUV = clamp(TexCoord + offset, 0.0, 1.0);
        color += texture(DiffuseSampler, sampleUV) * weight;
        totalWeight += weight;
    }

    fragColor = color / totalWeight;
}
