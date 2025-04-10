package physics2d.components;

import org.jbox2d.dynamics.Body;
import org.joml.Vector2f;

import components.Component;
import physics2d.enums.BodyType;

public class Rigidbody2D extends Component {
    private Vector2f velocity = new Vector2f();
    private float angularDamping = 0.8f;
    private float linearDamping = 0.5f;
    private float mass = 0;
    private BodyType bodyType = BodyType.Dynamic;

    private boolean fixedRotation = false;
    private boolean continuousCollision = true;

    private transient Body rawBody = null;

    @Override public void update(float dt) {
        if(rawBody != null) {
            this.gameObject.transform.position.set(rawBody.getPosition().x, rawBody.getPosition().y);
            this.gameObject.transform.rotation = (float) Math.toDegrees(rawBody.getAngle());
        }
    }

    public Vector2f getVelocity() {
        return this.velocity;
    }

    public void setVelocity(Vector2f velocity) {
        this.velocity = velocity;
    }

    public float getAngularDamping() {
        return this.angularDamping;
    }

    public void setAngularDamping(float angularDamping) {
        this.angularDamping = angularDamping;
    }

    public float getLinearDamping() {
        return this.linearDamping;
    }

    public void setLinearDamping(float linearDamping) {
        this.linearDamping = linearDamping;
    }

    public float getMass() {
        return this.mass;
    }

    public void setMass(float mass) {
        this.mass = mass;
    }

    public BodyType getBodyType() {
        return this.bodyType;
    }

    public void setBodyType(BodyType bodyType) {
        this.bodyType = bodyType;
    }

    public boolean isFixedRotation() {
        return this.fixedRotation;
    }

    public boolean getFixedRotation() {
        return this.fixedRotation;
    }

    public void setFixedRotation(boolean fixedRotation) {
        this.fixedRotation = fixedRotation;
    }

    public boolean isContinuousCollision() {
        return this.continuousCollision;
    }

    public boolean getContinuousCollision() {
        return this.continuousCollision;
    }

    public void setContinuousCollision(boolean continuousCollision) {
        this.continuousCollision = continuousCollision;
    }

    public Body getRawBody() {
        return this.rawBody;
    }

    public void setRawBody(Body rawBody) {
        this.rawBody = rawBody;
    }
}
