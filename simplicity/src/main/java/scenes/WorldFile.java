package scenes;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import components.Component;
import simplicity.GameObject;
import simplicity.GameObjectGson;

/**
 * The .world file format, shared by the game and the editor:
 * { "formatVersion": 1, "objects": [ ...game objects... ] }
 * Older files are a bare array of game objects (or "{}"), and are still read.
 */
public final class WorldFile {
    public static final int FORMAT_VERSION = 1;

    public int formatVersion = FORMAT_VERSION;
    public List<GameObject> objects = new ArrayList<>();

    public static WorldFile read(Path file) throws IOException {
        return parse(Files.readString(file));
    }

    /** @throws IOException if the text isn't a world, or was written by a newer engine */
    public static WorldFile parse(String json) throws IOException {
        WorldFile world = new WorldFile();
        String trimmed = json.trim();
        if (trimmed.isEmpty() || trimmed.equals("{}")) return world;   // empty world, as early project creation wrote it

        try {
            JsonElement root = JsonParser.parseString(trimmed);
            JsonElement objects;
            if (root.isJsonArray()) {
                objects = root;                                         // the old format: just the objects
            } else if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                if (obj.has("formatVersion")) world.formatVersion = obj.get("formatVersion").getAsInt();
                if (world.formatVersion > FORMAT_VERSION) {
                    throw new IOException("made with a newer engine (world format " + world.formatVersion + ")");
                }
                objects = obj.get("objects");
            } else {
                throw new IOException("expected a world object or a list of game objects");
            }

            if (objects != null && !objects.isJsonNull()) {
                GameObject[] read = GameObjectGson.GSON.fromJson(objects, GameObject[].class);
                if (read != null) world.objects.addAll(Arrays.asList(read));
            }
            world.formatVersion = FORMAT_VERSION;                       // upgraded once it's in memory
            return world;
        } catch (JsonParseException | IllegalStateException e) {        // broken JSON, or a field of the wrong kind
            throw new IOException(e.getMessage(), e);
        }
    }

    /** A snapshot of the world's saved objects (editor-only objects, with doSerialization() off, are left out). */
    public static WorldFile from(World2DLayer world) {
        WorldFile file = new WorldFile();
        for (GameObject go : world.getGameObjectList()) {
            if (go.doSerialization() && !go.isDead()) file.objects.add(go);
        }
        return file;
    }

    /** Adds the objects to the world, and moves the uid counters past theirs so new objects don't reuse a uid. */
    public void addTo(World2DLayer world) {
        if (objects.isEmpty()) return;
        int maxGoId = -1;
        int maxCompId = -1;
        for (GameObject go : objects) {
            world.addGameObjectToScene(go);
            for (Component c : go.getAllComponenets()) maxCompId = Math.max(maxCompId, c.getUid());
            maxGoId = Math.max(maxGoId, go.getUid());
        }
        GameObject.init(maxGoId + 1);
        Component.init(maxCompId + 1);
    }

    /** The file's text; parse() reads it back. Also used for in-memory snapshots (the editor's Play). */
    public String toJson() {
        return GameObjectGson.GSON.toJson(this);
    }

    /** Writes next to the file first, then swaps it in: a failed write never leaves a half-written world. */
    public void write(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, toJson());
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
