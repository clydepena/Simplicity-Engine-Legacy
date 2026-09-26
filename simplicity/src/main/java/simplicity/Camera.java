package simplicity;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Camera {
    
    private Matrix4f projectionMatrix, viewMatrix, inverseProjection, inverseView;
    public Vector2f position;

    private float projectionWidth = 6;
    private float projectionHeight = 3;
    private Vector2f projectionSize = new Vector2f(projectionWidth, projectionHeight);

    private float zoom = 1.0f;

    public Camera(Vector2f position) {
        this.position = position;
        this.projectionMatrix = new Matrix4f();
        this.viewMatrix = new Matrix4f();
        this.inverseProjection = new Matrix4f();
        this.inverseView = new Matrix4f();
        adjustProjection();
    }

    /**
     * Keeps the visible height fixed and widens/narrows the view to match the render target,
     * so world units stay square on any window size.
     */
    public void setAspectRatio(float aspectRatio) {
        float width = projectionHeight * aspectRatio;
        if (projectionSize.x == width && projectionSize.y == projectionHeight) return;
        projectionSize.set(width, projectionHeight);
        adjustProjection();
    }

    public void adjustProjection() {
        projectionMatrix.identity();
        projectionMatrix.ortho(0.0f, projectionSize.x * this.zoom, 0.0f, projectionSize.y * this.zoom, 0.0f, 100.0f);
        projectionMatrix.invert(inverseProjection);
    
    }

    public Matrix4f getViewMatrix() {
        Vector3f cameraFront = new Vector3f(0.0f, 0.0f, -1.0f);
        Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);

        this.viewMatrix.identity();
        this.viewMatrix = viewMatrix.lookAt(
            new Vector3f(position.x, position.y, 20.0f),
            cameraFront.add(position.x, position.y, 0.0f),
            cameraUp
            );
        this.viewMatrix.invert(inverseView);
        return this.viewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return this.projectionMatrix;
    }

    public Matrix4f getInverseProjection() {
        return this.inverseProjection;
    }

    public Matrix4f getInverseView() {
        return this.inverseView;
    }

    public Vector2f getProjectionSize() {
        return this.projectionSize;
    }

    public Vector2f getPosition() {
        return this.position;
    }

    /**
     * World position of a point on the render target, given as 0..1 with a bottom-left origin (u right, v up).
     * The view spans position .. position + projectionSize * zoom (see adjustProjection).
     */
    public Vector2f viewportToWorld(float u, float v) {
        return new Vector2f(position.x + u * projectionSize.x * zoom, position.y + v * projectionSize.y * zoom);
    }

    /** Inverse of viewportToWorld: where a world position lands on the target, 0..1 with a bottom-left origin. */
    public Vector2f worldToViewport(float x, float y) {
        return new Vector2f((x - position.x) / (projectionSize.x * zoom), (y - position.y) / (projectionSize.y * zoom));
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public float getZoom() {
        return this.zoom;
    }

    public void addZoom(float value) {
        this.zoom += value;
    }

}
