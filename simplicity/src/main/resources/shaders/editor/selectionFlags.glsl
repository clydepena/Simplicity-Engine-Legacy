#type vertex
#version 460 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec4 aColor;
layout (location = 2) in vec2 aTexCoords;
layout (location = 3) in float aTexId;
layout (location = 4) in float aEntityId;

// IdFlagBuffer: bit 1 = a sprite pixel inside uRect (covered or not), bit 2 = part of the sprite outside uRect or off the frame
layout (std430, binding = 0) buffer Flags { uint flags[]; };

uniform mat4 uProjection;
uniform mat4 uView;

out vec4 fColor;
out vec2 fTexCoords;
out float fTexId;
out float fEntityId;

void main() {
    fColor = aColor;
    fTexCoords = aTexCoords;
    fTexId = aTexId;
    fEntityId = aEntityId;

    gl_Position = uProjection * uView * vec4(aPos, 1.0);

    // pixels off the frame are never rasterized, so a corner off the frame marks the sprite as outside here
    int entity = int(aEntityId + 0.5);
    if (entity > 0 && entity < flags.length() && any(greaterThan(abs(gl_Position.xy), vec2(gl_Position.w)))) {
        atomicOr(flags[entity], 2u);
    }
}

#type fragment
#version 460 core

in vec4 fColor;
in vec2 fTexCoords;
in float fTexId;
in float fEntityId;

layout (std430, binding = 0) buffer Flags { uint flags[]; };

uniform sampler2D uTextures[8];
uniform vec4 uRect;   // frame pixels: (left, bottom, right + 1, top + 1)

out vec4 color;

// Runs for every sprite pixel, covered or not (no depth test), and flags its entity as inside or outside uRect.
void main() {
    vec4 texColor = vec4(1, 1, 1, 1);
    if (fTexId > 0) {
        int id = int(fTexId);
        texColor = fColor * texture(uTextures[id], fTexCoords);
    }

    if (texColor.a < 0.5) {
        discard;
    }

    int entity = int(fEntityId + 0.5);
    if (entity > 0 && entity < flags.length()) {
        vec2 p = gl_FragCoord.xy;
        bool inside = p.x >= uRect.x && p.y >= uRect.y && p.x < uRect.z && p.y < uRect.w;
        atomicOr(flags[entity], inside ? 1u : 2u);
    }
    color = vec4(0);
}
