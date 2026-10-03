package editor;

import editor.Project.ProjectFile;
import editor.panels.HierarchyPanel;
import editor.panels.InspectorPanel;
import editor.panels.LauncherPanel;
import editor.panels.LoggerPanel;
import editor.panels.NodeEditorPanel;
import editor.panels.SimplicityPanel;
import editor.panels.SpritePalettePanel;
import editor.panels.ViewportPanel;
import editor.savables.WorldSavable;
import editor.undo.UndoHistory;

import static observers.events.EventType.KeyInput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import asset.AssetPoolHandler;
import asset.UnsavedChanges;
import imgui.ImGui;
import observers.events.Event;
import scenes.LevelEditorSceneInitializer;
import scenes.World2DLayer;
import simplicity.Application;
import simplicity.Window;
import simplicity.Window.WindowCloseEvent;
import simplicity.Application.RenderContext;
import simplicity.KeyListener.KeyEvent;
import util.Inputs;

public class SimplicityEditor extends ImGuiEditorLayer {

    public enum  EditorMode {
        LAUNCHER, EDITOR
    }

    private LauncherPanel launcher = null;
    private SimplicityEditorContext editorContext = null;
    private ViewportPanel viewport  = null;
    private final List<SimplicityPanel> panels = new ArrayList<>();
    private EditorMode editorMode = EditorMode.EDITOR;

    @Override
    protected void onInitEditor() {
        editorContext = new SimplicityEditorContext(this);

        World2DLayer world = context.getLayer(World2DLayer.class);
        if (world == null) throw new IllegalStateException(this.getClass().getSimpleName() + " requires a World2DLayer to be pushed first");
        world.setFrozen(true);
        editorContext.world = world;

        AssetPoolHandler assetPoolHandler = AssetPoolHandler.GetInstance();
        editorContext.assetPoolHandler = assetPoolHandler;
        // the codecs and the engine pool are set up by Application.initEngineAssets(), before the renderer starts
        editorContext.engineResources = assetPoolHandler.getAssetPool("engine");

        initEngineResources();
        
        
        // Project project = new Project(
        //     Path.of("C:/"),
        //     new ProjectFile(),
        //     assetPoolHandler.createAssetPool("res", "C:/", AssetPoolHandler.FileReadingCallback.FILE_SYSTEM)
        // );
        // setProject(project);
        editorMode = EditorMode.LAUNCHER;
        initLauncher();
    }

    protected void initEngineResources() {
        editorContext.icons = new EditorIcons(editorContext.assetPoolHandler);
    }
    
    protected void initLauncher() {
        onDestroyEditor();
        editorMode = EditorMode.LAUNCHER;   // first, like initEditor(): no event may reach the destroyed editor panels

        Window window = context.window();
        window.setVisible(false);
        // try {
        //     Thread.sleep(500);
        // } catch (InterruptedException e) {
        //     e.printStackTrace();
        // }
        window.setTitle("Simplicity Launcher");
        window.restore();
        int width = 650, height = 360;
        window.setWindowSizeLimits(width , height , width , height);
        window.allowResize(false);
        window.centerToMonitor();
        window.setVisible(true);

        launcher = new LauncherPanel(editorContext);
        editorContext.world.setActive(false);
        editorContext.gameObjectSelection = null;
    }
    
    protected void initEditor() {
        onDestroyEditor();
        // set the mode now, not at the end: loading the world below logs, logs arrive as events, and an event
        // in LAUNCHER mode would go to the launcher that onDestroyEditor() just destroyed
        editorMode = EditorMode.EDITOR;
        editorContext.gameObjectSelection = new EditorSelection();

        Window window = context.window();
        window.setVisible(false);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (window.getTitle().equals("Simplicity Launcher")) window.setTitle("Simplicity");
        window.allowResize(true);
        window.restoreDefaultWindowSizeLimits();
        window.centerToMonitor();
        window.setVisible(true);
        window.maximize();
        
        editorContext.world.initSceneResources();
        editorContext.world.startScene();
        editorContext.world.setFrozen(true);
        editorContext.world.setActive(true);
        viewport = new ViewportPanel(editorContext);
        panels.add(new NodeEditorPanel(editorContext));
        panels.add(viewport);
        panels.add(new LoggerPanel(editorContext));
        panels.add(new HierarchyPanel(editorContext));
        panels.add(new InspectorPanel(editorContext));
        panels.add(new SpritePalettePanel(editorContext));
        System.out.println("Successfully loaded " + editorContext.project.toString());
    }

    /** @param projectFilePath the project's .simplicity file; its folder is the project's root */
    public void setProject(Path projectFilePath, ProjectFile projectFile) {
        setProject(new Project(projectFilePath, projectFile, editorContext.assetPoolHandler));
    }

    public void setProject(Project project) {
        editorContext.projectAssets = project.projectAssets;
        editorContext.project = project;
        context.window().setTitle(project.projectData.projectName + " - " + "Simplicity");
        editorContext.unsavedChanges = new UnsavedChanges();
        String startingWorld = project.projectData.startingWorld;
        if (startingWorld == null || startingWorld.isBlank()) {
            // older or hand-made project files have no starting world: open an empty one instead of failing
            editorContext.world.setScene(new LevelEditorSceneInitializer());
            editorContext.worldSavable = null;
        } else {
            Path worldPath = project.rootPath.resolve(startingWorld);
            editorContext.world.setScene(new LevelEditorSceneInitializer(worldPath.toString()));
            editorContext.worldSavable = new WorldSavable(editorContext.world, worldPath);
        }
        editorContext.history = new UndoHistory(editorContext.unsavedChanges, editorContext.worldSavable);
        initEditor();
    }

    /** Save: writes the open world, dirty or not (Ctrl+S always writes, as in most editors). */
    private void save() {
        if (refuseWhilePlaying()) return;
        WorldSavable world = editorContext.worldSavable;
        if (world == null) {
            logger.Logger.warn("Nothing to save: this world has no file");
            return;
        }
        try {
            editorContext.unsavedChanges.save(world);
            afterSave();
            logger.Logger.info("Saved '" + world.displayName() + "'");
        } catch (Exception e) {
            logger.Logger.error("Can't save '" + world.displayName() + "': " + e.getMessage());
        }
    }

    /** Save All: everything with unsaved changes; whatever fails stays dirty, so it can be saved again. */
    private void saveAll() {
        if (refuseWhilePlaying()) return;
        UnsavedChanges changes = editorContext.unsavedChanges;
        if (changes == null || !changes.any()) return;
        List<String> failed = changes.saveAll();
        afterSave();
        for (String failure : failed) logger.Logger.error("Can't save " + failure);
        if (failed.isEmpty()) logger.Logger.info("Saved all changes");
    }

    /** Once the world is saved, the undo history marks this point as the clean one (undoing away from it dirties). */
    private void afterSave() {
        WorldSavable world = editorContext.worldSavable;
        UnsavedChanges changes = editorContext.unsavedChanges;
        if (editorContext.history != null && world != null && changes != null && !changes.isDirty(world)) {
            editorContext.history.markSaved();
        }
    }

    /*
     * Closing with unsaved changes: Close Project and closing the window both ask first, in a modal popup.
     * ImGui can only open a popup while it builds a frame, and events arrive between frames, so a request sets a
     * flag that onRenderEditor() turns into openPopup(). The chosen action then runs at the start of the next
     * update (closeNow), outside the ImGui frame, so panels aren't destroyed while they're being drawn.
     */

    private enum CloseAction { CLOSE_PROJECT, QUIT }

    private static final String UNSAVED_POPUP = "Unsaved changes";
    private CloseAction pendingClose = null;    // waiting for the popup's answer
    private boolean openUnsavedPopup = false;   // open the popup on the next frame
    private CloseAction closeNow = null;        // run at the start of the next update
    private String unsavedPopupError = null;    // shown in the popup when Save All fails

    private boolean hasUnsavedChanges() {
        return editorMode == EditorMode.EDITOR && editorContext.unsavedChanges != null && editorContext.unsavedChanges.any();
    }

    /** While playing, the world holds the played state: saving it would save that instead of the edits. */
    private boolean refuseWhilePlaying() {
        if (!editorContext.playSession.isPlaying()) return false;
        logger.Logger.warn("Stop playing before saving");
        return true;
    }

    /** Stops Play first (so the world is the edited one again), then asks if anything is unsaved; otherwise closes. */
    private void requestClose(CloseAction action) {
        editorContext.playSession.stop(editorContext);
        if (hasUnsavedChanges()) {
            pendingClose = action;
            openUnsavedPopup = true;
            unsavedPopupError = null;
        } else {
            closeNow = action;
        }
    }

    private void runClose(CloseAction action) {
        switch (action) {
            case CLOSE_PROJECT: closeProject(); break;
            case QUIT: context.close(); break;
        }
    }

    /** Back to the launcher, closing the project's "res" pool so the next project can create its own. */
    private void closeProject() {
        initLauncher();
        Project project = editorContext.project;
        if (project != null && project.projectAssets != null) project.projectAssets.close();
        editorContext.project = null;
        editorContext.projectAssets = null;
        if (editorContext.history != null) editorContext.history.clear();   // frees objects only the history held
        editorContext.history = null;
        editorContext.unsavedChanges = null;
        editorContext.worldSavable = null;
    }

    private void renderUnsavedPopup() {
        if (pendingClose == null) return;   // nothing asked
        UnsavedChanges changes = editorContext.unsavedChanges;
        String question = pendingClose == CloseAction.QUIT ? "Save changes before quitting?" : "Save changes before closing the project?";

        SImGui.SaveChoice choice = SImGui.unsavedChangesModal(UNSAVED_POPUP, openUnsavedPopup, question,
            changes == null ? List.of() : changes.displayNames(), unsavedPopupError, this::saveAllBeforeClose);
        openUnsavedPopup = false;

        switch (choice) {
            case SAVED, DONT_SAVE -> { closeNow = pendingClose; pendingClose = null; }
            case CANCEL -> pendingClose = null;
            case NONE -> {}
        }
    }

    /** Save All from the popup: true closes it; on failure the items stay dirty and the popup shows why. */
    private boolean saveAllBeforeClose() {
        UnsavedChanges changes = editorContext.unsavedChanges;
        if (changes == null) return true;
        List<String> failed = changes.saveAll();
        afterSave();
        for (String failure : failed) logger.Logger.error("Can't save " + failure);
        unsavedPopupError = failed.isEmpty() ? null : "Couldn't save:\n" + String.join("\n", failed);
        return failed.isEmpty();
    }

    /** "My Project* - Simplicity" while something is unsaved; only sets the title when it changes. */
    private void updateTitle() {
        Project project = editorContext.project;
        if (project == null) return;
        boolean unsaved = editorContext.unsavedChanges != null && editorContext.unsavedChanges.any();
        String title = project.projectData.projectName + (unsaved ? "*" : "") + " - Simplicity";
        Window window = context.window();
        if (!title.equals(window.getTitle())) window.setTitle(title);
    }

    @Override
    protected void onRenderEditor(RenderContext renderContext) {
        switch (editorMode) {
            case EDITOR:
                for (SimplicityPanel panel : panels) panel.onRender(renderContext);
                renderUnsavedPopup();
                // ImGui.showDemoWindow();
                break;
            case LAUNCHER: 
                if (launcher != null) launcher.onRender(renderContext);
                break;
        }
    }

    @Override
    protected void onUpdateEditor(float dt) {
        if (closeNow != null) {
            CloseAction action = closeNow;
            closeNow = null;
            runClose(action);
            return;
        }
        switch (editorMode) {
            case EDITOR: 
                EditorSelection gameObjectsSelection = editorContext.gameObjectSelection;
                if (!gameObjectsSelection.isNoSelection()) {
                    gameObjectsSelection.selectedGameObjects.removeIf(go -> go == null || go.isDead());
                }
                for (SimplicityPanel panel : panels) panel.onUpdate(dt);
                updateTitle();
                break;
            case LAUNCHER:
                if (launcher != null) launcher.onUpdate(dt);
                break;
        }
    }
    
    @Override
    protected void onDestroyEditor() {
        if (launcher != null) {
            launcher.destroy();
            launcher = null;
        }
        for (SimplicityPanel panel : panels) panel.destroy();
        panels.clear();
    }

    @Override
    protected void onNotifyEditor(Event event) {
        switch (editorMode) {
            case EDITOR:;
                for (SimplicityPanel panel : panels) panel.onEvent(event);
                break;
            case LAUNCHER:
                if (launcher != null) launcher.onEvent(event);   // none while switching modes
                break;
        }
        if (event.type == KeyInput) {
            KeyEvent keyEvent = ((KeyEvent) event);
            boolean ctrl = (keyEvent.mods & Inputs.MOD_CONTROL) != 0;
            if (editorMode == EditorMode.EDITOR && ctrl && keyEvent.key == Inputs.KEY_S && keyEvent.action == Inputs.KEY_PRESS) {
                if ((keyEvent.mods & Inputs.MOD_SHIFT) != 0) saveAll();
                else save();
            }
            if (editorMode == EditorMode.EDITOR) handleEditShortcuts(keyEvent);
        }
        if (event instanceof WindowCloseEvent close && hasUnsavedChanges()) {
            close.cancel();                        // keep the window open and ask; QUIT closes it afterwards
            requestClose(CloseAction.QUIT);
        }
    }

    /**
     * Ctrl+Z undo, Ctrl+Y / Ctrl+Shift+Z redo (both repeat while held), Ctrl+D duplicate, Delete delete.
     * Skipped while a text field has the keyboard: there, these keys edit the text (ImGui has its own text undo).
     */
    private void handleEditShortcuts(KeyEvent keyEvent) {
        if (keyEvent.action == Inputs.KEY_RELEASE || ImGui.getIO().getWantTextInput()) return;
        boolean ctrl = (keyEvent.mods & Inputs.MOD_CONTROL) != 0;
        boolean shift = (keyEvent.mods & Inputs.MOD_SHIFT) != 0;
        boolean press = keyEvent.action == Inputs.KEY_PRESS;

        if (ctrl && keyEvent.key == Inputs.KEY_Z) {
            if (shift) editorContext.redo();
            else editorContext.undo();
        } else if (ctrl && keyEvent.key == Inputs.KEY_Y) {
            editorContext.redo();
        } else if (ctrl && press && keyEvent.key == Inputs.KEY_D) {
            editorContext.duplicateSelected();
        } else if (!ctrl && press && keyEvent.key == Inputs.KEY_DELETE) {
            editorContext.deleteSelected();
        }
    }

    @Override
    protected boolean rendersWorldAsImage() {
        switch (editorMode) {
            case EDITOR: return viewport != null;
            case LAUNCHER: return false;
        }
        return  false;
    }

    @Override
    protected boolean shouldRenderDefaultDockspace() {
        switch (editorMode) {
            case EDITOR: return true;
            case LAUNCHER: return false;
        }
        return  true;
    }

    @Override
    protected boolean isMouseOverWorld() {
        return viewport != null && viewport.isHovered();
    }

    @Override
    protected void onRenderMenuBar() {
        if(ImGui.beginMenu("Project")) {

            if(ImGui.menuItem("Save", "Ctrl+S", false, editorContext.worldSavable != null && !editorContext.playSession.isPlaying())) {
                save();
            }

            UnsavedChanges changes = editorContext.unsavedChanges;
            if(ImGui.menuItem("Save All", "Ctrl+Shift+S", false, changes != null && changes.any() && !editorContext.playSession.isPlaying())) {
                saveAll();
            }

            if(ImGui.menuItem("Save As")) {

            }

            if(ImGui.menuItem("Load")) {
                // String result = IOHelper.openSingle(context.window(), "png");
                // Asset<Texture> texture = projectAssets.get(result, Texture.class);
                // projectAssets.acquireAsync(texture);
            }

            if(ImGui.menuItem("Close")) {
                requestClose(CloseAction.CLOSE_PROJECT);
            }

            ImGui.endMenu();
        }

        if(ImGui.beginMenu("Edit")) {
            UndoHistory history = editorContext.history;
            boolean canEdit = editorContext.canEdit();
            String undoName = history == null ? null : history.undoName();
            String redoName = history == null ? null : history.redoName();

            if (ImGui.menuItem(undoName == null ? "Undo" : "Undo " + undoName, "Ctrl+Z", false, canEdit && undoName != null)) {
                editorContext.undo();
            }
            if (ImGui.menuItem(redoName == null ? "Redo" : "Redo " + redoName, "Ctrl+Y", false, canEdit && redoName != null)) {
                editorContext.redo();
            }
            ImGui.separator();
            boolean hasSelection = !editorContext.selectedObjects().isEmpty();
            if (ImGui.menuItem("Duplicate", "Ctrl+D", false, canEdit && hasSelection)) {
                editorContext.duplicateSelected();
            }
            if (ImGui.menuItem("Delete", "Delete", false, canEdit && hasSelection)) {
                editorContext.deleteSelected();
            }

            ImGui.endMenu();
        }

        if(ImGui.beginMenu("Editor")) {

            if(ImGui.beginMenu("Preferences")) {

                if(ImGui.beginMenu("Theme")) {

                    if (ImGui.menuItem("Default")) {
                        setUIColors(5);
                    }

                    if (ImGui.menuItem("Classic")) {
                        setUIColors(0);
                    }

                    if (ImGui.menuItem("Dark blue")) {
                        setUIColors(1);
                    }

                    if (ImGui.menuItem("Dark purple")) {
                        setUIColors(2);
                    }

                    if (ImGui.menuItem("Light")) {
                        setUIColors(3);
                    }

                    if (ImGui.menuItem("Gray")) {
                        setUIColors(4);
                    }

                    ImGui.endMenu();
                }

                ImGui.endMenu();
            }

            ImGui.endMenu();
        }
    }
}
