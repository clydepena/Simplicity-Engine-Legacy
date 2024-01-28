package physics2d.components;

public class CircleCollider extends Collider {
    private float radius = 1.0f;

    public float getRadius() {
        return this.radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }
    
}
