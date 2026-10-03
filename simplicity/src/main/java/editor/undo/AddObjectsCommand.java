package editor.undo;

import java.util.ArrayList;
import java.util.List;

import editor.EditorSelection;
import scenes.World2DLayer;
import simplicity.GameObject;

/**
 * New objects (duplicated, placed from the palette): redo adds them to the world and selects them, undo takes them
 * out again. The same instances go in and out, so later commands that point to them stay valid.
 */
public final class AddObjectsCommand implements EditorCommand {

    private final String name;
    private final World2DLayer world;
    private final EditorSelection selection;
    private final List<GameObject> objects;
    private final List<GameObject> selectionBefore = new ArrayList<>();
    private boolean inWorld = false;

    public AddObjectsCommand(String name, World2DLayer world, EditorSelection selection, List<GameObject> objects) {
        this.name = name;
        this.world = world;
        this.selection = selection;
        this.objects = new ArrayList<>(objects);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void redo() {
        selectionBefore.clear();
        if (selection != null) selectionBefore.addAll(selection.selectedGameObjects);
        for (GameObject go : objects) world.addGameObjectToScene(go);
        inWorld = true;
        if (selection != null) selection.set(objects);
    }

    @Override
    public void undo() {
        for (GameObject go : objects) world.removeGameObject(go);
        inWorld = false;
        if (selection != null) selection.set(selectionBefore);
    }

    /** Undone and gone from the history: nothing can add them back, so they're finished. */
    @Override
    public void discard() {
        if (!inWorld) for (GameObject go : objects) go.destroy();
    }
}
