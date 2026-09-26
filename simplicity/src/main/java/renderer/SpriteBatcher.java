package renderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import components.SpriteRenderer;

/**
 * A set of sprites, batched for drawing: sprites with the same zIndex share RenderBatches (up to MAX_BATCH_SIZE
 * sprites and 7 textures each), and batches are drawn in zIndex order, one draw call each.
 * Whoever owns the content owns its batcher (e.g. a world's sprites); draw it with Renderer.draw(batcher).
 * <p>
 * Vertex layout: pos(2) | color(4) | texCoords(2) | texId(1) | entityId(1), entityId = the sprite's GameObject uid + 1.
 * <p>
 * Call sync() once per frame before drawing: it moves sprites whose zIndex changed and drops empty batches, so every
 * draw() only draws (plus uploading sprites that changed) and extra passes can't restructure anything.
 */
public class SpriteBatcher implements Drawable {

    private static final int MAX_BATCH_SIZE = 1000;

    private final List<RenderBatch> batches = new ArrayList<>();
    private final List<SpriteRenderer> moved = new ArrayList<>();

    /** Adds a sprite to a batch with room for its zIndex and texture, or a new batch. Needs a current GL context. */
    public void add(SpriteRenderer spr) {
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

    public boolean remove(SpriteRenderer spr) {
        for (RenderBatch b : batches) if (b.remove(spr)) return true;
        return false;
    }

    /** Removes every sprite and frees the batches. */
    public void clear() {
        while (!batches.isEmpty()) {
            batches.removeLast().destroy();
        }
    }

    /** Once per frame, outside a pass: re-adds sprites whose zIndex changed and frees batches left empty. */
    public void sync() {
        moved.clear();
        for (RenderBatch b : batches) b.takeMoved(moved);
        for (SpriteRenderer spr : moved) add(spr);
        moved.clear();

        batches.removeIf(b -> {
            if (!b.isEmpty()) return false;
            b.destroy();
            return true;
        });
    }

    @Override
    public void draw(Shader shader) {
        for (RenderBatch b : batches) b.render(shader);
    }

    public void destroy() {
        clear();
    }
}
