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
        public static final String FILE_NAME = "project.simplicity";
        public static final int FORMAT_VERSION = 1;

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

    public Path rootPath;
    public ProjectFile projectData;
    public AssetPool projectAssets;

    public Project(Path rootPath, ProjectFile projectData, AssetPool projectAssets) {
        this.rootPath = rootPath;
        this.projectData = projectData;
        this.projectAssets = projectAssets;
    }

    public static Project open(Path projectFile) throws IOException {
        ProjectFile settings = ProjectFile.read(projectFile);
        Path root = projectFile.toAbsolutePath().getParent();
        AssetPool pool = AssetPoolHandler.GetInstance().createAssetPool("", root.toString(), AssetPoolHandler.FileReadingCallback.FILE_SYSTEM);
        return new Project(root, settings, pool);
    }
}