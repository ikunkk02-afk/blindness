#version 150

uniform sampler2D ContactMaskSampler;
uniform sampler2D DiffuseDepthSampler;
#ifndef BLINDNESS_DEPTH_ONLY
uniform sampler2D VeilDynamicNormal;
#endif
uniform int ModelMaskReady;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    if (ModelMaskReady == 0) {
        fragColor = vec4(0.0);
        return;
    }
    vec2 texel = 1.0 / vec2(textureSize(ContactMaskSampler, 0));
    vec2 centerMask = texture(ContactMaskSampler, texCoord).rg;
    vec2 alphaEdge = vec2(0.0);
    float depthEdge = 0.0;
    float centerDepth = texture(DiffuseDepthSampler, texCoord).r;
#ifndef BLINDNESS_DEPTH_ONLY
    vec3 centerNormal = texture(VeilDynamicNormal, texCoord).xyz;
    float normalEdge = 0.0;
#endif
    const ivec2 OFFSETS[8] = ivec2[8](
        ivec2(-1, 0), ivec2(1, 0), ivec2(0, -1), ivec2(0, 1),
        ivec2(-1, -1), ivec2(1, -1), ivec2(-1, 1), ivec2(1, 1)
    );
    for (int i = 0; i < 8; i++) {
        vec2 uv = texCoord + vec2(OFFSETS[i]) * texel;
        vec2 sampleMask = texture(ContactMaskSampler, uv).rg;
        alphaEdge = max(alphaEdge, abs(centerMask - sampleMask));
        depthEdge = max(depthEdge, abs(centerDepth - texture(DiffuseDepthSampler, uv).r) * 220.0);
#ifndef BLINDNESS_DEPTH_ONLY
        normalEdge = max(normalEdge, length(centerNormal - texture(VeilDynamicNormal, uv).xyz));
#endif
    }
    vec2 modelPresence = max(centerMask, vec2(0.0));
    vec2 geometryEdge = modelPresence * clamp(depthEdge, 0.0, 1.0);
#ifndef BLINDNESS_DEPTH_ONLY
    geometryEdge = max(geometryEdge, modelPresence * clamp(normalEdge * 0.65, 0.0, 1.0));
#endif
    vec2 edge = clamp(max(alphaEdge, geometryEdge), 0.0, 1.0);
    fragColor = vec4(edge, 0.0, 1.0);
}
