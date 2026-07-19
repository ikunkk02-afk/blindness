#version 150

#include veil:space_helper

uniform sampler2D DiffuseSampler0;
uniform sampler2D SceneDepthSampler;
uniform float CenterOutlineThickness;
uniform float AdjacentOutlineThickness;
uniform float CenterGlowRadius;
uniform float AdjacentGlowRadius;
uniform float CenterOutlineBrightness;
uniform float AdjacentOutlineBrightness;
uniform float CenterGlowStrength;
uniform float AdjacentGlowStrength;
uniform vec3 CreatureOrigin;
uniform float CreatureStrength;

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
    for (int y = -16; y <= 16; y++) {
        float distance = float(abs(y));
        vec4 sampleValue = texture(DiffuseSampler0, texCoord + vec2(0.0, float(y) * texel.y));
        if (distance <= centerCoreRadius) centerCore = max(centerCore, sampleValue.r);
        if (distance <= adjacentCoreRadius) adjacentCore = max(adjacentCore, sampleValue.g);
        if (distance <= CenterGlowRadius) {
            centerGlow = max(centerGlow, sampleValue.b * (1.0 - distance / (CenterGlowRadius + 1.0)));
        }
        if (distance <= AdjacentGlowRadius) {
            adjacentGlow = max(adjacentGlow, sampleValue.a * (1.0 - distance / (AdjacentGlowRadius + 1.0)));
        }
    }
    float core = max(centerCore * CenterOutlineBrightness, adjacentCore * AdjacentOutlineBrightness);
    float glow = max(centerGlow * CenterGlowStrength, adjacentGlow * AdjacentGlowStrength);
    float contact = clamp(max(core, glow), 0.0, 1.0);
    vec3 outputColor = vec3(0.72, 0.96, 1.0) * contact;

    if (CreatureStrength > 0.0) {
        float depth = texture(SceneDepthSampler, texCoord).r;
        if (depth < 0.99999) {
            vec3 viewPos = screenToLocalSpace(texCoord, depth).xyz;
            float cue = (1.0 - smoothstep(0.4, 1.1, length(viewPos - CreatureOrigin))) * CreatureStrength * 0.35;
            outputColor = max(outputColor, vec3(0.75, 0.88, 0.94) * cue);
        }
    }
    fragColor = vec4(outputColor, 1.0);
}
