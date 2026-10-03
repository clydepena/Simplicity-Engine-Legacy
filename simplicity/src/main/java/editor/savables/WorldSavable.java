package editor.savables;

import java.io.IOException;
import java.nio.file.Path;

import asset.Savable;
import scenes.World2DLayer;
import scenes.WorldFile;

/** The open world as a document: saves the live world's objects to its .world file. One per opened world. */
public final class WorldSavable implements Savable {

    private final World2DLayer world;
    private final Path file;

    public WorldSavable(World2DLayer world, Path file) {
        this.world = world;
        this.file = file;
    }

    public Path getFile() {
        return file;
    }

    @Override
    public String displayName() {
        return file.getFileName().toString();
    }

    @Override
    public void save() throws IOException {
        WorldFile.from(world).write(file);
    }
}
