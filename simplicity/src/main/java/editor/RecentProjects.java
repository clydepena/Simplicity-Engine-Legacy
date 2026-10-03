package editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/**
 * The launcher's recent-projects list, saved per user (never in a project):
 *   Windows:  %APPDATA%/Simplicity/recent.json
 *   macOS:    ~/Library/Application Support/Simplicity/recent.json
 *   Linux:    ~/.config/simplicity/recent.json
 * Newest first, one entry per project file, at most MAX_ENTRIES.
 */
public final class RecentProjects {

    public static final int FORMAT_VERSION = 1;
    public static final int MAX_ENTRIES = 20;

    /** One project. lastOpened is an ISO-8601 string: Gson can't serialize java.time.Instant by reflection on Java 17+. */
    public static final class Entry {
        public String name;          // a copy of the project's name, refreshed whenever it's opened
        public String path;          // absolute path to its project.simplicity
        public String lastOpened;    // e.g. "2026-10-03T09:14:00Z"

        public Instant lastOpenedInstant() {
            try {
                return lastOpened == null ? null : Instant.parse(lastOpened);
            } catch (RuntimeException e) {
                return null;
            }
        }

        /** Recomputed when asked, never saved, so it can't go stale. */
        public boolean isMissing() {
            return path == null || !Files.isRegularFile(Path.of(path));
        }
    }

    // the saved fields
    public int formatVersion = FORMAT_VERSION;
    public String lastLocation;      // last parent folder used for "New Project"
    public List<Entry> projects = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    /** Where the list is stored for this user. */
    public static Path file() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home");
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return Path.of(appData != null ? appData : home, "Simplicity", "recent.json");
        }
        if (os.contains("mac")) return Path.of(home, "Library", "Application Support", "Simplicity", "recent.json");
        return Path.of(home, ".config", "simplicity", "recent.json");
    }

    /** The saved list, or an empty one if there's none yet or it can't be read (never throws: the launcher must open). */
    public static RecentProjects load() {
        Path file = file();
        if (!Files.isRegularFile(file)) return new RecentProjects();
        try {
            RecentProjects loaded = GSON.fromJson(Files.readString(file), RecentProjects.class);
            if (loaded == null) return new RecentProjects();
            if (loaded.projects == null) loaded.projects = new ArrayList<>();
            loaded.projects.removeIf(e -> e == null || e.path == null);
            return loaded;
        } catch (IOException | JsonParseException e) {
            System.err.println("Couldn't read the recent projects (" + file + "): " + e.getMessage());
            return new RecentProjects();
        }
    }

    /** Writes the list; a temp file swapped in, so a crash mid-save can't leave half a file. */
    public void save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(this));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("Couldn't save the recent projects (" + file + "): " + e.getMessage());
        }
    }

    /** The project was just opened: moves it to the top with the current time and name (adds it if new). */
    public void touch(Path projectFile, String name) {
        String key = normalize(projectFile);
        projects.removeIf(e -> normalize(Path.of(e.path)).equals(key));
        Entry entry = new Entry();
        entry.name = name;
        entry.path = key;
        entry.lastOpened = Instant.now().toString();
        projects.add(0, entry);
        while (projects.size() > MAX_ENTRIES) projects.remove(projects.size() - 1);
    }

    /** Removes a project from the list (its files are untouched). */
    public void remove(String projectFile) {
        String key = normalize(Path.of(projectFile));
        projects.removeIf(e -> normalize(Path.of(e.path)).equals(key));
    }

    /** A project was moved: points its entry at the new project file, keeping its place in the list. */
    public void relocate(String oldProjectFile, Path newProjectFile, String name) {
        String oldKey = normalize(Path.of(oldProjectFile));
        String newKey = normalize(newProjectFile);
        projects.removeIf(e -> normalize(Path.of(e.path)).equals(newKey) && !newKey.equals(oldKey));   // no duplicate
        for (Entry e : projects) {
            if (normalize(Path.of(e.path)).equals(oldKey)) {
                e.path = newKey;
                if (name != null) e.name = name;
            }
        }
    }

    /** "C:\Games\x\project.simplicity" and "C:/Games/x/./project.simplicity" are the same project. */
    private static String normalize(Path path) {
        return path.toAbsolutePath().normalize().toString().replace('\\', '/');
    }
}
