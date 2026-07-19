#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;
out vec4 fragColor;

void main() {
    float textureAlpha = texture(Sampler0, texCoord0).a;
    if (textureAlpha < 0.1) discard;
    vec2 channel = vertexColor.rg * vertexColor.a * ColorModulator.a;
    fragColor = vec4(channel, 0.0, 1.0);
}
