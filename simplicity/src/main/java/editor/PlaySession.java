package editor;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector2f;

import scenes.World2DLayer;
import scenes.WorldFile;
import simplicity.Camera;
import simplicity.GameObject;

/**
 * Play inside the editor: start() snapshots the world in memory (as it would be saved, unsaved edits included),
 * stop() rebuilds the world from that snapshot, so nothing that happened while playing is kept.
 * The rebuilt objects are new instances, so the selection is restored by position in the snapshot (game object
 * uids aren't kept through serialization yet).
 */
public final class PlaySession {

    private String snapshot = null;                       // the world's JSON while playing, null otherwise
    private final List<Integer> selected = new ArrayList<>();   // indexes into the snapshot's objects
    private final Vector2f cameraPosition = new Vector2f();
    private float cameraZoom = 1.0f;

    public boolean isPlaying() {
        return snapshot != null;
    }

    public void start(SimplicityEditorContext editorContext) {
        if (isPlaying()) return;
        World2DLayer world = editorContext.world;

        WorldFile file = WorldFile.from(world);
        selected.clear();
        EditorSelection selection = editorContext.gameObjectSelection;
        if (selection != null && !selection.isNoSelection()) {
            for (GameObject go : selection.selectedGameObjects) {
                int index = file.objects.indexOf(go);
                if (index >= 0) selected.add(index);
            }
        }
        Camera camera = world.camera();
        if (camera != null) {
            cameraPosition.set(camera.position);
            cameraZoom = camera.getZoom();
        }
        snapshot = file.toJson();

        // bodies were built when the scene started; edits since then only changed transforms and components
        world.rebuildPhysics();
    }

    /** Puts the world back as it was at start(). If the snapshot can't be read, the world is left as it is. */
    public void stop(SimplicityEditorContext editorContext) {
        if (!isPlaying()) return;
        String json = snapshot;
        snapshot = null;

        WorldFile restored;
        try {
            restored = WorldFile.parse(json);
        } catch (Exception e) {
            logger.Logger.error("Can't restore the world after Play, it keeps its played state: " + e.getMessage());
            return;
        }

        World2DLayer world = editorContext.world;
        world.clearGameObjects();
        restored.addTo(world);
        world.startScene();

        // undo commands point to the objects that were just replaced: they can't be applied to the new ones
        // (that needs ids that survive serialization), so the history starts over; the unsaved state is kept
        if (editorContext.history != null) editorContext.history.clear();

        Camera camera = world.camera();
        if (camera != null) {
            camera.position.set(cameraPosition);
            camera.setZoom(cameraZoom);
        }

        EditorSelection selection = editorContext.gameObjectSelection;
        if (selection != null) {
            List<GameObject> reselected = new ArrayList<>();
            for (int index : selected) {
                if (index < restored.objects.size()) reselected.add(restored.objects.get(index));
            }
            selection.set(reselected);
        }
    }
}
