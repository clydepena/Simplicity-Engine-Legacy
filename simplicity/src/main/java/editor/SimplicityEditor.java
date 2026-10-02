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

import asset.AssetHelpers;
import asset.AssetPoolHandler;
import imgui.ImGui;
import observers.events.Event;
import scenes.World2DLayer;
import simplicity.Application.RenderContext;
import simplicity.KeyListener.KeyEvent;
import util.IOHelper;
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
        AssetHelpers.setCodecs(assetPoolHandler);   // loaders for png, glsl, sheet, ogg: needed before anything loads
        editorContext.engineResources = assetPoolHandler.createAssetPool("engine", null, AssetPoolHandler.FileReadingCallback.CLASSPATH);

        initEngineResources();
        
        
        Project project = new Project(
            Path.of("C:/"),
            new ProjectFile(),
            assetPoolHandler.createAssetPool("res", "C:/", AssetPoolHandler.FileReadingCallback.FILE_SYSTEM)
        );
        setProject(project);
    }

    protected void initEngineResources() {
        editorContext.icons = new EditorIcons(editorContext.assetPoolHandler);
    }
    
    protected void initLancher() {
        onDestroyEditor();
        context.window().setTitle("Simplicity Launcher");
        launcher = new LauncherPanel(editorContext);
        editorContext.world.setActive(false);
        editorContext.gameObjectSelection = null;
        editorMode = EditorMode.LAUNCHER;
    }
    
    protected void initEditor() {
        onDestroyEditor();
        editorContext.world.setFrozen(true);
        editorContext.world.setActive(true);
        editorContext.gameObjectSelection = new EditorSelection();
        viewport = new ViewportPanel(editorContext);
        panels.add(new NodeEditorPanel(editorContext));
        panels.add(viewport);
        panels.add(new LoggerPanel(editorContext));
        editorMode = EditorMode.EDITOR;
    }

    public void setProject(Project project) {
        editorContext.projectAssets = project.projectAssets;
        editorContext.project = project;
        context.window().setTitle(project.projectData.projectName + " - " + "Simplicity");
        initEditor();
    }

    @Override
    protected void onRenderEditor(RenderContext renderContext) {
        switch (editorMode) {
            case EDITOR:
                for (SimplicityPanel panel : panels) panel.onRender(renderContext);
                // ImGui.showDemoWindow();
                break;
            case LAUNCHER: 
                launcher.onRender(renderContext);
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
                break;
            case LAUNCHER:
                launcher.onUpdate(dt);
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
                launcher.onEvent(event);
                break;
        }
        if (event.type == KeyInput) {
            KeyEvent keyEvent = ((KeyEvent) event);
            if ((keyEvent.key == Inputs.KEY_DELETE && keyEvent.action == Inputs.KEY_RELEASE)) {
                switch (editorMode) {
                    case EDITOR: initLancher(); break;
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
        if(ImGui.beginMenu("File")) {

            if(ImGui.menuItem("Save")) {
                Project project = editorContext.project;
                Path path = Path.of(IOHelper.saveFile(context.window(), project.projectData.projectName, "simplicity"));
                try {
                    project.projectData.write(path);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println(path);
            }

            if(ImGui.menuItem("Save As")) {

            }

            if(ImGui.menuItem("Load")) {
                // String result = IOHelper.openSingle(context.window(), "png");
                // Asset<Texture> texture = projectAssets.get(result, Texture.class);
                // projectAssets.acquireAsync(texture);
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
