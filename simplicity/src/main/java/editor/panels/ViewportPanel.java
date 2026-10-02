package editor.panels;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_DECIMAL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.joml.Vector2f;

import components.NonPickable;
import editor.EditorCamera;
import editor.Gizmo;
import editor.SelectionRenderer;
import editor.EditorSelection;
import editor.SimplicityEditorContext;
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

    // the editor's own view of the world, drawn through while editing (the game camera is left alone)
    private final EditorCamera editorCamera = new EditorCamera();
    private Camera lastGameCamera = null;   // a new one means a new scene: start the editor camera from it
    private boolean panning = false;

    // transform gizmo on the last selected object
    private final Gizmo gizmo = new Gizmo();

    // world <-> screen through the camera the world is drawn with and the part of the frame the image shows
    private final Gizmo.View gizmoView = new Gizmo.View() {
        @Override
        public Vector2f worldToScreen(float x, float y) {
            Vector2f uv = editorContext.world.renderCamera().worldToViewport(x, y);
            float vTop = 1 - uv.y;   // the camera's v points up, the image's down
            return new Vector2f(imagePos.x + (uv.x - uvMin.x) / (uvMax.x - uvMin.x) * imageSize.x,
                                imagePos.y + (vTop - uvMin.y) / (uvMax.y - uvMin.y) * imageSize.y);
        }

        @Override
        public Vector2f screenToWorld(float x, float y) {
            float u = uvMin.x + (x - imagePos.x) / imageSize.x * (uvMax.x - uvMin.x);
            float v = uvMin.y + (y - imagePos.y) / imageSize.y * (uvMax.y - uvMin.y);
            return editorContext.world.renderCamera().viewportToWorld(u, 1 - v);
        }
    };

    public ViewportPanel(SimplicityEditorContext editorContext) {
        super(editorContext);
        this.selectionRenderer = new SelectionRenderer();
    }

    @Override
    public void onUpdate(float dt) {
        Camera gameCamera = editorContext.world.camera();
        if (gameCamera != lastGameCamera) {
            if (gameCamera != null) editorCamera.copyFrom(gameCamera);
            lastGameCamera = gameCamera;
        }

        if (!isPlaying) {
            editorContext.world.setFrozen(true);
            editorContext.world.setViewCamera(editorCamera.camera());
            editorCamera.update(dt);
            editorContext.world.onEditorUpdate(dt);
        } else {
            editorContext.world.setFrozen(false);
            editorContext.world.setViewCamera(null);
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

        // owns the mouse over the image (left: select, middle: pan), so a drag keeps coming here even when it
        // leaves the panel; its state is read once, since later code adds no items but the handlers need it
        boolean hasImage = imageSize.x > 0 && imageSize.y > 0;
        boolean activated = false, deactivated = false;
        if (hasImage) {
            ImGui.setCursorScreenPos(imagePos.x, imagePos.y);
            // built from the button index like Dear ImGui's own definition (ImGuiButtonFlags_MouseButtonX = 1 << X):
            // imgui-java 1.86's ImGuiButtonFlags.MouseButtonLeft is 0, which would leave the left button out
            ImGui.invisibleButton("##viewportInput", imageSize.x, imageSize.y,
                (1 << ImGuiMouseButton.Left) | (1 << ImGuiMouseButton.Middle));
            activated = ImGui.isItemActivated();
            deactivated = ImGui.isItemDeactivated();
        }

        // a left press is owned by exactly one of: a gizmo handle (tested first) or selection, until its release
        GameObject gizmoTarget = gizmoTarget();
        handleGizmoPress(gizmoTarget, hasImage, activated);
        handleSelectionInput(renderContext, hasImage, activated, deactivated);

        // drawn into the world's frame now; ImGui samples it later, at render time. Not while playing: the viewport is
        // then the game view and shows the frame exactly as the game draws it (the selection itself is kept)
        Camera camera = editorContext.world.renderCamera();
        if (!isPlaying && camera != null && renderContext.framebuffer() != null) {
            selectionRenderer.drawOutline(renderContext.renderer(), renderContext.framebuffer(), camera,
                editorContext.world.sprites(), selectedUids());
        }

        drawSelectionRect();
        drawGizmo(gizmoTarget);

        // last: the world, the outline and the gizmo were drawn with this frame's camera and transforms;
        // gizmo and camera changes show from the next frame, so everything stays in step
        handleGizmoDrag(deactivated);
        handleCameraInput(hasImage, activated, deactivated);

        ImGui.end();
    }

    /**
     * Editor camera: middle-drag pans (the grabbed point stays under the mouse), the scroll wheel zooms toward the
     * mouse, numpad '.' eases the view back to the origin. Only while editing, not playing.
     */
    private void handleCameraInput(boolean hasImage, boolean activated, boolean deactivated) {
        if (isPlaying || !hasImage) {
            panning = false;
            return;
        }

        if (activated && ImGui.isMouseClicked(ImGuiMouseButton.Middle)) panning = true;
        if (panning && (deactivated || !ImGui.isMouseDown(ImGuiMouseButton.Middle))) panning = false;

        Camera camera = editorCamera.camera();
        if (panning) {
            // screen pixels -> world units, through the part of the frame the image shows
            ImVec2 delta = ImGui.getIO().getMouseDelta();
            float worldPerPixelX = (uvMax.x - uvMin.x) / imageSize.x * camera.getProjectionSize().x * camera.getZoom();
            float worldPerPixelY = (uvMax.y - uvMin.y) / imageSize.y * camera.getProjectionSize().y * camera.getZoom();
            editorCamera.pan(-delta.x * worldPerPixelX, delta.y * worldPerPixelY);   // screen y points down
        }

        float wheel = ImGui.getIO().getMouseWheel();
        if (hovered && wheel != 0) {
            ImVec2 mouse = ImGui.getMousePos();
            float u = uvMin.x + (mouse.x - imagePos.x) / imageSize.x * (uvMax.x - uvMin.x);
            float v = uvMin.y + (mouse.y - imagePos.y) / imageSize.y * (uvMax.y - uvMin.y);
            editorCamera.zoomAt(u, 1 - v, wheel);   // the camera's v points up
        }

        if ((hovered || ImGui.isWindowFocused()) && ImGui.isKeyPressed(GLFW_KEY_KP_DECIMAL, false)) {
            editorCamera.resetView();
        }
    }

    /**
     * Click and drag-select. A left press on the image starts it; the release selects: a click picks the object under
     * the press, a drag past ImGui's drag threshold picks the rectangle. Escape cancels. The selection mode comes from
     * the modifiers at release (see selectionMode()). Only while editing: while playing, clicks belong to the game
     * (they still reach the world through isMouseOverWorld()), and a press or drag in progress is dropped.
     */
    private void handleSelectionInput(RenderContext renderContext, boolean hasImage, boolean activated, boolean deactivated) {
        if (isPlaying || !hasImage) {
            pressActive = false;
            dragging = false;
            return;
        }

        if (activated && ImGui.isMouseClicked(ImGuiMouseButton.Left) && !gizmo.isDragging()) {
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

        if (deactivated) {
            List<GameObject> picked = dragging
                ? pickInScreenRect(renderContext, pressPos, dragEnd)
                : pickAtScreenPoint(renderContext, pressPos);
            if (picked != null) editorContext.gameObjectSelection.apply(selectionMode(), picked);
            pressActive = false;
            dragging = false;
        }
    }


    /** The object the gizmo sits on: the last live selected object; none while playing. */
    private GameObject gizmoTarget() {
        if (isPlaying) return null;
        List<GameObject> selected = editorContext.gameObjectSelection.selectedGameObjects;
        if (selected == null) return null;
        for (int i = selected.size() - 1; i >= 0; i--) {
            GameObject go = selected.get(i);
            if (go != null && !go.isDead()) return go;
        }
        return null;
    }

    /**
     * Gizmo hover, tool keys (W translate, E rotate, R scale) and the press: a left press on a hovered handle starts
     * a gizmo drag, which then owns the press, so selection never sees it.
     */
    private void handleGizmoPress(GameObject target, boolean hasImage, boolean activated) {
        if (isPlaying || !hasImage) {
            if (gizmo.isDragging()) gizmo.end();
            gizmo.updateHover(null, gizmoView, 0, 0, false);
            return;
        }

        ImVec2 mouse = ImGui.getMousePos();
        if (!gizmo.isDragging() && !pressActive && !panning) {
            gizmo.updateHover(target, gizmoView, mouse.x, mouse.y, hovered);

            if ((hovered || ImGui.isWindowFocused())) {
                if (ImGui.isKeyPressed(GLFW_KEY_W, false)) gizmo.setTool(Gizmo.Tool.TRANSLATE);
                if (ImGui.isKeyPressed(GLFW_KEY_E, false)) gizmo.setTool(Gizmo.Tool.ROTATE);
                if (ImGui.isKeyPressed(GLFW_KEY_R, false)) gizmo.setTool(Gizmo.Tool.SCALE);
            }
        }

        if (activated && ImGui.isMouseClicked(ImGuiMouseButton.Left) && gizmo.getHovered() != Gizmo.Handle.NONE) {
            gizmo.begin(target, editorContext.gameObjectSelection.selectedGameObjects, gizmoView, mouse.x, mouse.y);
        }
    }

    /** Applies a gizmo drag; the release keeps it, Escape puts the transforms back. Ctrl snaps rotation. */
    private void handleGizmoDrag(boolean deactivated) {
        if (!gizmo.isDragging()) return;

        // Escape first: with keyboard navigation on, Escape also makes ImGui release the active item, so the same
        // frame reports the button as deactivated, which would otherwise end (keep) the drag
        if (ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.Escape))) {
            gizmo.cancel();
        } else if (deactivated || !ImGui.isMouseDown(ImGuiMouseButton.Left)) {
            gizmo.end();
        } else {
            ImVec2 mouse = ImGui.getMousePos();
            gizmo.drag(gizmoView, mouse.x, mouse.y, ImGui.getIO().getKeyCtrl());
        }
    }

    private void drawGizmo(GameObject target) {
        if (target == null || imageSize.x <= 0 || imageSize.y <= 0) return;
        ImDrawList drawList = ImGui.getWindowDrawList();
        drawList.pushClipRect(imagePos.x, imagePos.y, imagePos.x + imageSize.x, imagePos.y + imageSize.y, true);
        gizmo.draw(drawList, target, gizmoView);
        drawList.popClipRect();
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
        Camera camera = editorContext.world.renderCamera();
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
        Camera camera = editorContext.world.renderCamera();
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

    private void play() {
        isPlaying = true;
        // bodies were built when the scene started; edits since then only changed transforms and components
        editorContext.world.rebuildPhysics();
    }

    private void renderMenuBar() {
        if (!ImGui.beginMenuBar()) return;

        // TODO: route through editor commands once they exist (was EventSystem.notify(GameEngineStartPlay/StopPlay))
        if (ImGui.menuItem("Play", "", isPlaying, !isPlaying)) {
            play();
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