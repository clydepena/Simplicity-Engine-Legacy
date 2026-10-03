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
        
        this.camera = new Camera(new Vector2f(0, 0));
        clearGameObjects();
    }

    /**
     * Destroys every object and stops the scene, keeping the scene initializer and the camera: add objects again,
     * then call startScene(). The editor uses it to put the world back after Play.
     */
    public void clearGameObjects() {
        this.physics2d = new Physics2D();
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

    /**
     * Recreates every physics body from the objects' current transforms and components, in a fresh physics world.
     * Bodies are otherwise only built when an object joins the scene, so call this after editing (e.g. when Play
     * starts), or edits made while physics wasn't stepping (moves, rotations, collider or rigidbody changes) are lost.
     */
    public void rebuildPhysics() {
        if (!sceneRunning) return;
        for (GameObject go : gameObjects) physics2d.destroyGameObject(go);
        this.physics2d = new Physics2D();
        for (GameObject go : gameObjects) physics2d.add(go);
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
        addGameObjectToScene(gameObjects.size(), go);
    }

    /** Adds at a position in the object list (clamped to it), e.g. to put a removed object back where it was. */
    public void addGameObjectToScene(int index, GameObject go) {
        gameObjects.add(Math.max(0, Math.min(index, gameObjects.size())), go);
        if (sceneRunning) {
            go.start();
            addSprite(go);
            this.physics2d.add(go);
        }
    }

    /**
     * Takes an object out of the world without destroying it, so it can be added back later (the editor's undo).
     * @return the index it had, or -1 if it wasn't in this world
     */
    public int removeGameObject(GameObject go) {
        int index = gameObjects.indexOf(go);
        if (index < 0) return -1;
        gameObjects.remove(index);
        removeSprite(go);
        this.physics2d.destroyGameObject(go);
        return index;
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
}