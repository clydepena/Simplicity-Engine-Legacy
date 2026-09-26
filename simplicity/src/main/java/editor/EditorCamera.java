package editor;

import org.joml.Vector2f;

import simplicity.Camera;

/**
 * The editor's own view of the world: pan, zoom toward a point, and reset. The world draws through it while editing
 * (World2DLayer.setViewCamera) so the game's camera is never moved by the editor.
 * <p>
 * The camera's origin is the bottom-left corner of the view, which spans position .. position + projectionSize * zoom;
 * a larger zoom shows more of the world.
 */
public class EditorCamera {

    private static final float MIN_ZOOM = 0.05f;
    private static final float MAX_ZOOM = 20.0f;
    private static final float ZOOM_STEP = 1.1f;          // per scroll notch
    private static final float RESET_DONE_DISTANCE = 0.01f;

    private final Camera camera = new Camera(new Vector2f());

    // reset: eases the position back to the origin (zoom is kept)
    private boolean resetting = false;
    private float resetLerp = 0.0f;

    public Camera camera() {
        return camera;
    }

    /** Starts from another camera's view (e.g. the game camera when a scene is loaded). */
    public void copyFrom(Camera other) {
        camera.position.set(other.position);
        camera.setZoom(other.getZoom());
        camera.adjustProjection();
        resetting = false;
    }

    /** Moves the view by a world-space offset (the content appears to move the opposite way). */
    public void pan(float worldDx, float worldDy) {
        camera.position.add(worldDx, worldDy);
        resetting = false;
    }

    /**
     * Zooms by scroll notches (positive = in) keeping the world point at (u, v) on the target in place
     * (0..1, bottom-left origin).
     */
    public void zoomAt(float u, float v, float notches) {
        Vector2f anchor = camera.viewportToWorld(u, v);

        float zoom = (float) (camera.getZoom() * Math.pow(ZOOM_STEP, -notches));
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
        camera.setZoom(zoom);
        camera.adjustProjection();

        // move so the anchor is back under (u, v)
        Vector2f after = camera.viewportToWorld(u, v);
        camera.position.add(anchor.x - after.x, anchor.y - after.y);
    }

    /** Eases the view back to the origin over the next frames (see update); the zoom stays as it is. */
    public void resetView() {
        resetting = true;
        resetLerp = 0.0f;
    }

    public boolean isResetting() {
        return resetting;
    }

    /** Advances the reset ease; call once per frame. */
    public void update(float dt) {
        if (!resetting) return;

        // the legacy editor camera's ease: lerp toward the origin with a factor that grows every frame
        camera.position.lerp(new Vector2f(), Math.min(1.0f, resetLerp));
        resetLerp += 0.001f + dt;

        if (camera.position.length() <= RESET_DONE_DISTANCE || resetLerp >= 1.0f) {
            camera.position.set(0.0f, 0.0f);
            resetting = false;
        }
    }
}
