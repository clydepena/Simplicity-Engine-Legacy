package editor;

import static org.lwjgl.opengl.GL33.GL_NEAREST;
import static org.lwjgl.opengl.GL33.GL_RGB;
import static org.lwjgl.opengl.GL33.GL_RGB8;
import static org.lwjgl.opengl.GL33.GL_UNSIGNED_BYTE;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.joml.Vector4f;

import renderer.Framebuffer;
import renderer.IdFlagBuffer;
import renderer.IdFramebuffer;
import renderer.IdSetTexture;
import renderer.Renderer;
import renderer.Shader;
import scenes.World2DLayer;
import simplicity.Application.RenderContext;
import simplicity.Camera;
import simplicity.GameObject;
import util.AssetPool;
import util.Resources;

/**
 * Editor-side selection rendering: picks objects under a pixel or inside a rectangle, and outlines the selected ones.
 * All of it works through the shared Renderer on the world's sprites (entity id = uid + 1 in every vertex),
 * so the world knows nothing about selection. Every pass restores the renderer state it changed.
 */
public class SelectionRenderer {

    private static final Vector4f CLEAR_ZERO = new Vector4f(0, 0, 0, 0);

    // bits set by selectionFlags.glsl
    private static final int FLAG_INSIDE = 1;
    private static final int FLAG_OUTSIDE = 2;

    private final Shader pickingShader;
    private final Shader maskShader;
    private final Shader outlineShader;
    private final Shader flagsShader;

    // per-entity inside/outside flags for pickRect's non-default modes
    private final IdFlagBuffer flagBuffer = new IdFlagBuffer();
    private final Vector4f rect = new Vector4f();

    // created lazily at the world frame's size
    private IdFramebuffer idBuffer;
    private Framebuffer mask;

    // selected entity ids, for the mask shader; re-uploaded only when the selection changes
    private final IdSetTexture selectedIds = new IdSetTexture();
    private int[] ids = new int[64];

    private final Vector4f outlineColor = new Vector4f(1.0f, 0.55f, 0.1f, 1.0f);   // orange accent
    private int outlineThickness = 2;                                               // pixels

    public SelectionRenderer() {
        pickingShader = AssetPool.getShaderFromRes(Resources.Editor.SHADER_PICKING);
        maskShader = AssetPool.getShaderFromRes(Resources.Editor.SHADER_SELECTION_MASK);
        outlineShader = AssetPool.getShaderFromRes(Resources.Editor.SHADER_SELECTION_OUTLINE);
        flagsShader = AssetPool.getShaderFromRes(Resources.Editor.SHADER_SELECTION_FLAGS);
    }

    /**
     * The topmost object at frame pixel (x, y) (bottom-left origin), or null.
     * Renders the id buffer only when called, so call it on clicks rather than every frame.
     */
    public GameObject pick(RenderContext renderContext, World2DLayer world, int x, int y) {
        if (!renderIds(renderContext, world)) return null;
        int uid = idBuffer.readPixel(x, y);
        return uid < 0 ? null : world.getGameObject(uid);
    }

    /**
     * Objects in the rectangle between frame pixels (x0, y0) and (x1, y1), inclusive, bottom-left origin, corners in any order.
     * <ul>
     * <li>includeHidden: false = only objects with a visible pixel inside; true = also objects fully covered by others.</li>
     * <li>fullyInside: false = any pixel inside is enough (partial); true = the object's whole shape, covered parts
     *     included, must be inside (and on the frame).</li>
     * </ul>
     * Only sprite pixels that pass the alpha cutout count. Renders when called, so call it once (e.g. when a drag ends),
     * not every frame. The default (false, false) only needs the id buffer; the other modes add one flag pass.
     */
    public List<GameObject> pickRect(RenderContext renderContext, World2DLayer world, int x0, int y0, int x1, int y1,
                                     boolean includeHidden, boolean fullyInside) {
        List<GameObject> found = new ArrayList<>();

        Set<Integer> visible = null;
        if (!includeHidden) {
            if (!renderIds(renderContext, world)) return found;
            visible = idBuffer.readRect(x0, y0, x1, y1);
            if (!fullyInside) {
                for (int uid : visible) {
                    GameObject go = world.getGameObject(uid);
                    if (go != null) found.add(go);
                }
                return found;
            }
        }

        int[] flags = renderFlags(renderContext, world, x0, y0, x1, y1);
        if (flags == null) return found;

        for (GameObject go : world.getGameObjectList()) {
            int entity = go.getUid() + 1;
            if (entity >= flags.length) continue;
            int f = flags[entity];
            boolean inside = (f & FLAG_INSIDE) != 0;
            boolean outside = (f & FLAG_OUTSIDE) != 0;

            if (!inside) continue;
            if (fullyInside && outside) continue;
            if (visible != null && !visible.contains(go.getUid())) continue;
            found.add(go);
        }
        return found;
    }

    /**
     * Flag pass: draws every sprite, covered or not, and returns per-entity-id flags (FLAG_INSIDE / FLAG_OUTSIDE)
     * for the given rectangle. Null if the world has no scene yet.
     */
    private int[] renderFlags(RenderContext renderContext, World2DLayer world, int x0, int y0, int x1, int y1) {
        Camera camera = world.camera();
        if (camera == null) return null;

        int maxUid = -1;
        for (GameObject go : world.getGameObjectList()) maxUid = Math.max(maxUid, go.getUid());
        flagBuffer.clear(maxUid + 2);   // entity ids go up to maxUid + 1

        // the pass only writes flags; its color output goes to the mask, which drawOutline clears before use
        Framebuffer frame = renderContext.framebuffer();
        ensureMask(frame);

        Renderer r = renderContext.renderer();
        SavedState saved = new SavedState(r);

        rect.set(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1) + 1, Math.max(y0, y1) + 1);

        r.setFramebuffer(mask);
        r.setShader(flagsShader);
        r.setCamera(camera);
        r.setBlending(false);
        r.setClearing(false);
        flagBuffer.bind(0);
        r.begin();
        flagsShader.uploadVec4f("uRect", rect);
        r.drawSprites();
        r.end();
        flagBuffer.unbind();

        saved.restore(r, frame);
        return flagBuffer.read();
    }

    private void ensureMask(Framebuffer frame) {
        if (mask == null || mask.getWidth() != frame.getWidth() || mask.getHeight() != frame.getHeight()) {
            if (mask != null) mask.destroy();
            mask = new Framebuffer(frame.getWidth(), frame.getHeight(), GL_RGB8, GL_RGB, GL_UNSIGNED_BYTE, GL_NEAREST);
        }
    }

    /** Fills the id buffer with the world's sprites at the frame's size. False if the world has no scene yet. */
    private boolean renderIds(RenderContext renderContext, World2DLayer world) {
        Camera camera = world.camera();
        if (camera == null) return false;

        Framebuffer frame = renderContext.framebuffer();
        if (idBuffer == null || idBuffer.getWidth() != frame.getWidth() || idBuffer.getHeight() != frame.getHeight()) {
            if (idBuffer != null) idBuffer.destroy();
            idBuffer = new IdFramebuffer(frame.getWidth(), frame.getHeight());
        }

        Renderer r = renderContext.renderer();
        SavedState saved = new SavedState(r);

        r.setFramebuffer(idBuffer);
        r.setShader(pickingShader);
        r.setCamera(camera);
        r.setBlending(false);   // ids must not mix
        r.setClearing(true);
        r.setClearColor(CLEAR_ZERO);   // 0 = no object
        r.begin();
        r.drawSprites();
        r.end();

        saved.restore(r, frame);
        return true;
    }

    /** Draws an outline around every selected object onto the frame in renderContext. */
    public void drawOutline(RenderContext renderContext, World2DLayer world, List<GameObject> selected) {
        if (selected == null || selected.isEmpty()) return;
        Camera camera = world.camera();
        if (camera == null) return;

        int count = 0;
        if (ids.length < selected.size()) ids = new int[Math.max(selected.size(), ids.length * 2)];
        for (GameObject go : selected) {
            if (go == null || go.isDead()) continue;
            ids[count++] = go.getUid() + 1;
        }
        if (count == 0) return;
        selectedIds.set(ids, count);

        Framebuffer frame = renderContext.framebuffer();
        ensureMask(frame);

        Renderer r = renderContext.renderer();
        SavedState saved = new SavedState(r);

        // 1. mask: 1 where a selected sprite is (one pass, however many are selected)
        r.setFramebuffer(mask);
        r.setShader(maskShader);
        r.setCamera(camera);
        r.setBlending(false);
        r.setClearing(true);
        r.setClearColor(CLEAR_ZERO);
        r.begin();
        maskShader.uploadTexture("uSelected", Renderer.FIRST_EXTRA_TEXTURE_UNIT);
        r.drawSprites(selectedIds.getTexId());
        r.end();

        // 2. edges of the mask, blended over the frame
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

        saved.restore(r, frame);
    }

    public void setOutlineColor(Vector4f rgba) {
        outlineColor.set(rgba);
    }

    public void setOutlineThickness(int pixels) {
        outlineThickness = Math.max(1, pixels);
    }

    public void destroy() {
        if (idBuffer != null) {
            idBuffer.destroy();
            idBuffer = null;
        }
        if (mask != null) {
            mask.destroy();
            mask = null;
        }
        selectedIds.destroy();
        flagBuffer.destroy();
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
