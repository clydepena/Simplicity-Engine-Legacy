package editor.panels;

import java.util.List;

import org.joml.Vector2f;

import editor.EditorSelection;
import editor.SimplicityEditorContext;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiPopupFlags;
import imgui.flag.ImGuiTreeNodeFlags;
import observers.events.Event;
import simplicity.Application.RenderContext;
import simplicity.Camera;
import simplicity.GameObject;

/**
 * The world's objects as a list. Click selects (Ctrl toggles, Shift adds), right-click opens Duplicate / Delete,
 * right-click on empty space creates an object. Objects that aren't saved with the world are greyed out.
 */
public class HierarchyPanel extends SimplicityPanel {

    private Runnable pendingAction = null;   // menu actions change the object list, so they run after the loop

    public HierarchyPanel(SimplicityEditorContext editorContext) {
        super(editorContext);
    }

    @Override
    public void onRender(RenderContext renderContext) {
        ImGui.begin("Hierarchy");
        updateCalc();

        EditorSelection selection = editorContext.gameObjectSelection;
        List<GameObject> objects = editorContext.world.getGameObjectList();
        boolean canEdit = editorContext.canEdit();

        for (int i = 0; i < objects.size(); i++) {
            GameObject go = objects.get(i);
            boolean selected = selection != null && selection.selectedGameObjects.contains(go);

            ImGui.pushID(i);
            int flags = ImGuiTreeNodeFlags.Leaf | ImGuiTreeNodeFlags.NoTreePushOnOpen | ImGuiTreeNodeFlags.SpanAvailWidth;
            if (selected) flags |= ImGuiTreeNodeFlags.Selected;
            if (!go.doSerialization()) ImGui.pushStyleColor(ImGuiCol.Text, 0.6f, 0.6f, 0.6f, 1.0f);
            ImGui.treeNodeEx(go.name == null || go.name.isEmpty() ? "(unnamed)" : go.name, flags);
            if (!go.doSerialization()) ImGui.popStyleColor();

            if (selection != null && ImGui.isItemClicked(ImGuiMouseButton.Left)) {
                boolean ctrl = ImGui.getIO().getKeyCtrl(), shift = ImGui.getIO().getKeyShift();
                EditorSelection.Mode mode = ctrl ? EditorSelection.Mode.TOGGLE : shift ? EditorSelection.Mode.ADD : EditorSelection.Mode.REPLACE;
                selection.apply(mode, List.of(go));
            }

            if (ImGui.beginPopupContextItem()) {
                // acting on an unselected object acts on it alone, as in most editors
                if (selection != null && !selected) selection.set(List.of(go));
                if (ImGui.menuItem("Duplicate", "Ctrl+D", false, canEdit)) pendingAction = editorContext::duplicateSelected;
                if (ImGui.menuItem("Delete", "Delete", false, canEdit)) pendingAction = editorContext::deleteSelected;
                ImGui.endPopup();
            }
            ImGui.popID();
        }

        // a click on empty space clears the selection; a right-click there offers to create an object
        if (selection != null && ImGui.isWindowHovered() && !ImGui.isAnyItemHovered() && ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            selection.clear();
        }
        if (ImGui.beginPopupContextWindow("HierarchyEmpty", ImGuiPopupFlags.MouseButtonRight | ImGuiPopupFlags.NoOpenOverItems)) {
            if (ImGui.menuItem("Create Empty", "", false, canEdit)) pendingAction = this::createEmpty;
            ImGui.endPopup();
        }

        ImGui.end();

        if (pendingAction != null) {
            Runnable action = pendingAction;
            pendingAction = null;
            action.run();
        }
    }

    /** An object with only a Transform, in the middle of the view. */
    private void createEmpty() {
        GameObject go = editorContext.world.createGameObject("Empty");
        Camera view = editorContext.world.renderCamera();
        if (view != null) {
            Vector2f center = view.viewportToWorld(0.5f, 0.5f);
            go.transform.position.set(center);
        }
        editorContext.addObjects("Create 'Empty'", List.of(go));
    }

    @Override
    public void onUpdate(float dt) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void onEvent(Event event) {
    }
}
