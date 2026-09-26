package editor;

import static org.lwjgl.opengl.GL33.GL_NEAREST;
import static org.lwjgl.opengl.GL33.GL_RGB;
import static org.lwjgl.opengl.GL33.GL_RGB8;
import static org.lwjgl.opengl.GL33.GL_UNSIGNED_BYTE;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.joml.Vector4f;

import renderer.Framebuffer;
import renderer.IdFlagBuffer;
import renderer.IdFramebuffer;
import renderer.IdSetTexture;
import renderer.Renderer;
import renderer.Shader;
import renderer.SpriteBatcher;
import simplicity.Camera;
import util.AssetPool;
import util.Resources;

/**
 * Picks sprites under a pixel or inside a rectangle, and outlines some of them, by uid.
 * Works on a given SpriteBatcher (every vertex carries entity id = uid + 1), seen through a given
 * camera into a given target, so any view of those sprites can use it. It knows nothing about GameObjects or worlds:
 * callers map uids to objects. Every pass restores the renderer state it changed.
 * <p>
 * Pixel coordinates are in the target's pixels, bottom-left origin. Buffers are kept at the target's size.
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

    // created lazily at the target's size
    private IdFramebuffer idBuffer;
    private Framebuffer mask;

    // entity ids to outline, for the mask shader; re-uploaded only when they change
    private final IdSetTexture outlinedIds = new IdSetTexture();
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
     * Uid of the topmost sprite at pixel (x, y) of target, seen through camera, or -1 for none.
     * Renders the id buffer when called, so call it on clicks rather than every frame.
     */
    public int pick(Renderer renderer, Framebuffer target, Camera camera, SpriteBatcher sprites, int x, int y) {
        renderIds(renderer, target, camera, sprites);
        return idBuffer.readPixel(x, y);
    }

    /**
     * Uids of the sprites in the rectangle between pixels (x0, y0) and (x1, y1) of target, inclusive, corners in any order.
     * <ul>
     * <li>includeHidden: false = only sprites with a visible pixel inside; true = also sprites fully covered by others.</li>
     * <li>fullyInside: false = any pixel inside is enough (partial); true = the sprite's whole shape, covered parts
     *     included, must be inside (and on the target).</li>
     * </ul>
     * maxUid is the largest uid that can appear (sizes the flag buffer for the non-default modes).
     * Only sprite pixels that pass the alpha cutout count. Renders when called, so call it once (e.g. when a drag
     * ends), not every frame. The default (false, false) only needs the id buffer; the other modes add one flag pass.
     */
    public Set<Integer> pickRect(Renderer renderer, Framebuffer target, Camera camera, SpriteBatcher sprites,
                                 int x0, int y0, int x1, int y1,
                                 boolean includeHidden, boolean fullyInside, int maxUid) {
        Set<Integer> visible = null;
        if (!includeHidden) {
            renderIds(renderer, target, camera, sprites);
            visible = idBuffer.readRect(x0, y0, x1, y1);
            if (!fullyInside) return visible;
        }

        int[] flags = renderFlags(renderer, target, camera, sprites, x0, y0, x1, y1, maxUid);

        Set<Integer> found = new LinkedHashSet<>();
        for (int entity = 1; entity < flags.length; entity++) {
            int f = flags[entity];
            if ((f & FLAG_INSIDE) == 0) continue;
            if (fullyInside && (f & FLAG_OUTSIDE) != 0) continue;

            int uid = entity - 1;
            if (visible != null && !visible.contains(uid)) continue;
            found.add(uid);
        }
        return found;
    }

    /** Draws an outline around the sprites with the given uids onto target, seen through camera. Negative uids are ignored. */
    public void drawOutline(Renderer renderer, Framebuffer target, Camera camera, SpriteBatcher sprites,
                            Collection<Integer> uids) {
        if (uids == null || uids.isEmpty()) return;

        if (ids.length < uids.size()) ids = new int[Math.max(uids.size(), ids.length * 2)];
        int count = 0;
        for (Integer uid : uids) {
            if (uid != null && uid >= 0) ids[count++] = uid + 1;
        }
        if (count == 0) return;
        outlinedIds.set(ids, count);

        ensureMask(target);
        SavedState saved = new SavedState(renderer);

        // 1. mask: 1 where an outlined sprite is (one pass, however many there are)
        renderer.setFramebuffer(mask);
        renderer.setShader(maskShader);
        renderer.setCamera(camera);
        renderer.setBlending(false);
        renderer.setClearing(true);
        renderer.setClearColor(CLEAR_ZERO);
        renderer.begin();
        maskShader.uploadTexture("uSelected", Renderer.FIRST_EXTRA_TEXTURE_UNIT);
        renderer.draw(sprites, outlinedIds.getTexId());
        renderer.end();

        // 2. edges of the mask, blended over the target
        renderer.setFramebuffer(target);
        renderer.setShader(outlineShader);
        renderer.setBlending(true);
        renderer.setClearing(false);
        renderer.begin();
        outlineShader.uploadTexture("uMask", 0);
        outlineShader.uploadVec4f("uColor", outlineColor);
        outlineShader.uploadInt("uThickness", outlineThickness);
        renderer.drawFullscreen(mask.getTexId());
        renderer.end();

        saved.restore(renderer);
    }

    /** Fills the id buffer with every sprite, at target's size, seen through camera. */
    private void renderIds(Renderer renderer, Framebuffer target, Camera camera, SpriteBatcher sprites) {
        if (idBuffer == null || idBuffer.getWidth() != target.getWidth() || idBuffer.getHeight() != target.getHeight()) {
            if (idBuffer != null) idBuffer.destroy();
            idBuffer = new IdFramebuffer(target.getWidth(), target.getHeight());
        }

        SavedState saved = new SavedState(renderer);

        renderer.setFramebuffer(idBuffer);
        renderer.setShader(pickingShader);
        renderer.setCamera(camera);
        renderer.setBlending(false);   // ids must not mix
        renderer.setClearing(true);
        renderer.setClearColor(CLEAR_ZERO);   // 0 = no sprite
        renderer.begin();
        renderer.draw(sprites);
        renderer.end();

        saved.restore(renderer);
    }

    /** Flag pass: draws every sprite, covered or not, and returns per-entity-id flags (FLAG_INSIDE / FLAG_OUTSIDE). */
    private int[] renderFlags(Renderer renderer, Framebuffer target, Camera camera, SpriteBatcher sprites,
                              int x0, int y0, int x1, int y1, int maxUid) {
        flagBuffer.clear(Math.max(0, maxUid) + 2);   // entity ids go up to maxUid + 1

        // the pass only writes flags; its color output goes to the mask, which drawOutline clears before use
        ensureMask(target);
        SavedState saved = new SavedState(renderer);

        rect.set(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1) + 1, Math.max(y0, y1) + 1);

        renderer.setFramebuffer(mask);
        renderer.setShader(flagsShader);
        renderer.setCamera(camera);
        renderer.setBlending(false);
        renderer.setClearing(false);
        flagBuffer.bind(0);
        renderer.begin();
        flagsShader.uploadVec4f("uRect", rect);
        renderer.draw(sprites);
        renderer.end();
        flagBuffer.unbind();

        saved.restore(renderer);
        return flagBuffer.read();
    }

    private void ensureMask(Framebuffer target) {
        if (mask == null || mask.getWidth() != target.getWidth() || mask.getHeight() != target.getHeight()) {
            if (mask != null) mask.destroy();
            mask = new Framebuffer(target.getWidth(), target.getHeight(), GL_RGB8, GL_RGB, GL_UNSIGNED_BYTE, GL_NEAREST);
        }
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
        outlinedIds.destroy();
        flagBuffer.destroy();
    }

    /** Renderer state the passes change; restored so later layers see the renderer as they left it. */
    private static final class SavedState {
        private final Framebuffer framebuffer;
        private final Shader shader;
        private final Camera camera;
        private final boolean blending;
        private final boolean clearing;
        private final Vector4f clearColor;

        SavedState(Renderer r) {
            framebuffer = r.getFramebuffer();
            shader = r.getShader();
            camera = r.getCamera();
            blending = r.isBlending();
            clearing = r.isClearing();
            clearColor = r.getClearColor();
        }

        void restore(Renderer r) {
            r.setFramebuffer(framebuffer);
            r.setShader(shader);
            r.setCamera(camera);
            r.setBlending(blending);
            r.setClearing(clearing);
            r.setClearColor(clearColor);
        }
    }
}
