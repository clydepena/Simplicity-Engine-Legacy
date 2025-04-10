package components;

import static org.lwjgl.glfw.GLFW.*;

import com.google.gson.annotations.Expose;

import simplicity.KeyListener;
import simplicity.Window;

public class GizmoSystem extends Component {
    private Spritesheet gizmos;
    private int usingGizmo = 0;


    public GizmoSystem(Spritesheet gizmoSprites) {
        this.gizmos = gizmoSprites;
    }

    @Override
    public void start() {
        gameObject.addComponent(new TranslateTool(gizmos.getSprite(1), Window.getImGuiLayer().getPropertiesWindow()));
        gameObject.addComponent(new ScaleTool(gizmos.getSprite(2), Window.getImGuiLayer().getPropertiesWindow()));
    }

    @Override
    public void editorUpdate(float dt) {
        switch (usingGizmo) {
            case 0:
                gameObject.getComponent(TranslateTool.class).setUsing();
                gameObject.getComponent(ScaleTool.class).setNotUsing();
                break;
            case 1:
                gameObject.getComponent(TranslateTool.class).setNotUsing();
                gameObject.getComponent(ScaleTool.class).setUsing();
                break;
            default:
                break;
        }

        if(KeyListener.isKeyPressed(GLFW_KEY_E)) {
            usingGizmo = 0;
        } else if(KeyListener.isKeyPressed(GLFW_KEY_R)) {
            usingGizmo = 1;
        }
    }
    
}
