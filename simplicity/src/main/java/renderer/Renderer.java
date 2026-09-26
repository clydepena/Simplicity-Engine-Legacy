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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.opengl.*;

import simplicity.Camera;
import simplicity.Window;
import util.AssetPool;
import util.Resources;
import components.SpriteRenderer;


public class Renderer {

    private Shader defaultShader;
    private GLCapabilities capabilities;
    private Window window;
    private Shader shader;
    private boolean isBlending = true;
    private boolean isClearing = false;
    private Vector4f clearColor;
    private Camera camera;
    private Framebuffer framebuffer;
    private final int MAX_BATCH_SIZE = 1000;
    private final List<RenderBatch> batches = new ArrayList<>();
    private boolean beginStart = false;
    private int fullscreenVao = 0;   // empty: the core profile needs a bound VAO even without vertex data

    public Renderer(Window window) {
        this.window = window;
    }

    public void addSprite(SpriteRenderer spr) {
        int z = spr.gameObject.transform.zIndex;
        Texture tex = spr.getTexture();
        for (RenderBatch b : batches) {
            if (b.hasRoom() && b.getZIndex() == z && (tex == null || b.hasTexture(tex) || b.hasTextureRoom())) {
                b.addSprite(spr);
                return;
            }
        }
        RenderBatch b = new RenderBatch(MAX_BATCH_SIZE, z);
        b.start();
        b.addSprite(spr);
        batches.add(b);
        Collections.sort(batches);
    }

    public void removeSprite(SpriteRenderer spr) {
        for (RenderBatch b : batches) if (b.remove(spr)) return;
    }

    public void removeAllSprites() {
        while (!batches.isEmpty()) {
            batches.removeLast().destroy();
        }
    }

    public void drawSprites() {
        checkInPass("drawSprites()");
        List<SpriteRenderer> moved = new ArrayList<>();
        for (RenderBatch b : batches) b.render(shader, moved);
        for (SpriteRenderer spr : moved) addSprite(spr);

        // free batches that lost all their sprites (removals or zIndex moves)
        batches.removeIf(b -> {
            if (!b.isEmpty()) return false;
            b.destroy();
            return true;
        });
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
        removeAllSprites();
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