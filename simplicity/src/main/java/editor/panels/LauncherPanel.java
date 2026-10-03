package editor.panels;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import editor.FontAwesomeIcons;
import editor.RecentProjects;
import editor.SimplicityEditorContext;
import editor.SimplicityEditorIO;
import editor.Project.ProjectFile;
import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiMouseCursor;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiSelectableFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import observers.events.Event;
import simplicity.Application.RenderContext;
import util.IOHelper;

public class LauncherPanel extends SimplicityPanel {

    private final RecentProjects recent = RecentProjects.load();   // the saved list (recent.json in the user's folder)

    public LauncherPanel(SimplicityEditorContext editorContext) {
        super(editorContext);
        refreshList();
        if (recent.lastLocation != null) newLocation.set(recent.lastLocation);
    }

    /** Rebuilds the rows the UI shows from the saved list; "missing" is checked on disk here. */
    private void refreshList() {
        recentProjects.clear();
        for (RecentProjects.Entry e : recent.projects) {
            recentProjects.add(new RecentProject(e.name, e.path, e.lastOpenedInstant(), e.isMissing()));
        }
        selected = -1;
    }

    @Override
    public void onUpdate(float dt) {
        
    }

    @Override
    public void onRender(RenderContext renderContext) {
        ImGuiViewport vp = ImGui.getMainViewport();
        ImGui.setNextWindowPos(vp.getWorkPosX(), vp.getWorkPosY());
        ImGui.setNextWindowSize(vp.getWorkSizeX(), vp.getWorkSizeY());
        ImGui.setNextWindowViewport(vp.getID());

        int flags = ImGuiWindowFlags.NoDecoration          
                | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoDocking             
                | ImGuiWindowFlags.NoSavedSettings       
                | ImGuiWindowFlags.NoBringToFrontOnFocus
                | ImGuiWindowFlags.NoNavFocus;

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
        ImGui.begin("##launcher", flags);                   
        updateCalc();
        ImGui.popStyleVar(2);
        launcherContent();

        // SImGui.image(editorContext.icons.folder, 20, 20);
        // ImGui.sameLine();
        // ImGui.text("My Open World");
        // if (SImGui.imageButton("play", editorContext.icons.play, 24, 24)) {}
        // SImGui.showIconsExample();

        ImGui.end();
    }

    // ------------------------------------------------------------------------------------------------
    // UI state (only what the launcher shows; the file work happens in the on...() hooks at the bottom)
    // ------------------------------------------------------------------------------------------------

    /** One row of the recent-projects list. */
    public static final class RecentProject {
        public String name;          // copied from its project file
        public String path;          // the project.simplicity file
        public Instant lastOpened;
        public boolean missing;      // its file no longer exists

        public RecentProject(String name, String path, Instant lastOpened, boolean missing) {
            this.name = name;
            this.path = path;
            this.lastOpened = lastOpened;
            this.missing = missing;
        }
    }

    private enum View { RECENT, NEW_PROJECT }

    protected final List<RecentProject> recentProjects = new ArrayList<>();
    protected String errorMessage = null;          // shown in the bar at the bottom; null hides it
    protected String engineVersion = "0.1.0";

    private View view = View.RECENT;
    private int selected = -1;
    private final ImString filter = new ImString(128);
    private final ImString newName = new ImString("My Project", 128);
    private final ImString newLocation = new ImString(512);

    private static final float ACTIONS_WIDTH = 220;

    protected void launcherContent() {
        // header: engine name, version on the right
        ImGui.text("SIMPLICITY");
        ImGui.sameLine(ImGui.getWindowWidth() - 90);
        ImGui.textDisabled("v" + engineVersion);
        ImGui.separator();
        ImGui.spacing();

        // leave room at the bottom for the error bar
        float footer = errorMessage != null ? ImGui.getFrameHeightWithSpacing() + 8 : 0;

        if (recentProjects.isEmpty() && view == View.RECENT) {
            welcome();
        } else {
            ImGui.beginChild("##actions", ACTIONS_WIDTH, -footer, false);
            actions();
            ImGui.endChild();

            ImGui.sameLine();

            ImGui.beginChild("##main", 0, -footer, true);
            if (view == View.RECENT) recentList();
            else newProjectForm();
            ImGui.endChild();
        }

        if (errorMessage != null) errorBar();
    }

    /** Left column: the two big buttons. */
    private void actions() {
        float w = ImGui.getContentRegionAvailX();
        ImGui.spacing();
        if (ImGui.button("+  New Project", w, 40)) {
            view = View.NEW_PROJECT;
            errorMessage = null;
        }
        ImGui.spacing();
        if (ImGui.button("Open Project...", w, 40)) {
            onOpenProjectDialog();
        }
    }

    /** Right side: the recent projects, newest first. */
    private void recentList() {
        ImGui.text("Recent projects");
        ImGui.sameLine(ImGui.getWindowWidth() - 200 - ImGui.getStyle().getWindowPaddingX());   // filter box on the right
        ImGui.setNextItemWidth(200);
        ImGui.inputTextWithHint("##filter", "filter...", filter);
        ImGui.separator();

        String needle = filter.get().toLowerCase(Locale.ROOT);
        float rowHeight = ImGui.getTextLineHeightWithSpacing() * 2 + 6;
        float playWidth = ImGui.calcTextSize(FontAwesomeIcons.Play).x;   // the play icon on the right of each row
        RecentProject toRemove = null;

        for (int i = 0; i < recentProjects.size(); i++) {
            RecentProject p = recentProjects.get(i);
            if (!needle.isEmpty() && !p.name.toLowerCase(Locale.ROOT).contains(needle)
                                  && !p.path.toLowerCase(Locale.ROOT).contains(needle)) continue;

            ImGui.pushID(i);
            float rowWidth = ImGui.getContentRegionAvailX();
            float x = ImGui.getCursorPosX();
            float y = ImGui.getCursorPosY();

            // the whole row is one selectable; the texts are drawn on top of it afterwards
            if (ImGui.selectable("##row", selected == i, ImGuiSelectableFlags.AllowDoubleClick, 0, rowHeight)) {
                selected = i;
                if (ImGui.isMouseDoubleClicked(ImGuiMouseButton.Left) && !p.missing) onOpenRecent(p);
            }
            ImGui.setItemAllowOverlap();   // lets the Locate / Remove buttons on top of the row be clicked

            if (ImGui.beginPopupContextItem("##context")) {
                if (ImGui.menuItem("Open", "", false, !p.missing)) onOpenRecent(p);
                if (ImGui.menuItem("Show in Explorer", "", false, !p.missing)) onShowInExplorer(p);
                ImGui.separator();
                if (ImGui.menuItem("Remove from list")) toRemove = p;
                ImGui.endPopup();
            }

            // first line: name, and when it was last opened on the right
            ImGui.setCursorPos(x + 8, y + 3);
            if (p.missing) {
                ImGui.textColored(230, 170, 60, 255, "! " + p.name);
                ImGui.sameLine();
                ImGui.textDisabled("(not found)");
            } else {
                ImGui.text(p.name);
            }
            ImGui.sameLine(rowWidth - 90 - (p.missing ? 0 : playWidth + 16));   // left of the play icon
            ImGui.textDisabled(timeAgo(p.lastOpened));

            // second line: the folder, dimmer; Locate / Remove for missing projects
            ImGui.setCursorPosX(x + 8);
            ImGui.textDisabled(p.path);
            if (p.missing) {
                ImGui.sameLine(rowWidth - 140);
                if (ImGui.smallButton("Locate...")) onLocate(p);
                ImGui.sameLine();
                if (ImGui.smallButton("Remove")) toRemove = p;
            }

            // the play icon: how a project is opened. Plain text, no button frame: an invisible click area
            // the size of the icon, with the icon drawn on it; brighter while hovered
            if (!p.missing) {
                float iconHeight = ImGui.getTextLineHeight();
                ImGui.setCursorPos(x + rowWidth - playWidth - 12, y + (rowHeight - iconHeight) / 2);
                boolean open = ImGui.invisibleButton("##open", playWidth, iconHeight);
                boolean hovered = ImGui.isItemHovered();
                int color = hovered ? ImGui.colorConvertFloat4ToU32(0.51f, 1.00f, 0.59f, 1f)
                                    : ImGui.colorConvertFloat4ToU32(0.35f, 0.84f, 0.45f, 1f);
                ImGui.getWindowDrawList().addText(ImGui.getItemRectMinX(), ImGui.getItemRectMinY(), color, FontAwesomeIcons.Play);
                if (hovered) {
                    ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
                    ImGui.setTooltip("Open " + p.name);
                }
                if (open) onOpenRecent(p);
            }

            ImGui.setCursorPos(x, y + rowHeight + 2);
            ImGui.separator();
            ImGui.popID();
        }

        if (toRemove != null) {                   // removed after the loop, not while iterating
            recent.remove(toRemove.path);
            onRecentListChanged();
        }
    }

    /** Right side, after "New Project": name, location, preview, Create. */
    private void newProjectForm() {
        ImGui.text("New project");
        ImGui.separator();
        ImGui.spacing();

        float fieldWidth = ImGui.getContentRegionAvailX() - 180;

        ImGui.text("Name");
        ImGui.sameLine(90);
        ImGui.setNextItemWidth(fieldWidth);
        ImGui.inputText("##name", newName);

        ImGui.text("Location");
        ImGui.sameLine(90);
        ImGui.setNextItemWidth(fieldWidth);
        ImGui.inputText("##location", newLocation);
        ImGui.sameLine();
        if (ImGui.button("Browse...")) {
            String chosen = onBrowseLocation();
            if (chosen != null) newLocation.set(chosen);
        }

        ImGui.spacing();
        ImGui.spacing();
        ImGui.textDisabled("Will be created at:");
        ImGui.sameLine();
        ImGui.text(previewPath());
        String fileName = newName.get().trim().isEmpty() ? "<name>" : newName.get().trim();
        ImGui.textDisabled("    contains " + fileName + ".simplicity and world/");   // what createNewProjectFolder makes

        String problem = validateNewProject(newName.get(), newLocation.get());
        if (problem != null) {
            ImGui.spacing();
            ImGui.textColored(230, 170, 60, 255, "! " + problem);
        }

        // Cancel / Create in the bottom-right corner
        float buttonsWidth = 90 + 90 + ImGui.getStyle().getItemSpacingX();
        ImGui.setCursorPos(ImGui.getWindowWidth() - buttonsWidth - 12, ImGui.getWindowHeight() - ImGui.getFrameHeight() - 12);
        if (ImGui.button("Cancel", 90, 0)) {
            view = View.RECENT;
        }
        ImGui.sameLine();
        ImGui.beginDisabled(problem != null);
        if (ImGui.button("Create", 90, 0)) {
            onCreateProject(newName.get().trim(), newLocation.get().trim());
        }
        ImGui.endDisabled();
    }

    /** First launch: nothing to list, so two centred buttons instead. */
    private void welcome() {
        float buttonWidth = 300;
        float x = (ImGui.getWindowWidth() - buttonWidth) / 2;

        ImGui.setCursorPosY(ImGui.getWindowHeight() * 0.35f);
        ImGui.setCursorPosX(x);
        ImGui.text("Welcome to Simplicity");
        ImGui.spacing();
        ImGui.spacing();

        ImGui.setCursorPosX(x);
        if (ImGui.button("+  Create your first project", buttonWidth, 40)) {
            view = View.NEW_PROJECT;
        }
        ImGui.spacing();
        ImGui.setCursorPosX(x);
        if (ImGui.button("Open an existing project...", buttonWidth, 40)) {
            onOpenProjectDialog();
        }
    }

    /** Bottom bar, only while there's an error; [x] dismisses it. */
    private void errorBar() {
        ImGui.separator();
        ImGui.textColored(255, 90, 90, 255, "! " + errorMessage);
        ImGui.sameLine(ImGui.getWindowWidth() - 40);
        if (ImGui.smallButton("x")) errorMessage = null;
    }

    private String previewPath() {
        String location = newLocation.get().trim().replace('\\', '/');
        String name = newName.get().trim();
        if (location.isEmpty() || name.isEmpty()) return "-";
        return (location.endsWith("/") ? location : location + "/") + name + "/";
    }

    /** "just now", "5 min ago", "2 h ago", "3 days ago", or the date. */
    private static String timeAgo(Instant when) {
        if (when == null) return "";
        long minutes = Duration.between(when, Instant.now()).toMinutes();
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " min ago";
        if (minutes < 60 * 24) return (minutes / 60) + " h ago";
        if (minutes < 60 * 24 * 30) return (minutes / (60 * 24)) + " days ago";
        return when.atZone(ZoneId.systemDefault()).toLocalDate().toString();
    }

    // ------------------------------------------------------------------------------------------------
    // Hooks: the real work (dialogs, files, opening). Each one is called by the UI above.
    // ------------------------------------------------------------------------------------------------

    /** Name rules only; TODO: also check the folder (exists and not empty, location writable). */
    protected String validateNewProject(String name, String location) {
        String n = name.trim();
        if (n.isEmpty()) return "The project needs a name.";
        if (n.matches(".*[\\\\/:*?\"<>|].*")) return "The name can't contain \\ / : * ? \" < > |";
        if (location.trim().isEmpty()) return "Choose a location.";
        return null;
    }

    /** TODO: native dialog for a project.simplicity file, then open it. */
    protected void onOpenProjectDialog() {
        String stringPath = IOHelper.openSingle(editorContext.editorLayer.appContext().window(), "simplicity");
        if (stringPath != null) {
            setProject(stringPath);
        }
    }

    /** TODO: open this recent project (Project.open), then switch to the editor. */
    protected void onOpenRecent(RecentProject project) {
        if (!project.missing) {
            setProject(project.path);
        }
    }

    /** TODO: native folder dialog; return the chosen folder, or null if cancelled. */
    protected String onBrowseLocation() {
        return IOHelper.openFolder(editorContext.editorLayer.appContext().window());
    }

    /** Creates the project, adds it to the recent list (and remembers the location), then opens it. */
    protected void onCreateProject(String name, String location) {
        ProjectFile projectFile = SimplicityEditorIO.createNewProjectFolder(name, Path.of(location));
        if (projectFile == null) {
            errorMessage = "Couldn't create the project in " + location;
            return;
        }
        // the same file createNewProjectFolder writes: "<location>/<name>/<name>.simplicity"
        Path projectFilePath = Path.of(location).resolve(name).resolve(ProjectFile.fileNameFor(name));
        recent.lastLocation = location;
        recent.touch(projectFilePath, projectFile.projectName);
        recent.save();
        editorContext.editorLayer.setProject(projectFilePath, projectFile);
    }

    /** A missing project: pick its project file at the new location, and point the entry there. */
    protected void onLocate(RecentProject project) {
        String chosen = IOHelper.openSingle(editorContext.editorLayer.appContext().window(), "simplicity");
        if (chosen == null) return;
        try {
            ProjectFile projectFile = ProjectFile.read(Path.of(chosen));      // also checks it really is a project file
            recent.relocate(project.path, Path.of(chosen), projectFile.projectName);
            onRecentListChanged();
        } catch (Exception e) {
            errorMessage = "Not a project file: " + chosen + " (" + e.getMessage() + ")";
        }
    }

    /** Opens the project's folder in the OS file manager; on Windows with its project file selected. */
    protected void onShowInExplorer(RecentProject project) {
        Path projectFile = Path.of(project.path).toAbsolutePath();
        Path folder = projectFile.getParent();
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        try {
            if (os.contains("win")) {
                // "/select," and the path must be separate arguments: as one argument containing a space
                // (e.g. "My Project"), Java quotes the whole thing, explorer ignores the switch and opens Documents
                if (Files.isRegularFile(projectFile)) {
                    new ProcessBuilder("explorer.exe", "/select,", projectFile.toString()).start();   // folder, file highlighted
                } else {
                    new ProcessBuilder("explorer.exe", folder.toString()).start();                    // just the folder
                }
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", "-R", projectFile.toString()).start();       // Finder, file revealed
            } else {
                new ProcessBuilder("xdg-open", folder.toString()).start();              // the desktop's file manager
            }
        } catch (IOException e) {
            errorMessage = "Couldn't open " + folder + ": " + e.getMessage();
        }
    }

    /** The list changed (an entry removed or relocated): save it and rebuild the rows. */
    protected void onRecentListChanged() {
        recent.save();
        refreshList();
    }

    /** Opens a project file: on success, moves it to the top of the recent list; on failure, shows why. */
    protected void setProject(String stringPath) {
        try {
            Path projectPath = Path.of(stringPath);
            ProjectFile projectFile = ProjectFile.read(projectPath);
            recent.touch(projectPath, projectFile.projectName);
            recent.save();
            editorContext.editorLayer.setProject(projectPath, projectFile);
        } catch (Exception e) {
            errorMessage = "Couldn't open " + stringPath + ": " + e.getMessage();
        }
    }

    @Override
    public void destroy() {

    }

    @Override
    public void onEvent(Event event) {

    }
}