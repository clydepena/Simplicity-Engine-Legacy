package scenes;

import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.joml.Vector2f;
import org.joml.Vector4f;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import components.Component;
import components.ComponentDeserializer;
import components.EditorCamera;
import components.GizmoSystem;
import components.GrindLines;
import components.MouseControls;
import components.Sprite;
import components.SpriteRenderer;
import components.Spritesheet;
import components.StateMachine;
import simplicity.GameObject;
import simplicity.GameObjectDeserializer;
// import simplicity.OldWindow;
import util.AssetPool;
import util.Resources;


public class NewLevelEditorSceneInitializer implements NewSceneInitializer {
    private Spritesheet tileSprites, objectSprites;
    private String currentFile = null;
    private String sceneName = "";

    private GameObject levelEditorObj;

    public NewLevelEditorSceneInitializer() {

    }

    public NewLevelEditorSceneInitializer(String levelPath) {
        this.currentFile = levelPath;
        if (this.currentFile != null) {
            for (int i = this.currentFile.length() - 1; i >= 0; i--) {
                char c = this.currentFile.charAt(i);
                if (c == '/' || c == '\\') {
                    this.sceneName = this.currentFile.substring(i + 1);
                    break;
                }
            }
        }
    }

    @Override
    public void init(World2DLayer world) {
        // OldWindow.setWindowBgColor(new Vector4f(0.3f, 0.3f, 0.3f, 1.0f));

        // loadResources();
        
        // tileSprites = AssetPool.getSpritesheet("app/assets/images/TilesSpritesheet.png");
        tileSprites = AssetPool.getSpritesheet(Resources.SPRITESHEET_TILES);

        // objectSprites = AssetPool.getSpritesheet("app/assets/images/ObjectsSpritesheet.png");
        objectSprites = AssetPool.getSpritesheet(Resources.SPRITESHEET_OBJ);

        // editor tooling moves to the ImGui/editor layer; its components still depend on OldWindow
        // Spritesheet gizmos = AssetPool.getSpritesheetFromRes("editor_res/gizmos.png");

        // levelEditorObj = world.createGameObject("Level Editor");
        // levelEditorObj.setSerialize(false);
        // levelEditorObj.addComponent(new MouseControls());
        // levelEditorObj.addComponent(new GrindLines());
        // levelEditorObj.addComponent(new EditorCamera(world.camera()));

        // levelEditorObj.addComponent(new GizmoSystem(gizmos));
        // world.addGameObjectToScene(levelEditorObj);

        // OldWindow.getImGuiLayer().setEditorGameObject(levelEditorObj);

        if (currentFile != null) {
            load(currentFile, world);
        }
    }

    @Override
    public void loadResources(World2DLayer world) {

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
    }

    // deserialized textures are only filepaths; swap them for the uploaded ones in AssetPool
    private void refreshTextures(World2DLayer world) {
        for (GameObject go : world.getGameObjectList()) {
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

    private void load(String filepath, World2DLayer world) {
        Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Component.class, new ComponentDeserializer())
        .registerTypeAdapter(GameObject.class, new GameObjectDeserializer())
        .create();

        String inFile = "";
        try {
            inFile = filepath == null ? "" : new String(Files.readAllBytes(Paths.get(filepath)));
            if (filepath != null) {
                logger.Logger.info("Successfully loaded '" + filepath + "'");
            }
        } catch(IOException e) {
            logger.Logger.error("Unable to load '" + filepath + "'");
            e.printStackTrace();
        }
        if(!inFile.equals("")) {
            int maxGoId = -1;
            int maxCompId = -1;
            GameObject[] objs = gson.fromJson(inFile, GameObject[].class);
            for(int i = 0; i < objs.length; i++) {
                world.addGameObjectToScene(objs[i]);

                for(Component c : objs[i].getAllComponenets()) {
                    if(c.getUid() > maxCompId) {
                        maxCompId = c.getUid();
                    }
                }
                if(objs[i].getUid() > maxGoId) {
                    maxGoId = objs[i].getUid();
                }
            }

            maxGoId++;
            maxCompId++;
            GameObject.init(maxGoId);
            Component.init(maxCompId);

            // runs after the objects exist; loadResources() is called before init() so it sees an empty world
            refreshTextures(world);
        }
    }

    // @Override
    // public void imgui() {
    //     ImGui.begin("Editor Inspector");
    //     if (ImGui.button("Copy Style Colors")) {
    //         StringSelection selection = new StringSelection(getStyleCode());
    //         Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    //         clipboard.setContents(selection, selection);
    //     }
    //     levelEditorObj.imgui();
    //     ImGui.end();

    //     // ===============================================
    //     /*
    //     ImGui.begin("Blocks");
    //     ImVec2 windowPos = new ImVec2();
    //     ImGui.getWindowPos(windowPos);
    //     ImVec2 windowSize = new ImVec2();
    //     ImGui.getWindowSize(windowSize);
    //     ImVec2 itemSpacing = new ImVec2();
    //     ImGui.getStyle().getItemInnerSpacing(itemSpacing);

    //     float windowX2 = windowPos.x + windowSize.x;
    //     for(int i = 0; i < tileSprites.size(); i++) {
    //         Sprite sprite = tileSprites.getSprite(i);
    //         float spriteWidth = sprite.getWidth() * 1;
    //         float spriteHeight = sprite.getHeight() * 1;
    //         int id = sprite.getTexId();
    //         Vector2f[] texCoords = sprite.getTexCoords();

    //         ImGui.pushID(i);
    //         if(ImGui.imageButton(id, spriteWidth, spriteHeight, texCoords[2].x, texCoords[0].y, texCoords[0].x, texCoords[2].y)) {
    //             GameObject object = Prefabs.generateSpriteObject(sprite, Settings.GRID_WIDTH, Settings.GRID_HEIGHT);
                
    //             // attach to mouse cursor
    //             levelEditorObj.getComponent(MouseControls.class).pickupObject(object);
    //         }
    //         ImGui.popID();

    //         ImVec2 lastButtonPos = new ImVec2();
    //         ImGui.getItemRectMax(lastButtonPos);
    //         float lastButtonX2 = lastButtonPos.x;
    //         float nextButtonX2 = lastButtonX2 + itemSpacing.x + spriteWidth;
    //         if(i + 1 < tileSprites.size() && nextButtonX2 < windowX2) {
    //             ImGui.sameLine();
    //         }
    //     }

    //     ImGui.end();

    //     // ===============================================

    //     imgui2();
    //     */
    // }

    // private void imgui2() {
    //     ImGui.begin("Items");
    //     ImVec2 windowPos = new ImVec2();
    //     ImGui.getWindowPos(windowPos);
    //     ImVec2 windowSize = new ImVec2();
    //     ImGui.getWindowSize(windowSize);
    //     ImVec2 itemSpacing = new ImVec2();
    //     ImGui.getStyle().getItemInnerSpacing(itemSpacing);

    //     float windowX2 = windowPos.x + windowSize.x;
    //     for(int i = 0; i < objectSprites.size(); i++) {
    //         Sprite sprite = objectSprites.getSprite(i);
    //         float spriteWidth = sprite.getWidth() * 1;
    //         float spriteHeight = sprite.getHeight() * 1;
    //         int id = sprite.getTexId();
    //         Vector2f[] texCoords = sprite.getTexCoords();

    //         ImGui.pushID(i);
    //         if(ImGui.imageButton(id, spriteWidth, spriteHeight, texCoords[2].x, texCoords[0].y, texCoords[0].x, texCoords[2].y)) {
    //             GameObject object = Prefabs.generateSpriteObject(sprite, 0.3125f, 0.2344f);
                
    //             // attach to mouse cursor
    //             levelEditorObj.getComponent(MouseControls.class).pickupObject(object);
    //         }
    //         ImGui.popID();

    //         ImVec2 lastButtonPos = new ImVec2();
    //         ImGui.getItemRectMax(lastButtonPos);
    //         float lastButtonX2 = lastButtonPos.x;
    //         float nextButtonX2 = lastButtonX2 + itemSpacing.x + spriteWidth;
    //         if(i + 1 < objectSprites.size() && nextButtonX2 < windowX2) {
    //             ImGui.sameLine();
    //         }
    //     }

    //     ImGui.end();
    // }

    // @Override
    // public String getLevelPath() {
    //     return currentFile;
    // }

    // private String getStyleCode() {
    //     StringBuilder builder = new StringBuilder();
    //     // builder.append("ImVec4[] colors = new ImVec4[ImGuiCol.COUNT];\n");
    //     ImGuiStyle style = ImGui.getStyle();
    //     for (int i = 0; i < ImGuiCol.COUNT; i++) {
    //         ImVec4 c = new ImVec4(style.getColor(i));
    //         c.x = (float) (java.lang.Math.round(c.x * 100.0) / 100.0);
    //         c.y = (float) (java.lang.Math.round(c.y * 100.0) / 100.0);
    //         c.z = (float) (java.lang.Math.round(c.z * 100.0) / 100.0);
    //         c.w = (float) (java.lang.Math.round(c.w * 100.0) / 100.0);
    //         builder.append("colors[" + i + "]\t= new ImVec4(" + c.x + "f,\t" + c.y + "f,\t" + c.z + "f,\t" + c.w + "f);\n");
    //     }

    //     return builder.toString();
    // }
}
