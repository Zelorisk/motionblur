#version 330

uniform sampler2D CurrentFrame;
uniform sampler2D PrevFrame;

layout(std140) uniform BlendFactor {
    float blendFactor;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 current = texture(CurrentFrame, texCoord);
    vec4 prev = texture(PrevFrame, texCoord);
    fragColor = mix(current, prev, blendFactor);
}
