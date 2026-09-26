package renderer;

/**
 * Something with geometry that the Renderer can draw: Renderer.draw(drawable) calls draw() inside a pass, with the
 * pass's target, camera, blending and shader already set. The drawable only issues its own draw calls.
 * <p>
 * The bound shader must expect the drawable's vertex layout; nothing checks this, so callers set a matching shader.
 */
public interface Drawable {

    /** Draws with the shader already in use. Called only by Renderer.draw, between begin() and end(). */
    void draw(Shader shader);
}
