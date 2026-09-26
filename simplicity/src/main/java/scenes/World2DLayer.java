package scenes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.joml.Vector2f;
import components.SpriteRenderer;
import observers.events.Event;
import physics2d.Physics2D;
import renderer.Framebuffer;
import renderer.Renderer;
import simplicity.Application;
import simplicity.Application.Layer;
import simplicity.Application.RenderContext;
import simplicity.Camera;
import simplicity.GameObject;
import simplicity.Transform;

public final class World2DLayer implements Layer {
    
    // private String currentFile, levelName;
    private Camera camera;
    private boolean sceneRunning;
    private List<GameObject> gameObjects = new ArrayList<>();
    private Physics2D physics2d;
    private SceneInitializer sceneInitializer;




    private Application context;
    private boolean isFrozen = false;
    private boolean isACtive = true;
    private boolean isHidden = false;

    public World2DLayer() {
        
    }

    private void addToRenderer(GameObject go) {
         SpriteRenderer spr = go.getComponent(SpriteRenderer.class);
        if (spr != null && context != null) context.renderer().addSprite(spr);
    }

    private void removeFromRenderer(GameObject go) {
         SpriteRenderer spr = go.getComponent(SpriteRenderer.class);
        if (spr != null && context != null) context.renderer().removeSprite(spr);
    }

    @Override
    public void onUpdate(float dt) {
        if (!sceneRunning) return;
        this.camera.adjustProjection();
        this.physics2d.update(dt);

        for(int i = 0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);
            go.update(dt);

            if(go.isDead()) {
                gameObjects.remove(i);
                removeFromRenderer(go);
                this.physics2d.destroyGameObject(go);
                i--;
            }
        }
    }

    @Override
    public void onRender(RenderContext renderContext) {
        if (!sceneRunning) return;
        Renderer r = renderContext.renderer();
        Framebuffer target = renderContext.framebuffer();
        camera.setAspectRatio((float) target.getWidth() / target.getHeight());
        r.setCamera(camera);
        r.begin();
        r.drawSprites();
        r.end();
    }

    @Override
    public void destroy() {
        for (GameObject go : gameObjects) {
            go.destroy();
        }
    }

    @Override
    public void onAttach(Application context) {
        this.context = context;
        if (sceneRunning) {
            for (GameObject go : gameObjects) {
                addToRenderer(go);
            }
        }
    }

    @Override
    public void onDetach() {
        this.context.renderer().removeAllSprites();
        this.context = null;
    }

    @Override
    public void onNotify(Event event) {
        
    }

    @Override
    public boolean isFrozen() {
        return isFrozen;
    }

    @Override
    public void setFrozen(boolean bool) {
        isFrozen = bool;
    }

    @Override
    public void setActive(boolean bool) {
        isACtive = bool;
    }

    @Override
    public boolean isActive() {
        return  isACtive;
    }

    @Override
    public void setHidden(boolean bool) {
        isHidden = bool;
    }

    @Override
    public boolean isHidden() {
        return isHidden;
    }

    public void setScene(SceneInitializer sceneInitializer) {
        this.sceneInitializer = sceneInitializer;
        // this.currentFile = sceneInitializer.getLevelPath() == null ?  currentFile : sceneInitializer.getLevelPath();
        // if (this.currentFile != null) {
        //     for (int i = this.currentFile.length() - 1; i >= 0; i--) {
        //         char c = this.currentFile.charAt(i);
        //         if (c == '/' || c == '\\') {
        //             this.levelName = this.currentFile.substring(i + 1);
        //             break;
        //         }
        //     }
        // }
        
        this.physics2d = new Physics2D();
        this.camera = new Camera(new Vector2f(0, 0));
        if (this.context != null) this.context.renderer().removeAllSprites();
        if (!gameObjects.isEmpty()) for (GameObject go : gameObjects) go.destroy();
        this.gameObjects = new ArrayList<>();
        this.sceneRunning = false;
    }

    public void initSceneResources() {
        checkSceneSet("initSceneResources()");
        this.sceneInitializer.loadResources(this);
        this.sceneInitializer.init(this);
    }

    public void startScene() {
        checkSceneSet("startScene()");
        if (sceneRunning) throw new IllegalStateException("startScene() called twice; call setScene() first");
        for(int i = 0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);
            go.start();
            addToRenderer(go);
            this.physics2d.add(go);
        }
        sceneRunning = true;
    }

    private void checkSceneSet(String caller) {
        if (sceneInitializer == null) throw new IllegalStateException(caller + " requires setScene() to be called first");
    }

    public void addGameObjectToScene(GameObject go) {
        if(!sceneRunning) {
            gameObjects.add(go);
        } else {
            gameObjects.add(go);
            go.start();
            addToRenderer(go);
            this.physics2d.add(go);
        }
    }

    public GameObject getGameObject(int uid) {
        Optional<GameObject> result = this.gameObjects.stream().filter(gameObject -> gameObject.getUid() == uid).findFirst();
        return result.orElse(null);
    }

    public GameObject createGameObject(String name) {
        GameObject go = new GameObject(name);
        go.addComponent(new Transform());
        go.transform = go.getComponent(Transform.class);
        return go;
    }
    
    public List<GameObject> getGameObjectList() {
        return this.gameObjects;
    }

    public Camera camera() {
        return this.camera;
    }

    // public void editorUpdate(float dt) {
    //     this.camera.adjustProjection();

    //     for(int i = 0; i < gameObjects.size(); i++) {
    //         GameObject go = gameObjects.get(i);
    //         go.editorUpdate(dt);

    //         if(go.isDead()) {
    //             gameObjects.remove(i);
    //             this.renderer.destroyGameObject(go);
    //             this.physics2d.destroyGameObject(go);
    //             i--;
    //         }
    //     }
    // }
    
    // public void save() {
        //     if (this.currentFile != null) {
            //         saveAs(this.currentFile);
    //     } else {
    //         String path = util.IOHelper.saveFile(OldWindow.get(), "level", "json");
    //         saveAs(path);
    //         OldWindow.changeScene(new LevelEditorSceneInitializer(path));
    //     }
    // }

    // public void saveAs(String filepath) {
    //     if (filepath != null) {
    //         Gson gson = new GsonBuilder()
    //         .setPrettyPrinting()
    //         .registerTypeAdapter(Component.class, new ComponentDeserializer())
    //         .registerTypeAdapter(GameObject.class, new GameObjectDeserializer())
    //         .create();
    //         try {
    //             FileWriter writer = new FileWriter(filepath);
    //             List<GameObject> objsToSerialize = new ArrayList<>();
    //             for(GameObject go : this.gameObjects) {
    //                 if(go.doSerialization()) {
    //                     objsToSerialize.add(go);
    //                 }
    //             }
    //             writer.write(gson.toJson(objsToSerialize));
    //             writer.close();
    //             logger.Logger.info("Successfully saved '" + levelName + "'");
    //         } catch(IOException e) {
    //             logger.Logger.error("Unable to save '" + levelName + "'");
    //             e.printStackTrace();
    //         }
    //     }
    // }

    // public void load() {
    //     load(this.currentFile);
    // }

    // private void load(String filepath) {
    //     Gson gson = new GsonBuilder()
    //     .setPrettyPrinting()
    //     .registerTypeAdapter(Component.class, new ComponentDeserializer())
    //     .registerTypeAdapter(GameObject.class, new GameObjectDeserializer())
    //     .create();

    //     String inFile = "";
    //     try {
    //         inFile = filepath == null ? "" : new String(Files.readAllBytes(Paths.get(filepath)));
    //         if (filepath != null) {
    //             logger.Logger.info("Successfully loaded '" + filepath + "'");
    //         }
    //     } catch(IOException e) {
    //         logger.Logger.error("Unable to load '" + filepath + "'");
    //         e.printStackTrace();
    //     }
    //     if(!inFile.equals("")) {
    //         int maxGoId = -1;
    //         int maxCompId = -1;
    //         GameObject[] objs = gson.fromJson(inFile, GameObject[].class);
    //         for(int i = 0; i < objs.length; i++) {
    //             addGameObjectToScene(objs[i]);

    //             for(Component c : objs[i].getAllComponenets()) {
    //                 if(c.getUid() > maxCompId) {
    //                     maxCompId = c.getUid();
    //                 }
    //             }
    //             if(objs[i].getUid() > maxGoId) {
    //                 maxGoId = objs[i].getUid();
    //             }
    //         }

    //         maxGoId++;
    //         maxCompId++;
    //         GameObject.init(maxGoId);
    //         Component.init(maxCompId);
    //     }
    // }
}