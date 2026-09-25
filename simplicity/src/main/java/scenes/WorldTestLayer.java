package scenes;

import observers.events.Event;
import simplicity.Application;
import simplicity.Application.Layer;
import simplicity.Application.RenderContext;

/**
 * Injects a level into the World2DLayer below it. Must be pushed after the world layer:
 * layer commands are applied in order, so the world is already on the stack in onAttach.
 */
public class WorldTestLayer implements Layer {

    private final String levelPath;
    private World2DLayer world;

    private boolean isFrozen = false;
    private boolean isActive = true;
    private boolean isHidden = false;

    public WorldTestLayer(String levelPath) {
        this.levelPath = levelPath;
    }

    @Override
    public void onAttach(Application context) {
        world = context.getLayer(World2DLayer.class);
        if (world == null) {
            throw new IllegalStateException("WorldTestLayer requires a World2DLayer to be pushed first");
        }

        world.setScene(new NewLevelEditorSceneInitializer(levelPath));
        world.initSceneResources();
        world.startScene();

        System.out.println("Injected '" + levelPath + "' into the world: "
            + world.getGameObjectList().size() + " game objects");
    }

    @Override
    public void onDetach() {
        world = null;
    }

    @Override
    public void onUpdate(float dt) {

    }

    @Override
    public void onRender(RenderContext renderContext) {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void onNotify(Event event) {

    }

    @Override
    public void setFrozen(boolean bool) {
        isFrozen = bool;
    }

    @Override
    public boolean isFrozen() {
        return isFrozen;
    }

    @Override
    public void setActive(boolean bool) {
        isActive = bool;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void setHidden(boolean bool) {
        isHidden = bool;
    }

    @Override
    public boolean isHidden() {
        return isHidden;
    }
}
