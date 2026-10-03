package editor;

import java.util.ArrayList;
import java.util.List;

import asset.AssetPool;
import asset.AssetPoolHandler;
import asset.UnsavedChanges;
import editor.savables.WorldSavable;
import editor.undo.AddObjectsCommand;
import editor.undo.DeleteObjectsCommand;
import editor.undo.EditorCommand;
import editor.undo.UndoHistory;
import scenes.World2DLayer;
import simplicity.GameObject;

public class SimplicityEditorContext extends editor.EditorContext<SimplicityEditor> {
    public Project project;
    public World2DLayer world;
    public EditorSelection gameObjectSelection;
    public AssetPoolHandler assetPoolHandler;
    public AssetPool projectAssets, engineResources;
    public EditorIcons icons;
    public UnsavedChanges unsavedChanges;   // new for each opened project
    public WorldSavable worldSavable;       // the open world; null if it has no file
    public UndoHistory history;             // the open world's edits; also decides whether it's unsaved

    public final PlaySession playSession = new PlaySession();

    public SimplicityEditorContext(SimplicityEditor editorLayer) {
        super(editorLayer);
    }

    /*
     * World edits. Every change to the open world goes through the history (execute or record), never by marking
     * the world dirty directly: the history decides dirty/clean, so undoing back to the saved state clears the "*".
     * While playing, the world holds the played state, so editing is refused.
     */

    public boolean canEdit() {
        return history != null && world != null && !playSession.isPlaying();
    }

    /** Applies a world edit and records it for undo. */
    public void execute(EditorCommand command) {
        if (canEdit()) history.execute(command);
    }

    /** Records a world edit that was already applied (e.g. a gizmo drag, applied live). */
    public void record(EditorCommand command) {
        if (canEdit()) history.record(command);
    }

    public void undo() {
        if (canEdit()) history.undo();
    }

    public void redo() {
        if (canEdit()) history.redo();
    }

    /** Adds new objects to the world (selected), as one undoable step. */
    public void addObjects(String name, List<GameObject> objects) {
        if (objects.isEmpty()) return;
        execute(new AddObjectsCommand(name, world, gameObjectSelection, objects));
    }

    public void deleteSelected() {
        List<GameObject> selected = selectedObjects();
        if (!canEdit() || selected.isEmpty()) return;
        DeleteObjectsCommand command = new DeleteObjectsCommand(nameFor("Delete", selected), world, gameObjectSelection, selected);
        if (!command.isEmpty()) execute(command);
    }

    /** Copies of the selected objects, in place, selected instead of the originals. */
    public void duplicateSelected() {
        List<GameObject> selected = selectedObjects();
        if (!canEdit() || selected.isEmpty()) return;
        List<GameObject> copies = new ArrayList<>();
        for (GameObject go : selected) copies.add(go.copy());
        addObjects(nameFor("Duplicate", selected), copies);
    }

    /** The selected objects that are alive, in selection order. */
    public List<GameObject> selectedObjects() {
        List<GameObject> objects = new ArrayList<>();
        if (gameObjectSelection == null || gameObjectSelection.selectedGameObjects == null) return objects;
        for (GameObject go : gameObjectSelection.selectedGameObjects) {
            if (go != null && !go.isDead()) objects.add(go);
        }
        return objects;
    }

    /** "Delete 'Player'" or "Delete 3 objects", for the Edit menu. */
    public static String nameFor(String action, List<GameObject> objects) {
        return objects.size() == 1 ? action + " '" + objects.get(0).name + "'" : action + " " + objects.size() + " objects";
    }
}
