#version 330

uniform sampler2D DiffuseSampler;

layout(std140) uniform CrtStrength {
    float crtStrength;
};

layout(std140) uniform CrtTime {
    float crtTime;
};

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

vec2 barrel(vec2 uv, float amount) {
    vec2 c = uv - 0.5;
    float r2 = dot(c, c);
    return uv + c * r2 * amount;
}

float screenMask(vec2 uv, float feather) {
    vec2 d = smoothstep(vec2(0.0), vec2(feather), uv) *
             smoothstep(vec2(0.0), vec2(feather), 1.0 - uv);
    return d.x * d.y;
}

void main() {
    float s = crtStrength;

    vec2 uv = barrel(texCoord, s * 0.18);

    float feather = 0.018 + s * 0.012;
    float mask = screenMask(uv, feather);

    vec2 texSize = vec2(textureSize(DiffuseSampler, 0));
    vec2 pixel = uv * texSize;
    vec2 safeUv = clamp(uv, 0.0, 1.0);

    vec2 aberr = (uv - 0.5) * s * 0.006;
    float r = texture(DiffuseSampler, clamp(safeUv - aberr, 0.0, 1.0)).r;
    float g = texture(DiffuseSampler, safeUv).g;
    float b = texture(DiffuseSampler, clamp(safeUv + aberr, 0.0, 1.0)).b;
    vec3 col = vec3(r, g, b);

    float luma = dot(col, vec3(0.299, 0.587, 0.114));
    float glowRadius = (1.0 - luma) * s * 0.007;
    if (glowRadius > 0.0002) {
        vec2 gr = vec2(glowRadius, 0.0);
        vec3 glow;
        glow  = texture(DiffuseSampler, clamp(safeUv + gr, 0.0, 1.0)).rgb;
        glow += texture(DiffuseSampler, clamp(safeUv - gr, 0.0, 1.0)).rgb;
        glow += texture(DiffuseSampler, clamp(safeUv + gr.yx, 0.0, 1.0)).rgb;
        glow += texture(DiffuseSampler, clamp(safeUv - gr.yx, 0.0, 1.0)).rgb;
        col = mix(col, glow * 0.25, s * 0.22);
    }

    float scanFreq = texSize.y * 3.14159265;
    float scan = sin(uv.y * scanFreq) * 0.5 + 0.5;
    scan = pow(scan, 0.6);
    col *= mix(1.0, scan * 0.82 + 0.18, s * 0.55);

    int px = int(mod(pixel.x, 3.0));
    vec3 mask3 = vec3(
        px == 0 ? 1.0 : 0.18,
        px == 1 ? 1.0 : 0.18,
        px == 2 ? 1.0 : 0.18
    );
    col *= mix(vec3(1.0), mask3, s * 0.25);

    float staticNoise = hash(floor(pixel * mix(1.0, 0.5, s)) + vec2(crtTime * 7.3, crtTime * 3.1));
    float staticLine = step(0.994 - s * 0.006, staticNoise);
    col += staticLine * s * 0.18;

    float rollPhase = fract(crtTime * 0.031);
    float rollDist = abs(uv.y - rollPhase);
    rollDist = min(rollDist, 1.0 - rollDist);
    float rollBand = smoothstep(0.012, 0.0, rollDist);
    col += rollBand * hash(vec2(uv.x * 200.0, crtTime)) * s * 0.09;

    vec2 vig = uv - 0.5;
    float vignette = 1.0 - dot(vig, vig) * s * 1.4;
    col *= clamp(vignette, 0.0, 1.0);

    vec3 phosphorTint = vec3(0.85, 1.0, 0.78);
    col *= mix(vec3(1.0), phosphorTint, s * 0.18);

    vec3 bezelColor = vec3(0.015, 0.015, 0.02);
    col = mix(bezelColor, col, mask);

    fragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
