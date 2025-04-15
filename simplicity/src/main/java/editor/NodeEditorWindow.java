package editor;

import imgui.ImVec2;
import imgui.extension.nodeditor.NodeEditor;
import imgui.extension.nodeditor.NodeEditorConfig;
import imgui.extension.nodeditor.NodeEditorContext;
import imgui.extension.nodeditor.flag.NodeEditorPinKind;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.internal.ImGui;
import imgui.type.ImLong;

import editor.nodes.Graph;
import editor.nodes.Graph.GraphTuple;

public class NodeEditorWindow extends ImGuiInterface {

    /*  TODO:
     *  FIX THIS
     *  FIX LINKING
     */

    private static final NodeEditorContext CONTEXT;

    static {
        NodeEditorConfig config = new NodeEditorConfig();
        config.setSettingsFile(null);
        CONTEXT = new NodeEditorContext(config);
    }

    private final Graph graph = new Graph();

    @Override
    public void imgui() {
        // ImGui.setNextWindowSize(500, 400, ImGuiCond.Once);
        // ImGui.setNextWindowPos(ImGui.getMainViewport().getPosX() + 100, ImGui.getMainViewport().getPosY() + 200, ImGuiCond.Once);
        if (ImGui.begin("Node Editor", ImGuiWindowFlags.MenuBar)) {
            updateCalc();
            // ImGui.text("This a demo graph editor for imgui-node-editor");

            // ImGui.alignTextToFramePadding();
            // ImGui.text("Repo:");
            // ImGui.sameLine();
            
            menuBar();

            // if (ImGui.button("Center to content")) {
            //     NodeEditor.navigateToContent(1);
            // }

            NodeEditor.setCurrentEditor(CONTEXT);
            NodeEditor.begin("Node Editor Viewport");

            for (Graph.GraphNode node : graph.nodes.values()) {
                NodeEditor.beginNode(node.nodeId);
                ImGui.text(node.getName());
                int max = Math.max(node.getInputPinId().length, node.getOutputPinId().length);

                for (int i = 0; i < max; i++) {

                    if (i < node.getInputPinId().length) {
                        NodeEditor.beginPin(node.getInputPinId()[i], NodeEditorPinKind.Input);
                        ImGui.text("-> In");
                        NodeEditor.endPin();
                    } else {
                        ImGui.textColored(0, 0, 0, 0, "-> In");
                    }
                    
                    ImGui.sameLine();

                    if (i < node.getOutputPinId().length) {
                        NodeEditor.beginPin(node.getOutputPinId()[i], NodeEditorPinKind.Output);
                        ImGui.text("Out ->");
                        NodeEditor.endPin();
                    } else {
                        ImGui.textColored(0, 0, 0, 0, "Out ->");
                    }
                }

                NodeEditor.endNode();
            }

            if (NodeEditor.beginCreate()) {
                final ImLong a = new ImLong();
                final ImLong b = new ImLong();
                if (NodeEditor.queryNewLink(a, b)) {
                    GraphTuple sourceNode = graph.findByOutput(a.get());
                    GraphTuple targetNode = graph.findByInput(b.get());
                    if (sourceNode != null && targetNode != null && sourceNode.node.outputNodeId[sourceNode.pinIndex] != targetNode.node.nodeId && NodeEditor.acceptNewItem()) {
                        sourceNode.node.outputNodeId[sourceNode.pinIndex] = targetNode.node.nodeId;
                        sourceNode.node.outputNodePinId[sourceNode.pinIndex] = targetNode.pinId;
                    }
                }
            }
            NodeEditor.endCreate();

            int uniqueLinkId = 1;
            for (Graph.GraphNode node : graph.nodes.values()) {
                for (int a = 0; a < node.outputNodeId.length; a++) {
                    if (graph.nodes.containsKey(node.outputNodeId[a])) {
                        int outputId = node.getOutputPinId()[a];
                        int inputId = node.outputNodePinId[a];
                        NodeEditor.link(uniqueLinkId++, outputId, inputId);
                    }
                }
            }

            NodeEditor.suspend();

            final long nodeWithContextMenu = NodeEditor.getNodeWithContextMenu();
            if (nodeWithContextMenu != -1) {
                ImGui.openPopup("node_context");
                ImGui.getStateStorage().setInt(ImGui.getID("delete_node_id"), (int) nodeWithContextMenu);
            }

            if (ImGui.isPopupOpen("node_context")) {
                final int targetNode = ImGui.getStateStorage().getInt(ImGui.getID("delete_node_id"));
                if (ImGui.beginPopup("node_context")) {
                    if (ImGui.button("Delete " + graph.nodes.get(targetNode).getName())) {
                        graph.nodes.remove(targetNode);
                        ImGui.closeCurrentPopup();
                    }
                    ImGui.endPopup();
                }
            }

            if (NodeEditor.showBackgroundContextMenu()) {
                ImGui.openPopup("node_editor_context");
            }

            if (ImGui.beginPopup("node_editor_context")) {
                if (ImGui.button("Create New Node")) {
                    final Graph.GraphNode node = graph.createGraphNode();
                    final float canvasX = NodeEditor.toCanvasX(ImGui.getMousePosX());
                    final float canvasY = NodeEditor.toCanvasY(ImGui.getMousePosY());
                    NodeEditor.setNodePosition(node.nodeId, canvasX, canvasY);
                    ImGui.closeCurrentPopup();
                }
                ImGui.endPopup();
            }

            NodeEditor.resume();
            NodeEditor.end();
        }
        ImGui.end();
    }

    private void menuBar() {
        ImGui.beginMenuBar();
        if(ImGui.menuItem("Find", "", false, true)) {
            NodeEditor.navigateToContent(1);
        }
        ImGui.endMenuBar();
    }

    @Override
    public void destroy() {
        
    }
}
