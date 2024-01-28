package scenes;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.joml.Vector2f;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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

    public Scene(SceneInitializer sceneInitializer) {
        this.sceneInitializer = sceneInitializer;
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
        Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Component.class, new ComponentDeserializer())
        .registerTypeAdapter(GameObject.class, new GameObjectDeserializer())
        .create();

        try {
            FileWriter writer = new FileWriter("app/saves/level.json");
            List<GameObject> objsToSerialize = new ArrayList<>();
            for(GameObject go : this.gameObjects) {
                if(go.doSerialization()) {
                    objsToSerialize.add(go);
                }
            }
            writer.write(gson.toJson(objsToSerialize));
            writer.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void laod() {
        Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Component.class, new ComponentDeserializer())
        .registerTypeAdapter(GameObject.class, new GameObjectDeserializer())
        .create();

        String inFile = "";
        try {
            inFile = new String(Files.readAllBytes(Paths.get("app/saves/level.json")));
        } catch(IOException e) {
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
}
