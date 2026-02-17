#version 150

in vec2 Position;
in vec2 UV;

out vec2 TexCoord;

void main() {
    gl_Position = vec4(Position, 0.0, 1.0);
    TexCoord = UV;
}
