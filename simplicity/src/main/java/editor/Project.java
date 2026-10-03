package editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import asset.AssetPool;
import asset.AssetPoolHandler;

public final class Project {
    
    public static final class ProjectFile {
        public static final String EXTENSION = "simplicity";
        public static final int FORMAT_VERSION = 1;

        /** The project file's name for a project: "My Game" -> "My Game.simplicity". */
        public static String fileNameFor(String projectName) {
            return projectName + "." + EXTENSION;
        }

        public int formatVersion = FORMAT_VERSION;
        public String projectName = "Untitled";
        public String engineVersion = "0.1.0";
        public String startingWorld;

        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        public static ProjectFile read(Path file) throws IOException {
            ProjectFile projectFile = GSON.fromJson(Files.readString(file), ProjectFile.class);
            if (projectFile == null) throw new IOException("empty project file: " + file);
            if (projectFile.formatVersion > FORMAT_VERSION) throw new IOException("made with a newer engine (format " + projectFile.formatVersion + ")");
            return projectFile;
        }

        public void write(Path file) throws IOException {
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(this));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
    }

    public final Path projectFilePath;   // the .simplicity file this project was opened from, and is saved to
    public final Path rootPath;          // its folder: the root of the "res" pool
    public ProjectFile projectData;
    public AssetPool projectAssets;

    public Project(Path projectFilePath, ProjectFile projectData, AssetPool projectAssets) {
        this.projectFilePath = projectFilePath.toAbsolutePath().normalize();
        this.rootPath = this.projectFilePath.getParent();
        this.projectData = projectData;
        this.projectAssets = projectAssets;
    }

    /** Creates the project's "res" pool, rooted at the project file's folder. */
    public Project(Path projectFilePath, ProjectFile projectData, AssetPoolHandler assetPoolHandler) {
        this(projectFilePath, projectData, (AssetPool) null);
        this.projectAssets = assetPoolHandler.createAssetPool("res", rootPath.toString(), AssetPoolHandler.FileReadingCallback.FILE_SYSTEM);
    }

    public static Project open(Path projectFile) throws IOException {
        return new Project(projectFile, ProjectFile.read(projectFile), AssetPoolHandler.GetInstance());
    }

    @Override
    public String toString() {
        return  projectData.projectName + " | \'" + rootPath.toString() + '\'';
    }
}