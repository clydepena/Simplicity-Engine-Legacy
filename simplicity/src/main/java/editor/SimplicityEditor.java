package editor;

import editor.Project.ProjectFile;
import editor.panels.LauncherPanel;
import editor.panels.LoggerPanel;
import editor.panels.NodeEditorPanel;
import editor.panels.SimplicityPanel;
import editor.panels.ViewportPanel;

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
        initEditor();
    }

    /** Save: writes the open world, dirty or not (Ctrl+S always writes, as in most editors). */
    private void save() {
        WorldSavable world = editorContext.worldSavable;
        if (world == null) {
            logger.Logger.warn("Nothing to save: this world has no file");
            return;
        }
        try {
            editorContext.unsavedChanges.save(world);
            logger.Logger.info("Saved '" + world.displayName() + "'");
        } catch (Exception e) {
            logger.Logger.error("Can't save '" + world.displayName() + "': " + e.getMessage());
        }
    }

    /** Save All: everything with unsaved changes; whatever fails stays dirty, so it can be saved again. */
    private void saveAll() {
        UnsavedChanges changes = editorContext.unsavedChanges;
        if (changes == null || !changes.any()) return;
        List<String> failed = changes.saveAll();
        for (String failure : failed) logger.Logger.error("Can't save " + failure);
        if (failed.isEmpty()) logger.Logger.info("Saved all changes");
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
                // ImGui.showDemoWindow();
                break;
            case LAUNCHER: 
                if (launcher != null) launcher.onRender(renderContext);
                break;
        }
    }

    @Override
    protected void onUpdateEditor(float dt) {
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
            if ((keyEvent.key == Inputs.KEY_DELETE && keyEvent.action == Inputs.KEY_RELEASE)) {
                switch (editorMode) {
                    case EDITOR: initLauncher(); break;
                    case LAUNCHER: initEditor(); break;
                }
            }
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

            if(ImGui.menuItem("Save", "Ctrl+S", false, editorContext.worldSavable != null)) {
                save();
            }

            UnsavedChanges changes = editorContext.unsavedChanges;
            if(ImGui.menuItem("Save All", "Ctrl+Shift+S", false, changes != null && changes.any())) {
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
                initLauncher();
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
