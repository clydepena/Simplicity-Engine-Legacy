package scenes;

import java.io.IOException;
import java.nio.file.Paths;

/** Opens a world for editing: loads its .world file (if any) into the world. */
public class LevelEditorSceneInitializer implements SceneInitializer {
    private String currentFile = null;

    public LevelEditorSceneInitializer() {

    }

    public LevelEditorSceneInitializer(String levelPath) {
        this.currentFile = levelPath;
    }

    @Override
    public void init(World2DLayer world) {
        // editor tools (camera, gizmo, selection) live in the editor layer, not as objects in the world
        if (currentFile != null) {
            load(currentFile, world);
        }
    }

    @Override
    public void loadResources(World2DLayer world) {
        // nothing to preload: the shaders are loaded by the renderer, and sheets and textures by the asset pools
        // when objects ask for them
    }

    private void load(String filepath, World2DLayer world) {
        // the format (old bare arrays included) and the uid fix-up are in WorldFile, shared with the game
        try {
            WorldFile.read(Paths.get(filepath)).addTo(world);
            logger.Logger.info("Successfully loaded '" + filepath + "'");
        } catch (IOException e) {
            // a missing or broken world file: log it and open an empty world rather than crashing the editor
            logger.Logger.error("Can't read the world '" + filepath + "': " + e.getMessage());
        }
    }
}
