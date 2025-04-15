package scenes;

import org.joml.*;
import components.*;
import imgui.*;
import imgui.flag.ImGuiCol;
import simplicity.*;
import util.*;

import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;

public class LevelEditorSceneInitializer extends SceneInitializer {

    private Spritesheet tileSprites, objectSprites;
    private String currentFile;

    private GameObject levelEditorObj;

    public LevelEditorSceneInitializer() {

    }

    public LevelEditorSceneInitializer(String levelPath) {
        this.currentFile = levelPath;
    }

    @Override
    public void init(Scene scene) {
        Window.setWindowBgColor(new Vector4f(0.3f, 0.3f, 0.3f, 1.0f));

        // loadResources();
        
        // tileSprites = AssetPool.getSpritesheet("app/assets/images/TilesSpritesheet.png");
        tileSprites = AssetPool.getSpritesheet(Resources.SPRITESHEET_TILES);

        // objectSprites = AssetPool.getSpritesheet("app/assets/images/ObjectsSpritesheet.png");
        objectSprites = AssetPool.getSpritesheet(Resources.SPRITESHEET_OBJ);

        Spritesheet gizmos = AssetPool.getSpritesheetFromRes("editor_res/gizmos.png");


        levelEditorObj = scene.createGameObject("Level Editor");
        levelEditorObj.setSerialize(false);
        levelEditorObj.addComponent(new MouseControls());
        levelEditorObj.addComponent(new GrindLines());
        levelEditorObj.addComponent(new EditorCamera(scene.camera()));

        levelEditorObj.addComponent(new GizmoSystem(gizmos));
        scene.addGameObjectToScene(levelEditorObj);

        Window.getImGuiLayer().setEditorGameObject(levelEditorObj);

        //TEST
    }

    @Override
    public void loadResources(Scene scene) {

        AssetPool.getShaderFromRes(Resources.SHADER_GAME_DEFAULT);

        // AssetPool.addSpritesheet(
        //     "app/assets/images/TilesSpritesheet.png",
        //     new Spritesheet(
        //         AssetPool.getTexture("app/assets/images/TilesSpritesheet.png"),
        //         32,
        //         32,
        //         48,
        //         0
        //     )
        // );

        AssetPool.addSpritesheetToRes(
            Resources.SPRITESHEET_TILES,
            new Spritesheet(
                AssetPool.getTextureFromRes(Resources.SPRITESHEET_TILES),
                32,
                32,
                48,
                0
            )
        );

        // AssetPool.addSpritesheet(
        //     "app/assets/images/ObjectsSpritesheet.png",
        //     new Spritesheet(
        //         AssetPool.getTexture("app/assets/images/ObjectsSpritesheet.png"),
        //         40,
        //         30,
        //         16,
        //         0
        //     )
        // );

        AssetPool.addSpritesheetToRes(
            Resources.SPRITESHEET_OBJ,
            new Spritesheet(
                AssetPool.getTextureFromRes(Resources.SPRITESHEET_OBJ),
                40,
                30,
                16,
                0
            )
        );

        AssetPool.addSpritesheetToRes(
            Resources.Editor.SPRITESHEET_GIZMO, 
            // new Spritesheet(
            //     AssetPool.getTexture("app/assets/editor_res/gizmos.png"), 
            //     24, 
            //     48, 
            //     3, 
            //     0
            // )
            new Spritesheet(
                AssetPool.getTextureFromRes(Resources.Editor.SPRITESHEET_GIZMO), 
                24, 
                48, 
                3, 
                0
            )
        );
        
        // AssetPool.getTexture("app/assets/images/TestPirate.png");

        for (GameObject go : scene.getGameObjectList()) {
            if(go.getComponent(SpriteRenderer.class) != null) {
                SpriteRenderer spr = go.getComponent(SpriteRenderer.class);
                if(spr.getTexture() != null) {
                    spr.setTexture(AssetPool.getTexture(spr.getTexture().getFilepath()));
                }
            }

            if(go.getComponent(StateMachine.class) != null) {
                StateMachine stateMachine = go.getComponent(StateMachine.class);
                stateMachine.refreshTextures();
            }
        }

    }

    @Override
    public void imgui() {
        ImGui.begin("Editor Inspector");
        if (ImGui.button("Copy Style Colors")) {
            StringSelection selection = new StringSelection(getStyleCode());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        }
        levelEditorObj.imgui();
        ImGui.end();

        // ===============================================
        /*
        ImGui.begin("Blocks");
        ImVec2 windowPos = new ImVec2();
        ImGui.getWindowPos(windowPos);
        ImVec2 windowSize = new ImVec2();
        ImGui.getWindowSize(windowSize);
        ImVec2 itemSpacing = new ImVec2();
        ImGui.getStyle().getItemInnerSpacing(itemSpacing);

        float windowX2 = windowPos.x + windowSize.x;
        for(int i = 0; i < tileSprites.size(); i++) {
            Sprite sprite = tileSprites.getSprite(i);
            float spriteWidth = sprite.getWidth() * 1;
            float spriteHeight = sprite.getHeight() * 1;
            int id = sprite.getTexId();
            Vector2f[] texCoords = sprite.getTexCoords();

            ImGui.pushID(i);
            if(ImGui.imageButton(id, spriteWidth, spriteHeight, texCoords[2].x, texCoords[0].y, texCoords[0].x, texCoords[2].y)) {
                GameObject object = Prefabs.generateSpriteObject(sprite, Settings.GRID_WIDTH, Settings.GRID_HEIGHT);
                
                // attach to mouse cursor
                levelEditorObj.getComponent(MouseControls.class).pickupObject(object);
            }
            ImGui.popID();

            ImVec2 lastButtonPos = new ImVec2();
            ImGui.getItemRectMax(lastButtonPos);
            float lastButtonX2 = lastButtonPos.x;
            float nextButtonX2 = lastButtonX2 + itemSpacing.x + spriteWidth;
            if(i + 1 < tileSprites.size() && nextButtonX2 < windowX2) {
                ImGui.sameLine();
            }
        }

        ImGui.end();

        // ===============================================

        imgui2();
        */
    }

    private void imgui2() {
        ImGui.begin("Items");
        ImVec2 windowPos = new ImVec2();
        ImGui.getWindowPos(windowPos);
        ImVec2 windowSize = new ImVec2();
        ImGui.getWindowSize(windowSize);
        ImVec2 itemSpacing = new ImVec2();
        ImGui.getStyle().getItemInnerSpacing(itemSpacing);

        float windowX2 = windowPos.x + windowSize.x;
        for(int i = 0; i < objectSprites.size(); i++) {
            Sprite sprite = objectSprites.getSprite(i);
            float spriteWidth = sprite.getWidth() * 1;
            float spriteHeight = sprite.getHeight() * 1;
            int id = sprite.getTexId();
            Vector2f[] texCoords = sprite.getTexCoords();

            ImGui.pushID(i);
            if(ImGui.imageButton(id, spriteWidth, spriteHeight, texCoords[2].x, texCoords[0].y, texCoords[0].x, texCoords[2].y)) {
                GameObject object = Prefabs.generateSpriteObject(sprite, 0.3125f, 0.2344f);
                
                // attach to mouse cursor
                levelEditorObj.getComponent(MouseControls.class).pickupObject(object);
            }
            ImGui.popID();

            ImVec2 lastButtonPos = new ImVec2();
            ImGui.getItemRectMax(lastButtonPos);
            float lastButtonX2 = lastButtonPos.x;
            float nextButtonX2 = lastButtonX2 + itemSpacing.x + spriteWidth;
            if(i + 1 < objectSprites.size() && nextButtonX2 < windowX2) {
                ImGui.sameLine();
            }
        }

        ImGui.end();
    }

    @Override
    public String getLevelPath() {
        return currentFile;
    }

    private String getStyleCode() {
        StringBuilder builder = new StringBuilder();
        // builder.append("ImVec4[] colors = new ImVec4[ImGuiCol.COUNT];\n");
        ImGuiStyle style = ImGui.getStyle();
        for (int i = 0; i < ImGuiCol.COUNT; i++) {
            ImVec4 c = new ImVec4(style.getColor(i));
            c.x = (float) (java.lang.Math.round(c.x * 100.0) / 100.0);
            c.y = (float) (java.lang.Math.round(c.y * 100.0) / 100.0);
            c.z = (float) (java.lang.Math.round(c.z * 100.0) / 100.0);
            c.w = (float) (java.lang.Math.round(c.w * 100.0) / 100.0);
            builder.append("colors[" + i + "]\t= new ImVec4(" + c.x + "f,\t" + c.y + "f,\t" + c.z + "f,\t" + c.w + "f);\n");
        }

        return builder.toString();
    }
}
