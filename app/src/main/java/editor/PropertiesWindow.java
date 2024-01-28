package editor;

import static org.lwjgl.glfw.GLFW.*;

import components.NonPickable;
import imgui.ImGui;
import physics2d.components.*;
import renderer.PickingTexture;
import scenes.Scene;
import simplicity.GameObject;
import simplicity.MouseListener;

public class PropertiesWindow {
    private GameObject activeGameObject = null;
    private PickingTexture pickingTexture;
    private float debounce = 0.2f;

    public PropertiesWindow(PickingTexture pickingTexture) {
        this.pickingTexture = pickingTexture;
    }

    public void update(float dt, Scene currentScene) {
        debounce -= dt;
        if (!MouseListener.isDragging() && MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT) && debounce < 0) {
            int x = (int) MouseListener.getScreenX();
            int y = (int) MouseListener.getScreenY();
            int uid = pickingTexture.readPixel(x, y);
            GameObject pickedObj = currentScene.getGameObject(uid);
            if (pickedObj != null && pickedObj.getComponent(NonPickable.class) == null) {
                activeGameObject = currentScene.getGameObject(uid);
            } else if(pickedObj == null && !MouseListener.isDragging()) {
                activeGameObject = null;
            }
            this.debounce = 0.2f;
        }
    }

    public void imgui() {
        if(activeGameObject != null) {
            ImGui.begin("Properties");

            if(ImGui.beginPopupContextWindow("ComponentAdder")) {
                if(ImGui.menuItem("Add Rigidbody")) {
                    if(activeGameObject.getComponent(Rigidbody2D.class) == null) {
                        activeGameObject.addComponent(new Rigidbody2D());
                    }
                }
                if(ImGui.menuItem("Add Box Collider")) {
                    if(
                        activeGameObject.getComponent(Box2DCollider.class) == null &&
                        activeGameObject.getComponent(CircleCollider.class) == null
                        ) {
                        activeGameObject.addComponent(new Box2DCollider());
                    }
                }
                if(ImGui.menuItem("Add Circle Collider")) {
                    if(
                        activeGameObject.getComponent(CircleCollider.class) == null && 
                        activeGameObject.getComponent(Box2DCollider.class) == null
                        ) {
                        activeGameObject.addComponent(new CircleCollider());
                    }
                }
                ImGui.endPopup();
            }

            activeGameObject.imgui();
            ImGui.end();
        }
    }

    public GameObject getActivGameObject() {
        return this.activeGameObject;
    }

    public void setActiveGameObject(GameObject go) {
        this.activeGameObject = go;
    }
}
