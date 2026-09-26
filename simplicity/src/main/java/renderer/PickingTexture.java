package renderer;

import static org.lwjgl.opengl.GL46.*;

/**
 * Id buffer for picking: pickingShader.glsl writes each sprite's entity id (uid + 1) into it, 0 = nothing.
 * Render into getFramebuffer() through the Renderer (clear to 0, blending off), then readPixel().
 */
public class PickingTexture {

    private Framebuffer framebuffer;

    public PickingTexture(int width, int height) {
        framebuffer = create(width, height);
    }

    private static Framebuffer create(int width, int height) {
        // float storage keeps ids exact up to 2^24; nearest so ids are never blended
        return new Framebuffer(width, height, GL_RGB32F, GL_RGB, GL_FLOAT, GL_NEAREST);
    }

    /** Recreates the buffer if its size differs (contents are lost). */
    public void resize(int width, int height) {
        if (framebuffer.getWidth() == width && framebuffer.getHeight() == height) return;
        framebuffer.destroy();
        framebuffer = create(width, height);
    }

    public Framebuffer getFramebuffer() {
        return framebuffer;
    }

    /** Uid of the object at pixel (x, y), bottom-left origin, or -1 for none / out of bounds. */
    public int readPixel(int x, int y) {
        if (x < 0 || y < 0 || x >= framebuffer.getWidth() || y >= framebuffer.getHeight()) return -1;

        glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer.getFboID());
        glReadBuffer(GL_COLOR_ATTACHMENT0);

        float[] pixels = new float[3];
        glReadPixels(x, y, 1, 1, GL_RGB, GL_FLOAT, pixels);
        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);

        return (int) (pixels[0] + 0.5f) - 1;
    }

    public void destroy() {
        framebuffer.destroy();
    }
}
