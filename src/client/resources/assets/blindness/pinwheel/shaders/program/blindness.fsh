#version 150

#include veil:space_helper

uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;
#ifndef BLINDNESS_DEPTH_ONLY
uniform sampler2D VeilDynamicNormal;
#endif

uniform float BaseBrightness;
uniform float BlurStrength;
uniform float Saturation;
uniform float OutlineThickness;
uniform float GlowStrength;
uniform vec3 ScanOrigin;
uniform float ScanRadius;
uniform float ScanProgress;
uniform float FadeProgress;
uniform int ScanActive;
uniform int ScanMode;
uniform int ScanHitCount;
uniform vec3 ScanHit0;
uniform vec3 ScanHit1;
uniform vec3 ScanHit2;
uniform vec3 ScanHit3;
uniform vec3 CreatureOrigin;
uniform float CreatureStrength;

in vec2 texCoord;
out vec4 fragColor;

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

float depthAt(vec2 uv) {
    return texture(DiffuseDepthSampler, clamp(uv, vec2(0.001), vec2(0.999))).r;
}

void main() {
    vec2 texel = 1.0 / vec2(textureSize(DiffuseSampler0, 0));
    vec2 blurStep = texel * max(0.0, BlurStrength);
    vec3 center = texture(DiffuseSampler0, texCoord).rgb;
    vec3 blurred = center * 0.30;
    blurred += texture(DiffuseSampler0, texCoord + vec2( blurStep.x, 0.0)).rgb * 0.12;
    blurred += texture(DiffuseSampler0, texCoord + vec2(-blurStep.x, 0.0)).rgb * 0.12;
    blurred += texture(DiffuseSampler0, texCoord + vec2(0.0,  blurStep.y)).rgb * 0.12;
    blurred += texture(DiffuseSampler0, texCoord + vec2(0.0, -blurStep.y)).rgb * 0.12;
    blurred += texture(DiffuseSampler0, texCoord + blurStep).rgb * 0.055;
    blurred += texture(DiffuseSampler0, texCoord - blurStep).rgb * 0.055;
    blurred += texture(DiffuseSampler0, texCoord + vec2( blurStep.x, -blurStep.y)).rgb * 0.055;
    blurred += texture(DiffuseSampler0, texCoord + vec2(-blurStep.x,  blurStep.y)).rgb * 0.055;

    float gray = luminance(blurred);
    vec3 desaturated = mix(vec3(gray), blurred, clamp(Saturation, 0.0, 1.0));
    float depth = depthAt(texCoord);
    vec3 viewPos = screenToLocalSpace(texCoord, depth).xyz;
    float viewDistance = length(viewPos);
    float nearLight = 1.0 - smoothstep(1.0, 7.0, viewDistance);
    float strongLight = smoothstep(0.55, 1.0, luminance(blurred));
    vec3 worldColor = desaturated * (BaseBrightness + nearLight * 0.16);
    worldColor += vec3(0.18, 0.17, 0.14) * strongLight * (0.25 + nearLight * 0.35);

    float dL = depthAt(texCoord - vec2(texel.x * OutlineThickness, 0.0));
    float dR = depthAt(texCoord + vec2(texel.x * OutlineThickness, 0.0));
    float dU = depthAt(texCoord + vec2(0.0, texel.y * OutlineThickness));
    float dD = depthAt(texCoord - vec2(0.0, texel.y * OutlineThickness));
    float depthEdge = min(1.0, (abs(dL - dR) + abs(dU - dD)) * 180.0);
    float normalEdge = 0.0;
#ifndef BLINDNESS_DEPTH_ONLY
    vec3 n0 = texture(VeilDynamicNormal, texCoord).xyz;
    vec3 nx = texture(VeilDynamicNormal, texCoord + vec2(texel.x * OutlineThickness, 0.0)).xyz;
    vec3 ny = texture(VeilDynamicNormal, texCoord + vec2(0.0, texel.y * OutlineThickness)).xyz;
    normalEdge = min(1.0, length(n0 - nx) + length(n0 - ny));
#endif
    float geometryEdge = max(depthEdge, normalEdge);

    float scanOutline = 0.0;
    if (ScanActive == 1 && depth < 0.99999) {
        float shellDistance = abs(length(viewPos - ScanOrigin) - ScanRadius);
        float inner = 1.0 - smoothstep(0.06, 0.16 + OutlineThickness * 0.035, shellDistance);
        float outer = 1.0 - smoothstep(0.12, 0.48 + OutlineThickness * 0.08, shellDistance);
        float authorizedMask = 1.0;
        if (ScanMode == 1) {
            authorizedMask = 0.0;
            if (ScanHitCount > 0) authorizedMask = max(authorizedMask, 1.0 - smoothstep(0.65, 1.65, length(viewPos - ScanHit0)));
            if (ScanHitCount > 1) authorizedMask = max(authorizedMask, 1.0 - smoothstep(0.65, 1.65, length(viewPos - ScanHit1)));
            if (ScanHitCount > 2) authorizedMask = max(authorizedMask, 1.0 - smoothstep(0.65, 1.65, length(viewPos - ScanHit2)));
            if (ScanHitCount > 3) authorizedMask = max(authorizedMask, 1.0 - smoothstep(0.65, 1.65, length(viewPos - ScanHit3)));
        }
        scanOutline = geometryEdge * (inner + outer * 0.42 * GlowStrength) * FadeProgress * authorizedMask;
    }

    float creatureOutline = 0.0;
    if (CreatureStrength > 0.0 && depth < 0.99999) {
        float creatureDistance = length(viewPos - CreatureOrigin);
        creatureOutline = geometryEdge * (1.0 - smoothstep(0.4, 1.1, creatureDistance)) * CreatureStrength * 0.65;
    }

    vec3 outlineColor = vec3(0.62, 0.88, 1.0) * scanOutline * GlowStrength;
    outlineColor += vec3(0.82, 0.88, 0.94) * creatureOutline;
    fragColor = vec4(max(worldColor, outlineColor), 1.0);
}
