package editor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector4f;

import components.Sprite;
import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiSelectableFlags;
import imgui.flag.ImGuiTreeNodeFlags;
import logger.Log;
import logger.Logger;
import renderer.Texture;
import util.AssetPool;
import util.IOHelper;
import util.Resources;

public class FileExplorerWIndow extends ImGuiInterface{

    private Texture folderIcon, folderIconOpen, fileIcon;
    private Vector4f folderC;
    private Vector4f fileC;
    private float indent;
    private Map<Integer, Node> nodes;
    private static int globalId = 0, renderId = 0;
    private String data = "";
    private boolean once = true;

    public FileExplorerWIndow() {
        this.folderIcon = AssetPool.getTextureFromRes(Resources.Editor.SPRITE_FOLDER);
        this.folderIconOpen = AssetPool.getTextureFromRes(Resources.Editor.SPRITE_FOLDER_OPEN);
        this.fileIcon = AssetPool.getTextureFromRes(Resources.Editor.SPRITE_FILE);
        this.folderC = new Vector4f(1.00f, 0.49f, 0.00f, 1.00f);
        this.fileC = new Vector4f(0.77f, 0.38f, 0.00f, 1.00f);
        this.nodes = new HashMap<>();

    }

    @Override
    public void imgui() {
        // ImGui.setNextWindowSize(500, 500, ImGuiCond.Once);
        ImGui.begin("Project Explorer");

        if (once) {
            indent = ImGui.calcTextSize("BIG").x;
            once = false;
        }

        updateCalc();

        if (ImGui.button("Recurse")) {
            globalId = 0;
            nodes.clear();
            startRecurse(System.getProperty("user.dir"));
        }

        ImGui.sameLine();

        if (ImGui.button("Flush All")) {
            globalId = 0;
            nodes.clear();
            data = "";
        }

        ImGui.sameLine();

        if (ImGui.button("print")) {
            StringBuilder builder = new StringBuilder();
            builder.append("\nNodes: " + globalId);
            builder.append("\nMap Size: " + nodes.size());
    
            for (Node n : nodes.values()) {
                builder.append("\n" + n.toString());
            }
            data = builder.toString();
        }

        displayFileTree();
        ImGui.text(data);
        ImGui.end();
    }


    private void recurseFolders(String folderpath, String rootName, Node prevNode) {
        File[] dirContents = new File(folderpath).listFiles();

        if (dirContents == null) {
            return;
        }

        int dirs = 0;
        List<Node> nodeList = new ArrayList<>();
        List<File> dirFiles = new ArrayList<>();
        for (File file : dirContents) {
            if (file.isDirectory()) {
                Node currNode = new Node();
                nodes.put(currNode.nodeID, currNode);

                currNode.name = file.getName();
                currNode.setParent(prevNode);
                prevNode.addChild(currNode);

                nodeList.add(currNode);
                dirFiles.add(file);
                dirs++;
            } else {
                Node fileNode = new Node();
                fileNode.name = file.getName();
                fileNode.path = file.getAbsolutePath();
                fileNode.isLeaf = true;
                fileNode.setParent(prevNode);
                prevNode.addChild(fileNode);
                nodes.put(fileNode.nodeID, fileNode);
            }
        }

        if (dirs <= 0) {
            return;
        }

        for (int i = 0; i < dirs; i++) {
            recurseFolders(dirFiles.get(i).getAbsolutePath(), dirFiles.get(i).getName(), nodeList.get(i));
        }
    }

    private void startRecurse(String folderpath) {
        Node rootNode = new Node();
        nodes.put(rootNode.nodeID, rootNode);

        String name = new File(folderpath).getName();
        rootNode.name = name;
        recurseFolders(folderpath, name, rootNode);
    }

    private void displayAsFolder(Node node) {
        if (folderIcon != null) {
            ImGuiStyle style = ImGui.getStyle();
            float size = (ImGui.calcTextSize(node.name).y - style.getFramePaddingY());
            int texId = node.isOpen ? folderIconOpen.getId() : folderIcon.getId();

            ImGui.image(texId, size, size, 0f, 1f, 1f, 0f, folderC.x, folderC.y, folderC.z, folderC.w);
            ImGui.sameLine();
            if (ImGui.selectable(node.name)) {
                node.isOpen = !node.isOpen;
                // Logger.info(node.name + " is open " + node.isOpen);
            }
        } else {
            ImGui.text(node.name);
        }
    }

    private void displayAsFile(Node node) {
        if (fileIcon != null) {
            ImGuiStyle style = ImGui.getStyle();
            float size = (ImGui.calcTextSize(node.name).y - style.getFramePaddingY());
            int texId = fileIcon.getId();

            ImGui.image(texId, size, size, 0f, 1f, 1f, 0f, fileC.x, fileC.y, fileC.z, fileC.w);
            ImGui.sameLine();
            if (ImGui.selectable(node.name)) {
                Logger.info(node.path);
            }

        } else {
            ImGui.text(node.name);
        }
    }

    private void displayFileTree() {
        if (!nodes.isEmpty()) {
            // ImGui.beginChild("##Tree##");
            ImGui.text("Root Folder");
            recurseDisplay(indent, nodes.get(0));
            renderId = 0;
            // ImGui.endChild();
        }
    }

    private void recurseDisplay(float x, Node prevNode) {
        ImGui.setCursorPosX(ImGui.getCursorPosX() + x);
        renderId++;
        prevNode.imgui();
        if (prevNode.children.isEmpty() || !prevNode.isOpen || prevNode.isLeaf || renderId > 700) {
            return;
        }
        for (int i = 1; i <= prevNode.children.size(); i++) {
            Node child = prevNode.children.get(i - 1);
            recurseDisplay(x + (indent), child);
        }
    }

    @Override
    public void destroy() {
        
    }

    private class Node {
        public final int nodeID;
        private Node parent;
        private List<Node> children;
        public boolean isOpen;
        public boolean isLeaf = false;
        public String name;
        public String path;

        public Node() {
            this.nodeID = globalId++;
            this.name = "Gen " + nodeID;
            this.isOpen = false;
            this.children = new ArrayList<>();
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        public void addChild(Node child) {
            if (!isLeaf) {
                this.children.add(child);
            }
        }

        public void imgui() {
            if (isLeaf) {
                displayAsFile(this);
            } else {
                displayAsFolder(this);
            }
        }

        @Override
        public String toString() {
            return "[" + nodeID + "], " + name + ", Parent: " + (parent == null ? -1 : parent.name);
        }
        
    }
    
}
