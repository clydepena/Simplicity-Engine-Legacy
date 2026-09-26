#type vertex
#version 460 core

// full-screen triangle from gl_VertexID (drawn by Renderer.drawFullscreen, no vertex data)
void main() {
    vec2 pos = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
    gl_Position = vec4(pos * 2.0 - 1.0, 0.0, 1.0);
}

#type fragment
#version 460 core

uniform sampler2D uMask;    // selectionMask.glsl output, same size as the target
uniform vec4 uColor;        // straight alpha
uniform int uThickness;     // in pixels

out vec4 color;

// Outer outline: pixels outside the mask with a mask pixel within uThickness.
void main() {
    ivec2 size = textureSize(uMask, 0);
    ivec2 p = ivec2(gl_FragCoord.xy);

    if (texelFetch(uMask, p, 0).r > 0.5) {
        discard;
    }

    int r = uThickness;
    for (int dy = -r; dy <= r; dy++) {
        for (int dx = -r; dx <= r; dx++) {
            if (dx * dx + dy * dy > r * r) continue;
            ivec2 q = clamp(p + ivec2(dx, dy), ivec2(0), size - 1);
            if (texelFetch(uMask, q, 0).r > 0.5) {
                // premultiplied, to match glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA)
                color = vec4(uColor.rgb * uColor.a, uColor.a);
                return;
            }
        }
    }
    discard;
}
