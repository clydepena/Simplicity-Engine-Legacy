package components;

import static org.lwjgl.glfw.GLFW.*;

import org.joml.Vector2f;

import simplicity.Camera;
import simplicity.KeyListener;
import simplicity.MouseListener;

public class EditorCamera extends Component {
    private float dragDeBounce = 0.032f;
    private Camera lvlEditorCamera;
    private Vector2f clickOrigin;
    private float dragSensitivity = 30.0f;
    private float scrollSensitivity = 0.1f;
    private boolean reset = false;
    private float lerpTime = 0.0f;

    public EditorCamera(Camera lvlEditorCamera) {
        this.lvlEditorCamera = lvlEditorCamera;
        this.clickOrigin = new Vector2f();
    }

    @Override
    public void editorUpdate(float dt) {
        if(MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_MIDDLE) && dragDeBounce > 0) {
            this.clickOrigin = MouseListener.getWorld();
            dragDeBounce -= dt;
            return;
        } else if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_MIDDLE)) {
            Vector2f mousePos = MouseListener.getWorld();
            Vector2f delta = new Vector2f(mousePos).sub(this.clickOrigin);
            lvlEditorCamera.position.sub(delta.mul(dt).mul(dragSensitivity));
            this.clickOrigin.lerp(mousePos, dt);
        }

        if(dragDeBounce <= 0.0f && !MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_MIDDLE)) {
            dragDeBounce = 0.1f;
        }

        if(MouseListener.getScrollY() != 0.0f) {
            float addVal = (float) Math.pow(Math.abs(MouseListener.getScrollY() * scrollSensitivity), 1 / lvlEditorCamera.getZoom());
            addVal *= Math.signum(MouseListener.getScrollY());
            lvlEditorCamera.addZoom(-addVal);
        }

        if(KeyListener.isKeyPressed(GLFW_KEY_KP_DECIMAL)) {
            reset = true;
        }

        if(reset) {
            lvlEditorCamera.position.lerp(new Vector2f(), lerpTime);
            lvlEditorCamera.setZoom(this.lvlEditorCamera.getZoom() + ((1.0f - this.lvlEditorCamera.getZoom()) * lerpTime));
            this.lerpTime += 0.001f + dt;
            if(Math.abs(lvlEditorCamera.position.x) <= 5.0 && Math.abs(lvlEditorCamera.position.y) <= 5.0) {
                this.lerpTime = 0.0f;
                lvlEditorCamera.position.set(0.0f, 0.0f);
                this.lvlEditorCamera.setZoom(1.0f);
                reset = false;
            }
        }

    }


    
}
