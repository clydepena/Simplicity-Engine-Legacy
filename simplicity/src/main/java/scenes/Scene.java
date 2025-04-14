package scenes;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.joml.Vector2f;
import com.google.gson.*;
import components.*;
import physics2d.Physics2D;
import renderer.*;
import simplicity.*;

public class Scene {
    
    private Renderer renderer = new Renderer();
    private Camera camera;
    private boolean isRunning;
    private List<GameObject> gameObjects = new ArrayList<>();
    private Physics2D physics2d;
    private SceneInitializer sceneInitializer;

    private static String currentFile, levelName;

    public Scene(SceneInitializer sceneInitializer) {
        this.sceneInitializer = sceneInitializer;
        Scene.currentFile = sceneInitializer.getLevelPath() == null ?  currentFile : sceneInitializer.getLevelPath();
        //"app/saves/level.json"
        if (Scene.currentFile != null) {
            for (int i = Scene.currentFile.length() - 1; i >= 0; i--) {
                char c = Scene.currentFile.charAt(i);
                if (c == '/' || c == '\\') {
                    Scene.levelName = Scene.currentFile.substring(i + 1);
                    break;
                }
            }
        }
        
        this.physics2d = new Physics2D();
        this.renderer = new Renderer();
        this.gameObjects = new ArrayList<>();
        this.isRunning = false;
    }

    public void init() {
        this.camera = new Camera(new Vector2f(0, 0));
        this.sceneInitializer.loadResources(this);
        this.sceneInitializer.init(this);
    }

    public void start() {
        for(int i = 0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);
            go.start();
            this.renderer.add(go);
            this.physics2d.add(go);
        }
        isRunning = true;
    }

    public void addGameObjectToScene(GameObject go) {
        if(!isRunning) {
            gameObjects.add(go);
        } else {
            gameObjects.add(go);
            go.start();
            this.renderer.add(go);
            this.physics2d.add(go);
        }
    }

    public GameObject getGameObject(int uid) {
        Optional<GameObject> result = this.gameObjects.stream().filter(gameObject -> gameObject.getUid() == uid).findFirst();
        
        return result.orElse(null);
    }

    public void editorUpdate(float dt) {
        this.camera.adjustProjection();

        for(int i = 0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);
            go.editorUpdate(dt);

            if(go.isDead()) {
                gameObjects.remove(i);
                this.renderer.destroyGameObject(go);
                this.physics2d.destroyGameObject(go);
                i--;
            }
        }
    }


    public void update(float dt) {
        this.camera.adjustProjection();
        this.physics2d.update(dt);

        for(int i = 0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);
            go.update(dt);

            if(go.isDead()) {
                gameObjects.remove(i);
                this.renderer.destroyGameObject(go);
                this.physics2d.destroyGameObject(go);
                i--;
            }
        }
    }

    public void render() {
        this.renderer.render();
    }

    public Camera camera() {
        return this.camera;
    }

    public void imgui() {
        this.sceneInitializer.imgui();
    }

    public GameObject createGameObject(String name) {
        GameObject go = new GameObject(name);
        go.addComponent(new Transform());
        go.transform = go.getComponent(Transform.class);
        return go;
    }

    public void save() {
        if (Scene.currentFile != null) {
            saveAs(Scene.currentFile);
        } else {
            String path = util.IOHelper.saveFile(Window.get(), "level", "json");
            saveAs(path);
            Window.changeScene(new LevelEditorSceneInitializer(path));
        }
    }

    public void saveAs(String filepath) {
        if (filepath != null) {
            Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Component.class, new ComponentDeserializer())
            .registerTypeAdapter(GameObject.class, new GameObjectDeserializer())
            .create();
            try {
                FileWriter writer = new FileWriter(filepath);
                List<GameObject> objsToSerialize = new ArrayList<>();
                for(GameObject go : this.gameObjects) {
                    if(go.doSerialization()) {
                        objsToSerialize.add(go);
                    }
                }
                writer.write(gson.toJson(objsToSerialize));
                writer.close();
                logger.Logger.info("Successfully saved '" + levelName + "'");
            } catch(IOException e) {
                logger.Logger.error("Unable to save '" + levelName + "'");
                e.printStackTrace();
            }
        }
    }

    public void load() {
        load(Scene.currentFile);
    }

    private void load(String filepath) {
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
                addGameObjectToScene(objs[i]);

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

    public List<GameObject> getGameObjectList() {
        return this.gameObjects;
    }

    public void destroy() {
        for (GameObject go : gameObjects) {
            go.destroy();
        }
    }

    public String getFilename() {
        return Scene.levelName;
    }
}
