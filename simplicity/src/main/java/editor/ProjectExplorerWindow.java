package editor;

import java.io.File;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.ImVec4;
import imgui.flag.ImGuiButtonFlags;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;
import simplicity.Window;
import util.AssetPool;
import util.IOHelper;
import util.Resources;
import util.Settings;

public class ProjectExplorerWindow extends ImGuiInterface{

    /* TODO:
     * file pop-up functionality
     * 
     */

    private final int folderIcon, folderIconOpen, fileIcon, addFolderIcon, collapseFoldersIcon, refreshIcon;
    private ImVec4 folderC, fileC;
    private final ImVec2 framePadding;
    private float indent, iconSize, time = 0;
    private int globalId = 0;
    // private int globalIdBefore = globalId;
    private boolean once = true, shouldUpdate = false, collapse = false;
    private final boolean canUseIcon;
    private FileNode rootFile, popupFile;
    private String rootPath;

    public ProjectExplorerWindow() {
        this.folderIcon = AssetPool.getTextureFromRes(Resources.Editor.SPRITE_FOLDER).getId();
        this.folderIconOpen = AssetPool.getTextureFromRes(Resources.Editor.SPRITE_FOLDER_OPEN).getId();
        this.fileIcon = AssetPool.getTextureFromRes(Resources.Editor.SPRITE_FILE).getId();
        this.addFolderIcon = AssetPool.getTextureFromRes(Resources.Editor.SPRITE_ADDFOLDER).getId();
        this.collapseFoldersIcon = AssetPool.getTextureFromRes(Resources.Editor.SPRITE_COLLAPSEFOLDERS).getId();
        this.refreshIcon = AssetPool.getTextureFromRes(Resources.Editor.SPRITE_REFRESHFILES).getId();
        // this.folderC = new Vector4f(1.00f, 0.49f, 0.00f, 1.00f);
        // this.fileC = new Vector4f(0.77f, 0.38f, 0.00f, 1.00f);
        this.folderC = new ImVec4();
        this.fileC = new ImVec4();
        this.framePadding = new ImVec2();
        this.canUseIcon = folderIcon != -1 && folderIconOpen != -1 && fileIcon != -1;
    }

    @Override
    public void imgui(float dt) {
        // ImGui.setNextWindowSize(500, 500, ImGuiCond.Once);
        ImGui.begin("Project Explorer", ImGuiWindowFlags.MenuBar);
        updateCalc();

        if (once) {
            // this.indent = ImGui.calcTextSize("B").x;
            // this.iconSize = (ImGui.calcTextSize("Folder").y - ImGui.getStyle().getFramePaddingY());
            this.indent = Settings.FONT_SIZE;
            this.iconSize = Settings.FONT_SIZE;
            ImGui.getStyle().getFramePadding(framePadding);
            this.once = false;
        }

        // ImGui.setCursorPosX(indent * 2);
        // ImGui.text(rootPath == null ? "No Folder" : rootPath);

        if (rootFile == null && popupFile != null) popupFile = null;
        popUp();
        startRecurse(dt);
        menubar();

        // if (globalId != globalIdBefore) {
        //     globalIdBefore = globalId;
        //     System.out.println(globalIdBefore);
        // }
        // if (updateTick >= (1.0f / 2)) {
        //     startRecurse(rootPath);
        //     // System.out.println("tick");
        //     updateTick = 0;
        // }


        ImGui.end();
    }

    public void popUp() {
        if (ImGui.beginPopup("file_popup")) {
            if (ImGui.selectable("\tOpen " + popupFile.getName() + "\t")) {

                
            }
            if (ImGui.selectable("\tCopy")) {

            }
            if (ImGui.selectable("\tCopy as path\t")) {
                IOHelper.CopyToClipboard(popupFile.getAbsolutePath());
            }
            ImGui.separator();
            if (ImGui.selectable("\tDelete")) {

            }
            ImGui.endPopup();
        } else if (popupFile != null) {
             popupFile = null;
        }
    }

    private void menubar() {
        ImGui.beginMenuBar();
        ImGui.text(rootFile == null ? "No project opened" : rootFile.getName());

        float size = ImGui.getFrameHeight() - ImGui.getStyle().getFramePaddingY();
        ImGui.setCursorPosY(ImGui.getCursorPosY() + (ImGui.getStyle().getFramePaddingY() * 0.5f));
        ImGui.setCursorPosX(winSize.x - (size - ImGui.getStyle().getFramePaddingX()) * 5f);

        ImVec4 color = Settings.colorsCustom[0];
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, color.x, color.y, color.z, color.w);
        ImGui.pushStyleColor(ImGuiCol.Button, 0f, 0f, 0f, 0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0f, 0f, 0f, 0f);
        if (ImGui.imageButton(addFolderIcon, size, size, 0f, 1f, 1f, 0f, ImGuiButtonFlags.MouseButtonLeft)) {
            String path = IOHelper.openFolder(Window.get());
            if (path != null) {
                rootPath = path;
                rootFile = null;
            }
        }

        if (ImGui.imageButton(refreshIcon, size, size, 0f, 1f, 1f, 0f, ImGuiButtonFlags.MouseButtonLeft)) {
            shouldUpdate = true;
        }

        if (ImGui.imageButton(collapseFoldersIcon, size, size, 0f, 1f, 1f, 0f, ImGuiButtonFlags.MouseButtonLeft)) {
            collapse = true;
        }

        ImGui.popStyleColor(3);
        ImGui.endMenuBar();
    }

    private void recurseFolders(float xOffset, FileNode prevFile) {
        globalId++;
        ImGui.setCursorPosX(xOffset);

        prevFile.display();

        if (prevFile.isFile()) return;

        if (collapse && !prevFile.isRoot) prevFile.isOpen = false;

        if (!prevFile.isOpen && !prevFile.isRoot) return;

        if (!prevFile.hasChildren()) {
            prevFile.children = prevFile.listFiles();
            if (!prevFile.hasChildren()) return;
            for (FileNode f : prevFile.children) recurseFolders(xOffset + indent, f);
            return;
        }

        if (!shouldUpdate) {
            for (FileNode f : prevFile.children) recurseFolders(xOffset + indent, f);
            return;
        }

        boolean changed = false;
        FileNode[] contents = prevFile.listFiles();
        if (prevFile.children.length != contents.length) {
            changed = true;
        } else {
            int tmp = 0;
            for (int i = 0; i < prevFile.children.length; i++) {
                for (int j = 0; j < contents.length; j++) {
                    if (prevFile.children[i].getName().equals(contents[j].getName())) {
                        if (prevFile.children[i].isDirectory() == contents[j].isDirectory()) {
                            tmp++;
                            break;    
                        }
                    }
                }
            }
            if (tmp != prevFile.children.length) changed = true;
        }
        
        if (!changed) {
            for (FileNode f : prevFile.children) recurseFolders(xOffset + indent, f);
            return;
        } 

        prevFile.children = contents;
        if (!prevFile.hasChildren()) return;
        for (FileNode f : prevFile.children) recurseFolders(xOffset + indent, f);
    }

    private void startRecurse(float dt) {
        if (rootPath == null) {
            rootFile = null;
            return;
        }

        if (rootFile == null) {
            rootFile = new FileNode(rootPath);
            rootFile.isOpen = true;
            rootFile.isRoot = true;
        }

        ImGui.getStyle().getColor(ImGuiCol.TabActive, folderC);
        ImGui.getStyle().getColor(ImGuiCol.TabHovered, fileC);

        time += dt;
        if (time >= 3) {
            shouldUpdate = true;
            time = 0;
        }

        globalId = 0;
        recurseFolders(indent, rootFile);
        shouldUpdate = false;
        collapse = false;
    }

    @Override
    public void destroy() {
        
    }

    private class FileNode{
        public boolean isRoot = false;
        public boolean isOpen = false;
        public File file;
        public FileNode[] children;
        public boolean isDirectory;

        public FileNode(String pathname) {
            this.file = new File(pathname);
            this.isDirectory = file.isDirectory();
        }

        public FileNode(File file) {
            this.file = file;
            this.isDirectory = file.isDirectory();
        }

        public String getName() {
            return this.file.getName();
        }

        public String getAbsolutePath() {
            return this.file.getAbsolutePath();
        }

        public boolean isDirectory() {
            return isDirectory;
        }

        public boolean isFile() {
            return !isDirectory;
        }

        // public void addChild(FileNode child) {
        //     if (!isFile() && children != null) {
        //         for (int i = 0; i < children.length; i++) {
        //             if (children[i] == null) {
        //                 children[i] = child;
        //             }
        //         }
        //     }
        // }

        // public void deleteChildren() {
        //     if (!isFile() && children != null) {
        //         for (int i = 0; i < children.length; i++) {
        //             children[i].deleteChildren();
        //             children[i] = null;
        //         }
        //         children = null;
        //     }
        // }

        public boolean hasChildren() {
            if (children == null) {
                return false;
            }
            if (children.length <= 0) {
                return false;
            }
            for (int i = 0; i < children.length; i++) {
                if (children[i] != null) {
                    return true;
                }
            }
            return false;
        }

        public void display() {
            if (this.isRoot) {
                return;
            }
            ImVec2 pos = new ImVec2(), size = new ImVec2(), mousePos = new ImVec2();
            if (canUseIcon) {
                if (this.isDirectory) {
                    ImGui.image(this.isOpen ? folderIconOpen : folderIcon, iconSize, iconSize, 0f, 1f, 1f, 0f, folderC.x, folderC.y, folderC.z, folderC.w);
                } else {
                    ImGui.image(fileIcon, iconSize, iconSize, 0f, 1f, 1f, 0f, fileC.x, fileC.y, fileC.z, fileC.w);
                }
                ImGui.sameLine();
            } 

            ImGui.getMousePos(mousePos);
            ImGui.getCursorScreenPos(pos);
            pos.x -= framePadding.x;
            pos.y -= framePadding.y;
            size.x = winSize.x - ImGui.getCursorPosX();
            size.y = ImGui.calcTextSize(this.getName()).y + framePadding.y;
            boolean inX = pos.x < mousePos.x && (pos.x + size.x) > mousePos.x;
            boolean inY = pos.y < mousePos.y && (pos.y + size.y) >= mousePos.y;

            if (ImGui.selectable(this.getName())) {
                if (this.isDirectory && !this.isRoot) this.isOpen = !this.isOpen;
            }

            if (inX && inY) {
                if (ImGui.isMouseReleased(1)) {
                    if (popupFile == null && !this.isRoot) { 
                        popupFile = this;
                        ImGui.openPopup("file_popup");
                    }
                }

            }
        }

        public FileNode[] listFiles() {
            File[] fs = file.listFiles();
            if (fs == null) return null;
            FileNode[] fns = new FileNode[fs.length];
            for (int i = 0; i < fs.length; i++) {
                fns[i] = new FileNode(fs[i]);
            }
            // fs = null;
            return fns;
        }
    }
}
