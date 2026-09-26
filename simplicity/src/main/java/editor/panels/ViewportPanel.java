package editor.panels;

import java.util.ArrayList;
import java.util.List;

import components.NonPickable;
import editor.SelectionRenderer;
import editor.SimplicityEditor.EditorSelection;
import editor.SimplicityEditor.SimplicityEditorContext;
import editor.SimplicityEditor.SimplicityPanel;
import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.ImVec2;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiWindowFlags;
import observers.events.Event;
import renderer.Framebuffer;
import simplicity.Application.RenderContext;
import simplicity.GameObject;

public class ViewportPanel extends SimplicityPanel {

    private boolean focusOnce = true;
    private boolean hovered = false;
    private boolean isPlaying = false;

    // screen-space rectangle showing the world
    private final ImVec2 imagePos = new ImVec2();
    private final ImVec2 imageSize = new ImVec2();

    private final ImVec2 uvMin = new ImVec2(0, 0);
    private final ImVec2 uvMax = new ImVec2(1, 1);

    private final SelectionRenderer selectionRenderer;

    public ViewportPanel(SimplicityEditorContext editorContext) {
        super(editorContext);
        this.selectionRenderer = new SelectionRenderer();
    }

    @Override
    public void onUpdate(float dt) {
        if (isPlaying) {
            // editorContext.world.setFrozen(false);
        }
    }

    // int ctr = 0;

    @Override
    public void onRender(RenderContext renderContext) {
        if (focusOnce) {
            ImGui.setNextWindowFocus();
            focusOnce = false;
        }

        int flags = ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse | ImGuiWindowFlags.MenuBar;

        ImGui.begin("Viewport", flags);
        updateCalc();
        renderMenuBar();
        renderRegionImage(renderContext.framebuffer());

        hovered = ImGui.isWindowHovered() && ImGui.isMouseHoveringRect(imagePos.x, imagePos.y, imagePos.x + imageSize.x, imagePos.y + imageSize.y);
        // if (hovered) System.out.println((ctr++) + "hovered");

        if (hovered && ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            pickAtMouse(renderContext);
        }

        // drawn into the world's frame now; ImGui samples it later, at render time
        List<GameObject> selected = editorContext.gameObjectSelection.selectedGameObjects;
        selectionRenderer.drawOutline(renderContext, editorContext.world, selected);

        ImGui.end();
    }

    /** Click: select the object under the mouse (Ctrl: toggle it). Empty space clears; NonPickable objects are ignored. */
    private void pickAtMouse(RenderContext renderContext) {
        Framebuffer frame = renderContext.framebuffer();
        if (frame == null || imageSize.x <= 0 || imageSize.y <= 0) return;

        ImVec2 mouse = ImGui.getMousePos();
        float u = uvMin.x + (mouse.x - imagePos.x) / imageSize.x * (uvMax.x - uvMin.x);
        float v = uvMin.y + (mouse.y - imagePos.y) / imageSize.y * (uvMax.y - uvMin.y);
        int px = (int) (u * frame.getWidth());
        int py = (int) ((1 - v) * frame.getHeight());   // frame pixels start at the bottom-left

        GameObject hit = selectionRenderer.pick(renderContext, editorContext.world, px, py);
        if (hit != null && hit.getComponent(NonPickable.class) != null) return;

        EditorSelection selection = editorContext.gameObjectSelection;
        if (selection.selectedGameObjects == null) selection.selectedGameObjects = new ArrayList<>();
        List<GameObject> selected = selection.selectedGameObjects;
        boolean toggle = ImGui.getIO().getKeyCtrl();

        if (hit == null) {
            if (!toggle) selected.clear();
        } else if (toggle) {
            if (!selected.remove(hit)) selected.add(hit);
        } else {
            selected.clear();
            selected.add(hit);
        }
    }

    private void renderMenuBar() {
        if (!ImGui.beginMenuBar()) return;

        // TODO: route through editor commands once they exist (was EventSystem.notify(GameEngineStartPlay/StopPlay))
        if (ImGui.menuItem("Play", "", isPlaying, !isPlaying)) {
            isPlaying = true;
        }
        if (ImGui.menuItem("Stop", "", !isPlaying, isPlaying)) {
            isPlaying = false;
        }

        ImGui.endMenuBar();
    }

    private void renderRegionImage(Framebuffer frame) {
        ImGui.getCursorScreenPos(imagePos);
        ImGui.getContentRegionAvail(imageSize);
        if (frame == null || imageSize.x <= 0 || imageSize.y <= 0) {
            imageSize.set(0, 0);
            return;
        }

        ImGuiViewport main = ImGui.getMainViewport();
        float mainX = main.getPosX(), mainY = main.getPosY();
        float mainW = main.getSizeX(), mainH = main.getSizeY();
        if (mainW <= 0 || mainH <= 0) {
            imageSize.set(0, 0);
            return;
        }

        float srcX = imagePos.x, srcY = imagePos.y;
        float srcW = imageSize.x, srcH = imageSize.y;

        boolean insideMain = srcX >= mainX && srcY >= mainY && srcX + srcW <= mainX + mainW && srcY + srcH <= mainY + mainH;

        if (!insideMain) {
            srcW = Math.min(srcW, mainW);
            srcH = Math.min(srcH, mainH);
            srcX = mainX + (mainW - srcW) * 0.5f;
            srcY = mainY + (mainH - srcH) * 0.5f;
        }

        uvMin.set((srcX - mainX) / mainW, (srcY - mainY) / mainH);
        uvMax.set((srcX + srcW - mainX) / mainW, (srcY + srcH - mainY) / mainH);

        ImGui.image(frame.getTexId(), imageSize.x, imageSize.y, uvMin.x, 1 - uvMin.y, uvMax.x, 1 - uvMax.y);
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public ImVec2 getImagePos() {
        return new ImVec2(imagePos.x, imagePos.y);
    }

    public ImVec2 getImageSize() {
        return new ImVec2(imageSize.x, imageSize.y);
    }

    public ImVec2 getFrameUvMin() {
        return new ImVec2(uvMin.x, uvMin.y);
    }

    public ImVec2 getFrameUvMax() {
        return new ImVec2(uvMax.x, uvMax.y);
    }

    @Override
    public void destroy() {
        selectionRenderer.destroy();
    }

    @Override
    public void onEvent(Event event) {

    }
}