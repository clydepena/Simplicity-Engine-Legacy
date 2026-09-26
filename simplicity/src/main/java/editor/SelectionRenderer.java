package editor;

import static org.lwjgl.opengl.GL33.GL_NEAREST;
import static org.lwjgl.opengl.GL33.GL_RGB;
import static org.lwjgl.opengl.GL33.GL_RGB8;
import static org.lwjgl.opengl.GL33.GL_UNSIGNED_BYTE;

import java.util.List;

import org.joml.Vector4f;

import renderer.Framebuffer;
import renderer.PickingTexture;
import renderer.Renderer;
import renderer.Shader;
import scenes.World2DLayer;
import simplicity.Application.RenderContext;
import simplicity.Camera;
import simplicity.GameObject;
import util.AssetPool;
import util.Resources;

/**
 * Editor-side selection rendering: picks objects under a pixel and outlines the selected ones.
 * Both work through the shared Renderer on the world's sprites (entity id = uid + 1 in every vertex),
 * so the world knows nothing about selection. Every pass restores the renderer state it changed.
 */
public class SelectionRenderer {

    private static final int MAX_IDS_PER_PASS = 32;   // must match MAX_SELECTED in selectionMask.glsl
    private static final Vector4f CLEAR_ZERO = new Vector4f(0, 0, 0, 0);

    private final Shader pickingShader;
    private final Shader maskShader;
    private final Shader outlineShader;

    // created lazily at the world frame's size
    private PickingTexture pickingTexture;
    private Framebuffer mask;

    private final Vector4f outlineColor = new Vector4f(1.0f, 0.55f, 0.1f, 1.0f);   // orange accent
    private int outlineThickness = 2;                                               // pixels
    private final int[] ids = new int[MAX_IDS_PER_PASS];

    public SelectionRenderer() {
        pickingShader = AssetPool.getShaderFromRes(Resources.Editor.SHADER_PICKING);
        maskShader = AssetPool.getShaderFromRes(Resources.Editor.SHADER_SELECTION_MASK);
        outlineShader = AssetPool.getShaderFromRes(Resources.Editor.SHADER_SELECTION_OUTLINE);
    }

    /**
     * The topmost object at frame pixel (x, y) (bottom-left origin), or null.
     * Renders the id buffer only when called, so call it on clicks rather than every frame.
     */
    public GameObject pick(RenderContext renderContext, World2DLayer world, int x, int y) {
        Camera camera = world.camera();
        if (camera == null) return null;   // no scene set yet

        Framebuffer frame = renderContext.framebuffer();
        if (pickingTexture == null) {
            pickingTexture = new PickingTexture(frame.getWidth(), frame.getHeight());
        } else {
            pickingTexture.resize(frame.getWidth(), frame.getHeight());
        }

        Renderer r = renderContext.renderer();
        SavedState saved = new SavedState(r);

        r.setFramebuffer(pickingTexture.getFramebuffer());
        r.setShader(pickingShader);
        r.setCamera(camera);
        r.setBlending(false);   // ids must not mix
        r.setClearing(true);
        r.setClearColor(CLEAR_ZERO);   // 0 = no object
        r.begin();
        r.drawSprites();
        r.end();

        saved.restore(r, frame);

        int uid = pickingTexture.readPixel(x, y);
        return uid < 0 ? null : world.getGameObject(uid);
    }

    /** Draws an outline around every selected object onto the frame in renderContext. */
    public void drawOutline(RenderContext renderContext, World2DLayer world, List<GameObject> selected) {
        if (selected == null || selected.isEmpty()) return;
        Camera camera = world.camera();
        if (camera == null) return;

        Framebuffer frame = renderContext.framebuffer();
        if (mask == null || mask.getWidth() != frame.getWidth() || mask.getHeight() != frame.getHeight()) {
            if (mask != null) mask.destroy();
            mask = new Framebuffer(frame.getWidth(), frame.getHeight(), GL_RGB8, GL_RGB, GL_UNSIGNED_BYTE, GL_NEAREST);
        }

        Renderer r = renderContext.renderer();
        SavedState saved = new SavedState(r);

        // 1. mask: 1 where a selected sprite is, in chunks of MAX_IDS_PER_PASS (only the first pass clears)
        r.setFramebuffer(mask);
        r.setShader(maskShader);
        r.setCamera(camera);
        r.setBlending(false);
        r.setClearColor(CLEAR_ZERO);

        int i = 0;
        boolean first = true;
        boolean any = false;
        while (i < selected.size()) {
            int count = 0;
            while (i < selected.size() && count < MAX_IDS_PER_PASS) {
                GameObject go = selected.get(i++);
                if (go == null || go.isDead()) continue;
                ids[count++] = go.getUid() + 1;
            }
            if (count == 0) continue;

            r.setClearing(first);
            r.begin();
            maskShader.uploadIntArray("uSelectedIds", ids);
            maskShader.uploadInt("uSelectedCount", count);
            r.drawSprites();
            r.end();
            first = false;
            any = true;
        }

        // 2. edges of the mask, blended over the frame
        if (any) {
            r.setFramebuffer(frame);
            r.setShader(outlineShader);
            r.setBlending(true);
            r.setClearing(false);
            r.begin();
            outlineShader.uploadTexture("uMask", 0);
            outlineShader.uploadVec4f("uColor", outlineColor);
            outlineShader.uploadInt("uThickness", outlineThickness);
            r.drawFullscreen(mask.getTexId());
            r.end();
        }

        saved.restore(r, frame);
    }

    public void setOutlineColor(Vector4f rgba) {
        outlineColor.set(rgba);
    }

    public void setOutlineThickness(int pixels) {
        outlineThickness = Math.max(1, pixels);
    }

    public void destroy() {
        if (pickingTexture != null) {
            pickingTexture.destroy();
            pickingTexture = null;
        }
        if (mask != null) {
            mask.destroy();
            mask = null;
        }
    }

    /** Renderer state the passes change; restored so later layers see the renderer as they left it. */
    private static final class SavedState {
        private final Shader shader;
        private final Camera camera;
        private final boolean blending;
        private final boolean clearing;
        private final Vector4f clearColor;

        SavedState(Renderer r) {
            shader = r.getShader();
            camera = r.getCamera();
            blending = r.isBlending();
            clearing = r.isClearing();
            clearColor = r.getClearColor();
        }

        void restore(Renderer r, Framebuffer frame) {
            r.setFramebuffer(frame);
            r.setShader(shader);
            r.setCamera(camera);
            r.setBlending(blending);
            r.setClearing(clearing);
            r.setClearColor(clearColor);
        }
    }
}
