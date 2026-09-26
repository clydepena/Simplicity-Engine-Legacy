package editor.panels;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import components.NonPickable;
import editor.SelectionRenderer;
import editor.SimplicityEditor.EditorSelection;
import editor.SimplicityEditor.SimplicityEditorContext;
import editor.SimplicityEditor.SimplicityPanel;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiViewport;
import imgui.ImVec2;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiWindowFlags;
import observers.events.Event;
import renderer.Framebuffer;
import simplicity.Application.RenderContext;
import simplicity.Camera;
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

    // rectangle selection modes (defaults: visible objects only, partial overlap)
    private boolean rectIncludeHidden = false;   // true: also objects fully covered by others
    private boolean rectFullyInside = false;     // true: the whole object must be inside the rectangle

    // click / drag-select state: a press on the image starts it, the release selects
    private boolean pressActive = false;
    private boolean dragging = false;
    private final ImVec2 pressPos = new ImVec2();   // screen space
    private final ImVec2 dragEnd = new ImVec2();    // screen space, clamped to the image

    private final List<Integer> outlineUids = new ArrayList<>();   // reused each frame by selectedUids()

    public ViewportPanel(SimplicityEditorContext editorContext) {
        super(editorContext);
        this.selectionRenderer = new SelectionRenderer();
    }

    @Override
    public void onUpdate(float dt) {
        if (!isPlaying) {
            editorContext.world.setFrozen(true);
            editorContext.world.onEditorUpdate(dt);
        } else {
            editorContext.world.setFrozen(false);
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

        handleSelectionInput(renderContext);

        // drawn into the world's frame now; ImGui samples it later, at render time
        Camera camera = editorContext.world.camera();
        if (camera != null && renderContext.framebuffer() != null) {
            selectionRenderer.drawOutline(renderContext.renderer(), renderContext.framebuffer(), camera,
                editorContext.world.sprites(), selectedUids());
        }

        drawSelectionRect();

        ImGui.end();
    }

    /**
     * Click and drag-select. A press on the image starts it; the release selects: a click picks the object under the
     * press, a drag past ImGui's drag threshold picks the rectangle. Escape cancels. The selection mode comes from
     * the modifiers at release (see selectionMode()).
     */
    private void handleSelectionInput(RenderContext renderContext) {
        if (imageSize.x <= 0 || imageSize.y <= 0) {
            pressActive = false;
            dragging = false;
            return;
        }

        // owns the mouse over the image, so a drag keeps coming here even when it leaves the panel
        ImGui.setCursorScreenPos(imagePos.x, imagePos.y);
        ImGui.invisibleButton("##viewportInput", imageSize.x, imageSize.y);

        if (ImGui.isItemActivated()) {
            ImVec2 mouse = ImGui.getMousePos();
            pressPos.set(mouse.x, mouse.y);
            pressActive = true;
            dragging = false;
        }
        if (!pressActive) return;

        ImVec2 mouse = ImGui.getMousePos();
        dragEnd.set(clamp(mouse.x, imagePos.x, imagePos.x + imageSize.x - 1),
                    clamp(mouse.y, imagePos.y, imagePos.y + imageSize.y - 1));
        if (!dragging && ImGui.isMouseDragging(ImGuiMouseButton.Left)) dragging = true;

        if (ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.Escape))) {
            pressActive = false;
            dragging = false;
            return;
        }

        if (ImGui.isItemDeactivated()) {
            List<GameObject> picked = dragging
                ? pickInScreenRect(renderContext, pressPos, dragEnd)
                : pickAtScreenPoint(renderContext, pressPos);
            if (picked != null) editorContext.gameObjectSelection.apply(selectionMode(), picked);
            pressActive = false;
            dragging = false;
        }
    }

    /** Ctrl: toggle, Alt: subtract, Shift: add, none: replace (checked in that order). */
    private EditorSelection.Mode selectionMode() {
        ImGuiIO io = ImGui.getIO();
        if (io.getKeyCtrl()) return EditorSelection.Mode.TOGGLE;
        if (io.getKeyAlt()) return EditorSelection.Mode.SUBTRACT;
        if (io.getKeyShift()) return EditorSelection.Mode.ADD;
        return EditorSelection.Mode.REPLACE;
    }

    /** The drag rectangle, drawn by ImGui over the image (the world's frame is untouched), colored by mode. */
    private void drawSelectionRect() {
        if (!dragging) return;

        float x0 = Math.min(pressPos.x, dragEnd.x), y0 = Math.min(pressPos.y, dragEnd.y);
        float x1 = Math.max(pressPos.x, dragEnd.x), y1 = Math.max(pressPos.y, dragEnd.y);

        float r = 1.0f, g = 0.55f, b = 0.1f;   // replace / add: orange accent
        switch (selectionMode()) {
            case TOGGLE -> { r = 1.0f; g = 0.85f; b = 0.2f; }
            case SUBTRACT -> { r = 0.9f; g = 0.25f; b = 0.2f; }
            default -> {}
        }

        ImDrawList drawList = ImGui.getWindowDrawList();
        drawList.pushClipRect(imagePos.x, imagePos.y, imagePos.x + imageSize.x, imagePos.y + imageSize.y, true);
        drawList.addRectFilled(x0, y0, x1, y1, ImGui.getColorU32(r, g, b, 0.15f));
        drawList.addRect(x0, y0, x1, y1, ImGui.getColorU32(r, g, b, 0.9f), 0, 0, 1.0f);
        drawList.popClipRect();
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    /**
     * The object under a screen point as a pick: empty for empty space, null (leave the selection alone)
     * for a NonPickable object.
     */
    private List<GameObject> pickAtScreenPoint(RenderContext renderContext, ImVec2 screen) {
        List<GameObject> picked = new ArrayList<>();
        Framebuffer frame = renderContext.framebuffer();
        Camera camera = editorContext.world.camera();
        if (frame == null || camera == null) return picked;   // no scene set yet

        int[] p = toFramePixel(screen, frame);
        int uid = selectionRenderer.pick(renderContext.renderer(), frame, camera, editorContext.world.sprites(), p[0], p[1]);
        GameObject hit = uid < 0 ? null : editorContext.world.getGameObject(uid);
        if (hit != null && hit.getComponent(NonPickable.class) != null) return null;
        if (hit != null) picked.add(hit);
        return picked;
    }

    /**
     * Objects in the screen-space rectangle between a and b, using the rectangle selection modes; NonPickable
     * objects are left out.
     */
    private List<GameObject> pickInScreenRect(RenderContext renderContext, ImVec2 a, ImVec2 b) {
        List<GameObject> found = new ArrayList<>();
        Framebuffer frame = renderContext.framebuffer();
        Camera camera = editorContext.world.camera();
        if (frame == null || camera == null || imageSize.x <= 0 || imageSize.y <= 0) return found;

        List<GameObject> objects = editorContext.world.getGameObjectList();
        int maxUid = -1;
        for (GameObject go : objects) maxUid = Math.max(maxUid, go.getUid());

        int[] p0 = toFramePixel(a, frame);
        int[] p1 = toFramePixel(b, frame);
        Set<Integer> uids = selectionRenderer.pickRect(renderContext.renderer(), frame, camera, editorContext.world.sprites(),
            p0[0], p0[1], p1[0], p1[1], rectIncludeHidden, rectFullyInside, maxUid);

        // one pass over the world instead of a lookup per uid
        for (GameObject go : objects) {
            if (uids.contains(go.getUid()) && go.getComponent(NonPickable.class) == null) found.add(go);
        }
        return found;
    }



    /** Uids of the live selected objects, for the outline. */
    private List<Integer> selectedUids() {
        outlineUids.clear();
        List<GameObject> selected = editorContext.gameObjectSelection.selectedGameObjects;
        if (selected == null) return outlineUids;
        for (GameObject go : selected) {
            if (go != null && !go.isDead()) outlineUids.add(go.getUid());
        }
        return outlineUids;
    }

    /** Screen point (over the image) to a frame pixel, bottom-left origin, through the shown region uvMin..uvMax. */
    private int[] toFramePixel(ImVec2 screen, Framebuffer frame) {
        float u = uvMin.x + (screen.x - imagePos.x) / imageSize.x * (uvMax.x - uvMin.x);
        float v = uvMin.y + (screen.y - imagePos.y) / imageSize.y * (uvMax.y - uvMin.y);
        int px = (int) (u * frame.getWidth());
        int py = (int) ((1 - v) * frame.getHeight());   // frame pixels start at the bottom-left
        return new int[] {px, py};
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