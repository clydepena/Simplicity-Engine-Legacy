package renderer;

import static org.lwjgl.opengl.GL46.*;

import java.nio.FloatBuffer;
import java.util.LinkedHashSet;
import java.util.Set;

import org.lwjgl.system.MemoryUtil;

/**
 * Framebuffer holding entity ids instead of colors, for picking: pickingShader.glsl writes each sprite's
 * entity id (uid + 1) into it, 0 = nothing. Render into it through the Renderer (clear to 0, blending off),
 * then readPixel(). Like Framebuffer, its size is fixed; replace it when the size changes.
 */
public class IdFramebuffer extends Framebuffer {

    public IdFramebuffer(int width, int height) {
        // float storage keeps ids exact up to 2^24; nearest so ids are never blended
        super(width, height, GL_RGB32F, GL_RGB, GL_FLOAT, GL_NEAREST);
    }

    /** Uid of the object at pixel (x, y), bottom-left origin, or -1 for none / out of bounds. */
    public int readPixel(int x, int y) {
        if (x < 0 || y < 0 || x >= getWidth() || y >= getHeight()) return -1;

        glBindFramebuffer(GL_READ_FRAMEBUFFER, getFboID());
        glReadBuffer(GL_COLOR_ATTACHMENT0);

        float[] pixels = new float[3];
        glReadPixels(x, y, 1, 1, GL_RGB, GL_FLOAT, pixels);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);

        return (int) (pixels[0] + 0.5f) - 1;
    }

    /**
     * Uids of every object with a pixel inside the rectangle between corners (x0, y0) and (x1, y1), inclusive,
     * bottom-left origin, corners in any order. Clamped to the buffer; empty if the rectangle is fully outside.
     */
    public Set<Integer> readRect(int x0, int y0, int x1, int y1) {
        int left = Math.max(0, Math.min(x0, x1));
        int bottom = Math.max(0, Math.min(y0, y1));
        int right = Math.min(getWidth() - 1, Math.max(x0, x1));
        int top = Math.min(getHeight() - 1, Math.max(y0, y1));

        Set<Integer> uids = new LinkedHashSet<>();
        if (left > right || bottom > top) return uids;

        int w = right - left + 1, h = top - bottom + 1;
        FloatBuffer pixels = MemoryUtil.memAllocFloat(w * h);
        try {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, getFboID());
            glReadBuffer(GL_COLOR_ATTACHMENT0);
            glReadPixels(left, bottom, w, h, GL_RED, GL_FLOAT, pixels);   // only the id channel
            glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);

            float last = 0;
            for (int i = 0; i < w * h; i++) {
                float id = pixels.get(i);
                if (id == last) continue;   // neighbouring pixels usually belong to the same sprite
                last = id;
                int uid = (int) (id + 0.5f) - 1;
                if (uid >= 0) uids.add(uid);
            }
        } finally {
            MemoryUtil.memFree(pixels);
        }
        return uids;
    }
}
