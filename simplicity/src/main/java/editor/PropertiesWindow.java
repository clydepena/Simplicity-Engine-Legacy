package editor;

import static org.lwjgl.glfw.GLFW.*;

import org.joml.Vector2f;
import org.joml.Vector4f;

import components.NonPickable;
import components.Sprite;
import components.SpriteRenderer;
import imgui.ImGui;
import imgui.ImGuiStyle;
import physics2d.components.*;
import renderer.PickingTexture;
import scenes.Scene;
import simplicity.GameObject;
import simplicity.MouseListener;

public class PropertiesWindow extends ImGuiInterface {
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
        ImGui.begin("Properties");
        updateCalc();
        if(activeGameObject != null) {
            preview();
            ImGui.separator();
            components();
        }
        ImGui.end();
    }

    private void preview() {
        SpriteRenderer spriteRenderer = activeGameObject.getComponent(SpriteRenderer.class);
        if (spriteRenderer != null) {
            float limit = 200;
            Sprite sprite = spriteRenderer.getSprite();
            Vector4f c = spriteRenderer.getColor();
            int id = sprite.getTexId();
            Vector2f[] texCoords = sprite.getTexCoords();

            ImGuiStyle style = ImGui.getStyle();
            String label = sprite.getWidth() + " x " + sprite.getHeight();
            float size = ImGui.calcTextSize(label).x + style.getFramePaddingX() * 2.0f;
            float avail = ImGui.getContentRegionAvail().x;
            float off = (avail - size) * 0.5f;
            if (off > 0.0f) {
                ImGui.setCursorPosX(ImGui.getCursorPosX() + off);
            }
            ImGui.text(label);

            float spriteWidth = winSize.x * 0.5f;
            float spriteHeight = sprite.getHeight() * (spriteWidth / sprite.getWidth());

            if (sprite.getHeight() > limit || spriteHeight > limit) {
                spriteHeight = limit;
                spriteWidth = sprite.getWidth() * (limit / sprite.getHeight());
            }

            ImGui.setCursorPosX(((winSize.x - spriteWidth) / 2));
            ImGui.image(id, spriteWidth, spriteHeight, texCoords[2].x, texCoords[0].y, texCoords[0].x, texCoords[2].y, c.x, c.y, c.z, c.w);
        }
        ImGui.text("\tSelected: " + activeGameObject.name);
        ImGui.text("\tUID: " + activeGameObject.getUid());
    }

    private void components() {
        ImGui.beginChild("##Component Editor");
        ImGui.text("\tComponents");
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
        ImGui.endChild();
    }

    public GameObject getActivGameObject() {
        return this.activeGameObject;
    }

    public void setActiveGameObject(GameObject go) {
        this.activeGameObject = go;
    }

    @Override
    public void destroy() {
        
    }
}
