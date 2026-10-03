package editor;

import java.nio.file.Files;
import java.nio.file.Path;

import editor.Project.ProjectFile;
import scenes.WorldFile;

public class SimplicityEditorIO {

    public static ProjectFile createNewProjectFolder(String name, Path folderPath) {
        Path projectPath = folderPath.resolve(name);
        try {
            Files.createDirectories(projectPath);
            ProjectFile projectFile = new ProjectFile();
            projectFile.projectName = name;
            String worldName = name.replace(" ", "-").toLowerCase() + ".world";

            Path worldPath = projectPath.resolve("world/" + worldName);
            Path projectFilePath = projectPath.resolve(ProjectFile.fileNameFor(name));

            projectFile.startingWorld = projectPath.relativize(worldPath).normalize().toString().replace('\\', '/');
            projectFile.write(projectFilePath);

            new WorldFile().write(worldPath);   // an empty world, in the current world format (creates world/)

            return projectFile;
        } catch (Exception e) {
            System.err.println(e);
            return null;
        }
    }
}
