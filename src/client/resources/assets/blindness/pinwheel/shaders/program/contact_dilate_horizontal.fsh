#version 150

uniform sampler2D DiffuseSampler0;
uniform float CenterOutlineThickness;
uniform float AdjacentOutlineThickness;
uniform float CenterGlowRadius;
uniform float AdjacentGlowRadius;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 texel = 1.0 / vec2(textureSize(DiffuseSampler0, 0));
    float centerCore = 0.0;
    float adjacentCore = 0.0;
    float centerGlow = 0.0;
    float adjacentGlow = 0.0;
    float centerCoreRadius = max(1.0, ceil(CenterOutlineThickness * 0.5));
    float adjacentCoreRadius = max(1.0, ceil(AdjacentOutlineThickness * 0.5));
    for (int x = -16; x <= 16; x++) {
        float distance = float(abs(x));
        vec2 edge = texture(DiffuseSampler0, texCoord + vec2(float(x) * texel.x, 0.0)).rg;
        if (distance <= centerCoreRadius) centerCore = max(centerCore, edge.r);
        if (distance <= adjacentCoreRadius) adjacentCore = max(adjacentCore, edge.g);
        if (distance <= CenterGlowRadius) {
            centerGlow = max(centerGlow, edge.r * (1.0 - distance / (CenterGlowRadius + 1.0)));
        }
        if (distance <= AdjacentGlowRadius) {
            adjacentGlow = max(adjacentGlow, edge.g * (1.0 - distance / (AdjacentGlowRadius + 1.0)));
        }
    }
    fragColor = vec4(centerCore, adjacentCore, centerGlow, adjacentGlow);
}
