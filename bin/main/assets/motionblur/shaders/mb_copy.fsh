#version 330

uniform sampler2D Tex;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    fragColor = texture(Tex, texCoord);
}
