#version 150

uniform sampler2D CurrentFrame;
uniform sampler2D PrevFrame;
uniform float BlendFactor;

in vec2 TexCoord;

out vec4 fragColor;

void main() {
    vec4 current = texture(CurrentFrame, TexCoord);
    vec4 previous = texture(PrevFrame, TexCoord);
    fragColor = mix(current, previous, BlendFactor);
}
