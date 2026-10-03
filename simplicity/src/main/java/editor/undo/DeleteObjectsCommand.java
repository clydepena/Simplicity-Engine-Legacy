package editor.undo;

import java.util.ArrayList;
import java.util.List;

import editor.EditorSelection;
import scenes.World2DLayer;
import simplicity.GameObject;

/**
 * Deleted objects: redo takes them out of the world (without destroying them), undo puts the same instances back
 * at their old places in the object list, selected.
 */
public final class DeleteObjectsCommand implements EditorCommand {

    private final String name;
    private final World2DLayer world;
    private final EditorSelection selection;
    private final List<GameObject> objects;   // sorted by their index in the world, so undo can insert in order
    private final List<Integer> indexes = new ArrayList<>();
    private boolean inWorld = true;

    public DeleteObjectsCommand(String name, World2DLayer world, EditorSelection selection, List<GameObject> objects) {
        this.name = name;
        this.world = world;
        this.selection = selection;
        List<GameObject> all = world.getGameObjectList();
        List<GameObject> sorted = new ArrayList<>(objects);
        sorted.removeIf(go -> !all.contains(go));
        sorted.sort((a, b) -> Integer.compare(all.indexOf(a), all.indexOf(b)));
        this.objects = sorted;
    }

    public boolean isEmpty() {
        return objects.isEmpty();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void redo() {
        // the index each object has at the moment it's removed; removing from the back keeps the earlier ones valid
        indexes.clear();
        List<GameObject> all = world.getGameObjectList();
        for (GameObject go : objects) indexes.add(all.indexOf(go));
        for (int i = objects.size() - 1; i >= 0; i--) world.removeGameObject(objects.get(i));
        inWorld = false;
        if (selection != null) selection.remove(objects);
    }

    @Override
    public void undo() {
        // ascending: each object goes back to the index it had, with the earlier ones already in place
        for (int i = 0; i < objects.size(); i++) world.addGameObjectToScene(indexes.get(i), objects.get(i));
        inWorld = true;
        if (selection != null) selection.set(objects);
    }

    /** Deleted and gone from the history: nothing can bring them back, so they're finished. */
    @Override
    public void discard() {
        if (!inWorld) for (GameObject go : objects) go.destroy();
    }
}
