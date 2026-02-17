#version 330

uniform sampler2D DiffuseSampler;

layout(std140) uniform GlossyStrength {
    float glossyStrength;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 uv = texCoord;
    vec2 center = uv - 0.5;

    float aberration = glossyStrength * 0.0018;
    vec4 col;
    col.r = texture(DiffuseSampler, uv + center * aberration).r;
    col.g = texture(DiffuseSampler, uv).g;
    col.b = texture(DiffuseSampler, uv - center * aberration).b;
    col.a = texture(DiffuseSampler, uv).a;

    vec2 texelSize = 1.0 / vec2(textureSize(DiffuseSampler, 0));
    vec4 n  = texture(DiffuseSampler, uv + vec2(0.0,  texelSize.y));
    vec4 s  = texture(DiffuseSampler, uv + vec2(0.0, -texelSize.y));
    vec4 e  = texture(DiffuseSampler, uv + vec2( texelSize.x, 0.0));
    vec4 w  = texture(DiffuseSampler, uv + vec2(-texelSize.x, 0.0));
    vec4 sharp = col * (1.0 + glossyStrength * 3.2) - (n + s + e + w) * (glossyStrength * 0.8);
    col = clamp(sharp, 0.0, 1.0);

    float vignetteDist = dot(center, center);
    float gloss = 1.0 + glossyStrength * (0.12 - vignetteDist * 0.18);
    col.rgb *= clamp(gloss, 0.95, 1.15);

    fragColor = col;
}
