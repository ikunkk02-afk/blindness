#version 150

uniform vec4 ColorModulator;
in vec4 vertexColor;
out vec4 fragColor;

void main() {
    fragColor = vec4(vertexColor.rg * vertexColor.a * ColorModulator.a, 0.0, 1.0);
}
