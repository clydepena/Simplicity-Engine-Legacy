package renderer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.GL_BLEND;
import static org.lwjgl.opengl.GL33.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL33.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL33.GL_NEAREST;
import static org.lwjgl.opengl.GL33.GL_ONE;
import static org.lwjgl.opengl.GL33.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL33.glBlendFunc;
import static org.lwjgl.opengl.GL33.glClear;
import static org.lwjgl.opengl.GL33.glClearColor;
import static org.lwjgl.opengl.GL33.glDisable;
import static org.lwjgl.opengl.GL33.glEnable;
import static org.lwjgl.opengl.GL33.GL_DRAW_FRAMEBUFFER;
import static org.lwjgl.opengl.GL33.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL33.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.GL33.glBindFramebuffer;
import static org.lwjgl.opengl.GL33.glBlitFramebuffer;
import static org.lwjgl.opengl.GL33.GL_TEXTURE0;
import static org.lwjgl.opengl.GL33.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL33.GL_TRIANGLES;
import static org.lwjgl.opengl.GL33.glActiveTexture;
import static org.lwjgl.opengl.GL33.glBindTexture;
import static org.lwjgl.opengl.GL33.glBindVertexArray;
import static org.lwjgl.opengl.GL33.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL33.glDrawArrays;
import static org.lwjgl.opengl.GL33.glGenVertexArrays;


import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.opengl.*;

import simplicity.Camera;
import simplicity.Window;
import util.AssetPool;
import util.Resources;


public class Renderer {

    /** First texture unit free for draw()'s extra textures; sprite batches use units 0..7 (uTextures[8]). */
    public static final int FIRST_EXTRA_TEXTURE_UNIT = 8;

    private Shader defaultShader;
    private GLCapabilities capabilities;
    private Window window;
    private Shader shader;
    private boolean isBlending = true;
    private boolean isClearing = false;
    private Vector4f clearColor;
    private Camera camera;
    private Framebuffer framebuffer;
    private boolean beginStart = false;
    private int fullscreenVao = 0;   // empty: the core profile needs a bound VAO even without vertex data

    public Renderer(Window window) {
        this.window = window;
    }

    /**
     * Draws the drawable with the current shader, into the current pass. extraTextureIds are bound to units
     * FIRST_EXTRA_TEXTURE_UNIT, +1, ... for the whole draw (the caller uploads the matching sampler uniforms);
     * units below that belong to the drawable (sprite batches use 0..7).
     */
    public void draw(Drawable drawable, int... extraTextureIds) {
        checkInPass("draw()");
        for (int i = 0; i < extraTextureIds.length; i++) {
            glActiveTexture(GL_TEXTURE0 + FIRST_EXTRA_TEXTURE_UNIT + i);
            glBindTexture(GL_TEXTURE_2D, extraTextureIds[i]);
        }
        glActiveTexture(GL_TEXTURE0);

        drawable.draw(shader);

        for (int i = 0; i < extraTextureIds.length; i++) {
            glActiveTexture(GL_TEXTURE0 + FIRST_EXTRA_TEXTURE_UNIT + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
        glActiveTexture(GL_TEXTURE0);
    }

    public void init() {
        glfwMakeContextCurrent(window.ptr());
        glfwSwapInterval(1);
        capabilities = GL.createCapabilities();
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        defaultShader = AssetPool.getShaderFromRes(Resources.MAIN_SHADER);
        shader = defaultShader;
        clearColor = new Vector4f(0, 0, 0, 1);
        camera = new Camera(new Vector2f(0, 0));
        fullscreenVao = glGenVertexArrays();
    }

    public void onUpdate(float dt) {
        
    }

    public void destroy() {
        if (fullscreenVao != 0) {
            glDeleteVertexArrays(fullscreenVao);
            fullscreenVao = 0;
        }
    }
    
    public void begin() {
        checkNotInPass("begin()");
        framebuffer.bind();
        if (isBlending) {
            glEnable(GL_BLEND);
        } else {
            glDisable(GL_BLEND);
        }
        if (isClearing) {
            clear();
        }
        shader.use();
        shader.uploadMat4f("uProjection", camera.getProjectionMatrix());
        shader.uploadMat4f("uView", camera.getViewMatrix());
        beginStart = true;
    }

    public void end() {
        checkInPass("end()");
        beginStart = false;
        // DebugDraw.draw(camera);                         // #17 (before sprites = behind them, as in the old loop)
        // for (RenderBatch b : batches) b.render(shader); // #13
        // applyPendingZIndexMoves();
        shader.detach();
        framebuffer.unbind();
    }

    /**
     * Draws one triangle covering the whole framebuffer with the current shader, which builds it from gl_VertexID.
     * textureIds are bound to units 0, 1, ... (the caller uploads the matching sampler uniforms).
     */
    public void drawFullscreen(int... textureIds) {
        checkInPass("drawFullscreen()");
        for (int i = 0; i < textureIds.length; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, textureIds[i]);
        }

        glBindVertexArray(fullscreenVao);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);

        for (int i = 0; i < textureIds.length; i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
        glActiveTexture(GL_TEXTURE0);
    }

    public void present(Framebuffer source) {
        checkNotInPass("present()");
        glBindFramebuffer(GL_READ_FRAMEBUFFER, source.getFboID());
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        glBlitFramebuffer(0, 0, source.getWidth(), source.getHeight(),
                        0, 0, window.getFramebufferWidth(), window.getFramebufferHeight(),
                        GL_COLOR_BUFFER_BIT, GL_NEAREST);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void setShader(Shader shader) {
        checkNotInPass("setShader()");
        this.shader = shader;
    }

    public void restoreDefaultShader() {
        checkNotInPass("restoreDefaultShader()");
        this.shader = defaultShader;
    }

    public void swapBuffers() {
        checkNotInPass("swapBuffers()");
        glfwSwapBuffers(window.ptr());
    }

    public void setBlending(boolean isBlending) {
        checkNotInPass("setBlending()");
        this.isBlending = isBlending;
    }

    public void setClearing(boolean isClearing) {
        checkNotInPass("setClearing()");
        this.isClearing = isClearing;
    }

    public void setClearColor(Vector4f rgba) {
        checkNotInPass("setClearColor()");
        clearColor = rgba;
    }

    public void setCamera(Camera camera) {
        checkNotInPass("setCamera()");
        this.camera = camera;
    }

    public void setFramebuffer(Framebuffer framebuffer) {
        checkNotInPass("setFramebuffer()");
        this.framebuffer = framebuffer;
    }

    public Shader getShader() {
        return shader;
    }

    public Camera getCamera() {
        return camera;
    }

    public Framebuffer getFramebuffer() {
        return framebuffer;
    }

    public boolean isBlending() {
        return isBlending;
    }

    public boolean isClearing() {
        return isClearing;
    }

    /** A copy, so callers can restore it later with setClearColor(). */
    public Vector4f getClearColor() {
        return new Vector4f(clearColor);
    }

    private void checkNotInPass(String caller) {
        if (beginStart) throw new IllegalStateException(caller + " can't be called between begin() and end()");
    }

    private void checkInPass(String caller) {
        if (!beginStart) throw new IllegalStateException(caller + " must be called between begin() and end()");
    }

    public void clear() {
        glClearColor(clearColor.x, clearColor.y, clearColor.z, clearColor.w);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }
}