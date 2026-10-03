package scenes;

import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.joml.Vector2f;
import org.joml.Vector4f;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import asset.AssetPoolHandler;
import components.Component;
import components.Sprite;
import components.Spritesheet;
import simplicity.GameObject;
import simplicity.GameObjectGson;
// import simplicity.OldWindow;
import util.Resources;


public class LevelEditorSceneInitializer implements SceneInitializer {
    private Spritesheet tileSprites, objectSprites;
    private String currentFile = null;
    private String sceneName = "";

    private GameObject levelEditorObj;

    public LevelEditorSceneInitializer() {

    }

    public LevelEditorSceneInitializer(String levelPath) {
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
        
        // the .sheet files next to their images (engine resources); each loads its texture as a dependency
        AssetPoolHandler assets = AssetPoolHandler.GetInstance();
        tileSprites = assets.resolve("engine:" + Resources.SPRITESHEET_TILES_SHEET, Spritesheet.class);
        objectSprites = assets.resolve("engine:" + Resources.SPRITESHEET_OBJ_SHEET, Spritesheet.class);

        // editor tooling moves to the ImGui/editor layer; its components still depend on OldWindow
        // Spritesheet gizmos = assets.resolve("engine:" + Resources.Editor.SPRITESHEET_GIZMO_SHEET, Spritesheet.class);

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
        // nothing to preload: the shaders are loaded by the renderer, and sheets and textures from the engine pool
        // when init() and the level ask for them. The sheets that used to be built here are now .sheet files:
        // images/TilesSpritesheet.sheet, images/ObjectsSpritesheet.sheet, images/editor/gizmos.sheet
    }

    private void load(String filepath, World2DLayer world) {
        // textures are read as Asset handles (and loaded) by the asset adapter: no fix-up after loading
        Gson gson = GameObjectGson.GSON;

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
        // an empty world: an empty file, or "{}" as early project creation wrote it (now "[]")
        String trimmed = inFile.trim();
        if (trimmed.isEmpty() || trimmed.equals("{}")) return;

        if(!inFile.equals("")) {
            int maxGoId = -1;
            int maxCompId = -1;
            GameObject[] objs;
            try {
                objs = gson.fromJson(inFile, GameObject[].class);
            } catch (JsonParseException e) {
                // a broken world file: log it and open an empty world rather than crashing the editor
                logger.Logger.error("Can't read the world '" + filepath + "': " + e.getMessage());
                return;
            }
            if (objs == null) return;
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
