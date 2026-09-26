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
import renderer.SpriteBatcher;
import simplicity.Application;
import simplicity.Application.Layer;
import simplicity.Application.RenderContext;
import simplicity.Camera;
import simplicity.GameObject;
import simplicity.Transform;

public final class World2DLayer implements Layer {
    
    // private String currentFile, levelName;
    private Camera camera;
    private Camera viewCamera = null;   // when set (e.g. by the editor), drawn through instead of camera
    private boolean sceneRunning;
    private List<GameObject> gameObjects = new ArrayList<>();
    private Physics2D physics2d;
    private SceneInitializer sceneInitializer;
    private final SpriteBatcher sprites = new SpriteBatcher();   // this world's sprites, drawn in onRender


    private Application context;
    private boolean isFrozen = false;
    private boolean isACtive = true;
    private boolean isHidden = false;

    public World2DLayer() {
        
    }

    private void addSprite(GameObject go) {
         SpriteRenderer spr = go.getComponent(SpriteRenderer.class);
        if (spr != null) sprites.add(spr);
    }

    private void removeSprite(GameObject go) {
         SpriteRenderer spr = go.getComponent(SpriteRenderer.class);
        if (spr != null) sprites.remove(spr);
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
                removeSprite(go);
                this.physics2d.destroyGameObject(go);
                i--;
            }
        }
    }

    public void onEditorUpdate(float dt) {
        if (!sceneRunning) return;
        this.camera.adjustProjection();

        for(int i = 0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);
            go.editorUpdate(dt);

            if(go.isDead()) {
                gameObjects.remove(i);
                removeSprite(go);
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
        Camera view = renderCamera();
        view.setAspectRatio((float) target.getWidth() / target.getHeight());
        sprites.sync();   // here rather than in an update, so it also runs while the world is frozen
        r.setCamera(view);
        r.begin();
        r.draw(sprites);
        r.end();
    }

    @Override
    public void destroy() {
        for (GameObject go : gameObjects) {
            go.destroy();
        }
        sprites.destroy();
    }

    @Override
    public void onAttach(Application context) {
        this.context = context;
    }

    @Override
    public void onDetach() {
        // the sprites stay in this world's batcher; a detached world just isn't drawn
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
        sprites.clear();
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
            addSprite(go);
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
            addSprite(go);
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

    /** This world's sprites, for passes that draw or pick them (e.g. the editor's selection). */
    public SpriteBatcher sprites() {
        return this.sprites;
    }

    /** Draws through this camera instead of the game camera; null goes back to the game camera. */
    public void setViewCamera(Camera viewCamera) {
        this.viewCamera = viewCamera;
    }

    /** The camera the world is drawn through this frame: the view camera if set, else the game camera. */
    public Camera renderCamera() {
        return viewCamera != null ? viewCamera : camera;
    }

    /** The game camera (the scene's own). */
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