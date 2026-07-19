#version 150

in vec3 Position;
in vec4 Color;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float LineWidth;
uniform vec2 ScreenSize;

out vec4 vertexColor;

const float VIEW_SHRINK = 1.0 - (1.0 / 256.0);
const mat4 VIEW_SCALE = mat4(
    VIEW_SHRINK, 0.0, 0.0, 0.0,
    0.0, VIEW_SHRINK, 0.0, 0.0,
    0.0, 0.0, VIEW_SHRINK, 0.0,
    0.0, 0.0, 0.0, 1.0
);

void main() {
    vec4 start = ProjMat * VIEW_SCALE * ModelViewMat * vec4(Position, 1.0);
    vec4 end = ProjMat * VIEW_SCALE * ModelViewMat * vec4(Position + Normal, 1.0);
    vec3 ndcStart = start.xyz / start.w;
    vec3 ndcEnd = end.xyz / end.w;
    vec2 direction = normalize((ndcEnd.xy - ndcStart.xy) * ScreenSize);
    vec2 offset = vec2(-direction.y, direction.x) * LineWidth / ScreenSize;
    if (offset.x < 0.0) offset *= -1.0;
    gl_Position = gl_VertexID % 2 == 0
        ? vec4((ndcStart + vec3(offset, 0.0)) * start.w, start.w)
        : vec4((ndcStart - vec3(offset, 0.0)) * start.w, start.w);
    vertexColor = Color;
}
