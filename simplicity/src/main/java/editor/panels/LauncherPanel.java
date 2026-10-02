package editor.panels;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import editor.SImGui;
import editor.SimplicityEditorContext;
import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiSelectableFlags;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import observers.events.Event;
import simplicity.Application.RenderContext;

public class LauncherPanel extends SimplicityPanel {

    public LauncherPanel(SimplicityEditorContext editorContext) {
        super(editorContext);
        
        // TEMPORARY demo rows, to see the layout before the real recent list exists; delete when it does
        // recentProjects.add(new RecentProject("My Open World", "C:/Games/MyOpenWorld/project.simplicity", Instant.now().minusSeconds(7200), false));
        // recentProjects.add(new RecentProject("Test Project", "D:/tests/proj1/project.simplicity", Instant.now().minusSeconds(3 * 86400), false));
        // recentProjects.add(new RecentProject("Old Prototype", "E:/old/proto/project.simplicity", Instant.now().minusSeconds(60L * 86400), true));
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
        // launcherContent();

        // SImGui.image(editorContext.icons.folder, 20, 20);
        // ImGui.sameLine();
        // ImGui.text("My Open World");
        // if (SImGui.imageButton("play", editorContext.icons.play, 24, 24)) {}


        SImGui.showIconsExample();

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
            ImGui.sameLine(rowWidth - 90);
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

            ImGui.setCursorPos(x, y + rowHeight + 2);
            ImGui.separator();
            ImGui.popID();
        }

        if (toRemove != null) {                   // removed after the loop, not while iterating
            recentProjects.remove(toRemove);
            selected = -1;
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
        ImGui.textDisabled("    contains project.simplicity, assets/, worlds/");

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
    protected void onOpenProjectDialog() { }

    /** TODO: open this recent project (Project.open), then switch to the editor. */
    protected void onOpenRecent(RecentProject project) { }

    /** TODO: native folder dialog; return the chosen folder, or null if cancelled. */
    protected String onBrowseLocation() { return null; }

    /** TODO: create the folder and files (Project.create), add to the recent list, open it. */
    protected void onCreateProject(String name, String location) { }

    /** TODO: pick the project's new location and update the entry. */
    protected void onLocate(RecentProject project) { }

    /** TODO: open the project's folder in the OS file explorer. */
    protected void onShowInExplorer(RecentProject project) { }

    /** TODO: save the recent list (recent.json in the user's folder). */
    protected void onRecentListChanged() { }

    @Override
    public void destroy() {

    }

    @Override
    public void onEvent(Event event) {

    }
}