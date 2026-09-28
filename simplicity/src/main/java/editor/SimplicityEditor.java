package editor;

import editor.panels.EditorPanel;
import editor.panels.EditorSubPanel;
import editor.panels.LoggerPanel;
import editor.panels.NodeEditorPanel;
import editor.panels.ViewportPanel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import asset.AssetPoolHandler;
import imgui.ImGui;
import logger.Log;
import logger.Logger;
import observers.events.Event;
import scenes.World2DLayer;
import simplicity.GameObject;
import simplicity.Tasks;
import simplicity.Application.RenderContext;
import util.IOHelper;

public class SimplicityEditor extends ImGuiEditorLayer {

    public static class SimplicityEditorContext extends editor.EditorContext<SimplicityEditor> {
        public final World2DLayer world;
        public final EditorSelection gameObjectSelection;

        public SimplicityEditorContext(SimplicityEditor editorLayer, World2DLayer world, EditorSelection gameObjectSelection) {
            super(editorLayer);
            this.world = world;
            this.gameObjectSelection = gameObjectSelection;
        }
    }

    public static abstract class SimplicityPanel extends EditorPanel<SimplicityEditor, SimplicityEditorContext> {

        public SimplicityPanel(SimplicityEditorContext editorContext) {
            super(editorContext);
        }
    }

    public static abstract class SimplicitySubPanel<P extends SimplicityPanel> extends EditorSubPanel<SimplicityEditor, SimplicityEditorContext, P> {

        public SimplicitySubPanel(P parentPanel) {
            super(parentPanel);
        }
    }

    public static class EditorSelection {
        
        public enum Mode { REPLACE, ADD, TOGGLE, SUBTRACT }

        public List<GameObject> selectedGameObjects = new ArrayList<>();

        public EditorSelection() {}

        public boolean isSingleSelection() {
            return isNoSelection() ? false: selectedGameObjects.stream().filter(Objects::nonNull).count() == 1;
        }

        public boolean isNoSelection() {
            return selectedGameObjects == null ? true : selectedGameObjects.stream().noneMatch(Objects::nonNull);
        }

        /** How a pick changes the selection. */

        public void apply(Mode mode, Collection<GameObject> objects) {
            switch (mode) {
                case REPLACE -> set(objects);
                case ADD -> add(objects);
                case TOGGLE -> toggle(objects);
                case SUBTRACT -> remove(objects);
            }
        }

        public void set(Collection<GameObject> objects) {
            list().clear();
            add(objects);
        }

        public void add(Collection<GameObject> objects) {
            List<GameObject> list = list();
            for (GameObject go : objects) {
                if (go != null && !list.contains(go)) list.add(go);
            }
        }

        public void remove(Collection<GameObject> objects) {
            list().removeAll(objects);
        }

        public void toggle(Collection<GameObject> objects) {
            List<GameObject> list = list();
            for (GameObject go : objects) {
                if (go != null && !list.remove(go)) list.add(go);
            }
        }

        public void clear() {
            list().clear();
        }

        private List<GameObject> list() {
            if (selectedGameObjects == null) selectedGameObjects = new ArrayList<>();
            return selectedGameObjects;
        }
    }

    private World2DLayer world = null;
    private EditorSelection gameObjectsSelection = null;
    private SimplicityEditorContext editorContext = null;
    private ViewportPanel viewport  = null;
    private final List<SimplicityPanel> panels = new ArrayList<>();

    @Override
    protected void onInitEditor() {
        World2DLayer world = context.getLayer(World2DLayer.class);
        if (world == null) throw new IllegalStateException(this.getClass().getSimpleName() + " requires a World2DLayer to be pushed first");
        world.setFrozen(true);
        this.world = world;
        
        this.gameObjectsSelection = new EditorSelection();
    
        editorContext = new SimplicityEditorContext(this, world, gameObjectsSelection);
        viewport = new ViewportPanel(editorContext);
        panels.add(viewport);
        
        panels.add(new LoggerPanel(editorContext));
        // panels.add(new NodeEditorPanel(editorContext));
    }

    @Override
    protected void onRenderEditor(RenderContext renderContext) {
        for (SimplicityPanel panel : panels) panel.onRender(renderContext);
        ImGui.showDemoWindow();
    }

    @Override
    protected void onUpdateEditor(float dt) {
        if (!gameObjectsSelection.isNoSelection()) {
            gameObjectsSelection.selectedGameObjects.removeIf(go -> go == null || go.isDead());
        }

        for (SimplicityPanel panel : panels) panel.onUpdate(dt);
    }
    
    @Override
    protected void onDestroyEditor() {
        for (SimplicityPanel panel : panels) panel.destroy();
        panels.clear();
    }

    @Override
    protected void onNotifyEditor(Event event) {
        for (SimplicityPanel panel : panels) panel.onEvent(event);
    }

    @Override
    protected boolean rendersWorldAsImage() {
        // both viewport modes draw the world's frame as an image, so ImGui always renders into its own frame
        return viewport != null;
    }

    @Override
    protected boolean isMouseOverWorld() {
        return viewport != null && viewport.isHovered();
    }

    protected void resetAssetPools() {
        
    }

    @Override
    protected void onRenderMenuBar() {
        if(ImGui.beginMenu("File")) {

            // if(ImGui.menuItem("Save", OldWindow.getScene().getFilename())) {
            //     EventSystem.notify(new Event(EventType.SaveLevel));
            // }

            // if(ImGui.menuItem("Save As")) {
            //     EventSystem.notify(new Event(EventType.SaveLevelAs));
            // }

            if(ImGui.menuItem("Load")) {
                String result = IOHelper.openSingle(context.window(), "png");
                System.out.println(result);
                System.out.println(result.replace("\\", "/"));
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
