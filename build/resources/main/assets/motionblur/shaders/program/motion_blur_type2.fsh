#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 Velocity;
uniform int Samples;
uniform float Intensity;

in vec2 TexCoord;

out vec4 fragColor;

void main() {
    float speed = length(Velocity);

    if (speed < 0.0001) {
        fragColor = texture(DiffuseSampler, TexCoord);
        return;
    }

    vec4 color = vec4(0.0);
    int count = max(Samples, 1);

    for (int i = 0; i < count; i++) {
        float t = float(i) / float(count - 1) - 0.5;
        vec2 offset = Velocity * t;
        vec2 sampleUV = clamp(TexCoord + offset, 0.0, 1.0);
        color += texture(DiffuseSampler, sampleUV);
    }

    fragColor = color / float(count);
}
