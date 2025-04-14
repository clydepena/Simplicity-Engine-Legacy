package editor;

import java.util.List;

import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import simplicity.GameObject;
import simplicity.Window;

public class SceneHierarchyWindow extends ImGuiInterface {

    private static String playLoadDragDropType = "SceneHierarchy";
    
    public void imgui() {
        ImGui.begin("Scene Hierarchy");
        updateCalc();
        List<GameObject> gameObjects = Window.getScene().getGameObjectList();
        int index = 0;
        for (GameObject go : gameObjects) {
            if (!go.doSerialization()) {
                continue;
            }
            boolean treeNodeOpen = doTreeNode(go, index);
            if(treeNodeOpen) {
                ImGui.treePop();
            }
            index++;
        }
        ImGui.end();
    }

    private boolean doTreeNode(GameObject go, int index) {
        ImGui.pushID(index);
        boolean treeNodeOpen = ImGui.treeNodeEx(
            go.name, 
            ImGuiTreeNodeFlags.DefaultOpen | 
            ImGuiTreeNodeFlags.FramePadding | 
            ImGuiTreeNodeFlags.OpenOnArrow | 
            ImGuiTreeNodeFlags.SpanAvailWidth,
            go.name
        );
        ImGui.popID();
        if (ImGui.beginDragDropSource()) {
            ImGui.setDragDropPayload(playLoadDragDropType, go);

            ImGui.text(go.name);

            ImGui.endDragDropSource();
        }
        if (ImGui.beginDragDropTarget()) {
            Object payloadObj = ImGui.acceptDragDropPayload(playLoadDragDropType);
            if (payloadObj != null) {
                if (payloadObj.getClass().isAssignableFrom(GameObject.class)) {
                    GameObject playerGameObj = (GameObject) payloadObj;

                }
            }

            ImGui.endDragDropTarget();
        }

        return treeNodeOpen;
    }

    @Override
    public void destroy() {
        
    }
}
