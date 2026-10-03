package editor;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector2f;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import simplicity.GameObject;
import simplicity.Transform;

/**
 * Transform gizmo: translate (W), rotate (E) and scale (R) handles for the selected objects, drawn with an ImGui draw
 * list and hit-tested in screen pixels, both from the same shapes, so what is drawn is exactly what can be grabbed.
 * It is pure editor code: no world objects, no GPU passes, nothing saved.
 * <p>
 * The gizmo sits on one object (the owner passes the last selected one). A drag changes every selected object's
 * Transform by the same amount, measured from the values at the press (no per-frame drift); cancel() restores them.
 * Translate uses world axes; scale uses the object's rotated axes. Positions are object centres.
 */
public class Gizmo {

    public enum Tool { TRANSLATE, ROTATE, SCALE }

    public enum Handle { NONE, X, Y, CENTER, RING }

    /** World <-> screen mapping of the view the gizmo is drawn over. */
    public interface View {
        Vector2f worldToScreen(float x, float y);   // screen pixels
        Vector2f screenToWorld(float x, float y);   // world units
    }

    // shapes, in screen pixels (used for both drawing and hit-testing)
    private static final float AXIS_LENGTH = 80;
    private static final float LINE_THICKNESS = 3;
    private static final float LINE_HIT = 6;          // distance from an axis line that still grabs it
    private static final float HEAD_LENGTH = 14;      // translate arrowheads
    private static final float HEAD_HALF_WIDTH = 7;
    private static final float BOX_HALF = 6;          // scale handle ends
    private static final float CENTER_HALF = 7;
    private static final float RING_RADIUS = 70;
    private static final float RING_THICKNESS = 3;
    private static final float RING_HIT = 6;
    private static final float ORIENTATION_THICKNESS = 2;
    private static final float ORIENTATION_DOT_RADIUS = 4;
    private static final float LABEL_GAP = 8;         // between the ring and the rotate label

    private static final float SNAP_DEGREES = 15;
    private static final float MIN_SCALE = 0.001f;

    // base colors: X red, Y green, centre light grey, ring blue
    private static final float[] X_COLOR = {0.90f, 0.25f, 0.25f};
    private static final float[] Y_COLOR = {0.30f, 0.85f, 0.30f};
    private static final float[] CENTER_COLOR = {0.90f, 0.90f, 0.90f};
    private static final float[] RING_COLOR = {0.35f, 0.60f, 1.00f};

    private Tool tool = Tool.TRANSLATE;
    private Handle hovered = Handle.NONE;
    private Handle active = Handle.NONE;

    // drag state, captured at the press
    private final List<Transform> targets = new ArrayList<>();
    private final List<Transform> starts = new ArrayList<>();   // copies of the targets at the press
    private Transform pivotStart;                                // the gizmo object's transform at the press
    private final Vector2f pressMouseWorld = new Vector2f();
    private final Vector2f pressMouseScreen = new Vector2f();
    private float lastAngle, totalAngle;                         // rotate: degrees, unwrapped across turns

    public Tool getTool() {
        return tool;
    }

    /** Switches tools; ignored while a handle is being dragged. */
    public void setTool(Tool tool) {
        if (!isDragging()) this.tool = tool;
    }

    public boolean isDragging() {
        return active != Handle.NONE;
    }

    public Handle getHovered() {
        return hovered;
    }

    /** Updates which handle is under the mouse; NONE when there is no target or the mouse isn't over the view. */
    public void updateHover(GameObject target, View view, float mx, float my, boolean mouseOverView) {
        hovered = (target == null || !mouseOverView) ? Handle.NONE : hitTest(target.transform, view, mx, my);
    }

    /**
     * Starts dragging the hovered handle of target's gizmo, for every live object in selected.
     * Returns false (and does nothing) when no handle is hovered.
     */
    public boolean begin(GameObject target, List<GameObject> selected, View view, float mx, float my) {
        if (target == null || hovered == Handle.NONE) return false;

        targets.clear();
        starts.clear();
        for (GameObject go : selected) {
            if (go == null || go.isDead()) continue;
            targets.add(go.transform);
            starts.add(go.transform.copy());
        }
        if (targets.isEmpty()) return false;

        active = hovered;
        pivotStart = target.transform.copy();
        pressMouseWorld.set(view.screenToWorld(mx, my));
        pressMouseScreen.set(mx, my);
        lastAngle = angleAround(view.worldToScreen(pivotStart.position.x, pivotStart.position.y), mx, my);
        totalAngle = 0;
        return true;
    }

    /** Applies the drag for the mouse at (mx, my). snap: rotation in SNAP_DEGREES steps. */
    public void drag(View view, float mx, float my, boolean snap) {
        if (!isDragging()) return;
        switch (tool) {
            case TRANSLATE -> dragTranslate(view, mx, my);
            case ROTATE -> dragRotate(view, mx, my, snap);
            case SCALE -> dragScale(mx, my);
        }
    }

    /** What a finished drag changed: the transforms it moved, copies of them from the press, and the tool used. */
    public record Result(List<Transform> targets, List<Transform> before, Tool tool) {}

    /**
     * Ends the drag, keeping the changes.
     * @return what changed, for undo; null if nothing did (a click on a handle without a move)
     */
    public Result end() {
        boolean changed = false;
        for (int i = 0; i < targets.size() && !changed; i++) {
            changed = !targets.get(i).equals(starts.get(i));
        }
        Result result = changed ? new Result(new ArrayList<>(targets), new ArrayList<>(starts), tool) : null;
        active = Handle.NONE;
        targets.clear();
        starts.clear();
        return result;
    }

    /** Ends the drag and puts every target back to its values at the press. */
    public void cancel() {
        for (int i = 0; i < targets.size(); i++) {
            Transform t = targets.get(i), s = starts.get(i);
            t.position.set(s.position);
            t.scale.set(s.scale);
            t.rotation = s.rotation;
        }
        end();
    }

    private void dragTranslate(View view, float mx, float my) {
        Vector2f mouse = view.screenToWorld(mx, my);
        float dx = mouse.x - pressMouseWorld.x, dy = mouse.y - pressMouseWorld.y;
        if (active == Handle.X) dy = 0;
        if (active == Handle.Y) dx = 0;
        for (int i = 0; i < targets.size(); i++) {
            Vector2f start = starts.get(i).position;
            targets.get(i).position.set(start.x + dx, start.y + dy);
        }
    }

    private void dragRotate(View view, float mx, float my, boolean snap) {
        float angle = angleAround(view.worldToScreen(pivotStart.position.x, pivotStart.position.y), mx, my);
        float step = angle - lastAngle;
        if (step > 180) step -= 360;   // unwrap, so turning past +-180 keeps counting
        if (step < -180) step += 360;
        totalAngle += step;
        lastAngle = angle;

        float applied = snap ? Math.round(totalAngle / SNAP_DEGREES) * SNAP_DEGREES : totalAngle;
        for (int i = 0; i < targets.size(); i++) {
            targets.get(i).rotation = starts.get(i).rotation + applied;
        }
    }

    private void dragScale(float mx, float my) {
        float dx = mx - pressMouseScreen.x, dy = my - pressMouseScreen.y;
        float along;
        if (active == Handle.CENTER) {
            along = dx - dy;   // up-right grows, down-left shrinks
        } else {
            Vector2f dir = axisDirection(active, pivotStart.rotation);
            along = dx * dir.x + dy * dir.y;
        }
        // dragging one axis length further doubles the scale; linear, so presses near the centre stay controllable
        float factor = 1 + along / AXIS_LENGTH;

        boolean scaleX = active == Handle.X || active == Handle.CENTER;
        boolean scaleY = active == Handle.Y || active == Handle.CENTER;
        for (int i = 0; i < targets.size(); i++) {
            Vector2f start = starts.get(i).scale;
            Transform t = targets.get(i);
            if (scaleX) t.scale.x = Math.max(MIN_SCALE, start.x * factor);
            if (scaleY) t.scale.y = Math.max(MIN_SCALE, start.y * factor);
        }
    }

    // ---------------------------------------------------------------- shapes

    /** Screen direction (y down) of an axis handle; rotation in degrees, counter-clockwise in the world (y up). */
    private static Vector2f axisDirection(Handle handle, float rotationDegrees) {
        double r = Math.toRadians(rotationDegrees);
        float c = (float) Math.cos(r), s = (float) Math.sin(r);
        return handle == Handle.X ? new Vector2f(c, -s) : new Vector2f(-s, -c);
    }

    /** Degrees from centre to (mx, my), counter-clockwise as seen on screen (matching the world's rotation). */
    private static float angleAround(Vector2f centre, float mx, float my) {
        return (float) Math.toDegrees(Math.atan2(-(my - centre.y), mx - centre.x));
    }

    private float axisRotation(Transform t) {
        return tool == Tool.SCALE ? t.rotation : 0;
    }

    private Handle hitTest(Transform t, View view, float mx, float my) {
        Vector2f p = view.worldToScreen(t.position.x, t.position.y);
        float rx = mx - p.x, ry = my - p.y;

        if (tool == Tool.ROTATE) {
            float dist = (float) Math.sqrt(rx * rx + ry * ry);
            return Math.abs(dist - RING_RADIUS) <= RING_HIT ? Handle.RING : Handle.NONE;
        }

        if (Math.abs(rx) <= CENTER_HALF && Math.abs(ry) <= CENTER_HALF) return Handle.CENTER;

        for (Handle h : new Handle[] {Handle.X, Handle.Y}) {
            Vector2f d = axisDirection(h, axisRotation(t));
            float along = rx * d.x + ry * d.y;             // distance along the axis
            float across = Math.abs(-rx * d.y + ry * d.x); // distance from the axis line

            if (along >= CENTER_HALF && along <= AXIS_LENGTH && across <= LINE_HIT) return h;
            if (tool == Tool.TRANSLATE && along >= AXIS_LENGTH && along <= AXIS_LENGTH + HEAD_LENGTH
                && across <= HEAD_HALF_WIDTH) return h;
            if (tool == Tool.SCALE && Math.abs(along - AXIS_LENGTH) <= BOX_HALF && across <= BOX_HALF) return h;
        }
        return Handle.NONE;
    }

    /** Draws target's gizmo; the caller clips the draw list to the view. */
    public void draw(ImDrawList drawList, GameObject target, View view) {
        if (target == null) return;
        Transform t = target.transform;
        Vector2f p = view.worldToScreen(t.position.x, t.position.y);

        if (tool == Tool.ROTATE) {
            drawRotate(drawList, t, p);
            return;
        }

        for (Handle h : new Handle[] {Handle.X, Handle.Y}) {
            Vector2f d = axisDirection(h, axisRotation(t));
            Vector2f n = new Vector2f(-d.y, d.x);
            int col = color(h, h == Handle.X ? X_COLOR : Y_COLOR);

            float ex = p.x + d.x * AXIS_LENGTH, ey = p.y + d.y * AXIS_LENGTH;
            drawList.addLine(p.x + d.x * CENTER_HALF, p.y + d.y * CENTER_HALF, ex, ey, col, LINE_THICKNESS);

            if (tool == Tool.TRANSLATE) {
                float tx = p.x + d.x * (AXIS_LENGTH + HEAD_LENGTH), ty = p.y + d.y * (AXIS_LENGTH + HEAD_LENGTH);
                drawList.addTriangleFilled(tx, ty,
                    ex + n.x * HEAD_HALF_WIDTH, ey + n.y * HEAD_HALF_WIDTH,
                    ex - n.x * HEAD_HALF_WIDTH, ey - n.y * HEAD_HALF_WIDTH, col);
            } else {
                drawList.addQuadFilled(
                    ex + (d.x + n.x) * BOX_HALF, ey + (d.y + n.y) * BOX_HALF,
                    ex + (d.x - n.x) * BOX_HALF, ey + (d.y - n.y) * BOX_HALF,
                    ex + (-d.x - n.x) * BOX_HALF, ey + (-d.y - n.y) * BOX_HALF,
                    ex + (-d.x + n.x) * BOX_HALF, ey + (-d.y + n.y) * BOX_HALF, col);
            }
        }

        drawList.addRectFilled(p.x - CENTER_HALF, p.y - CENTER_HALF, p.x + CENTER_HALF, p.y + CENTER_HALF,
            color(Handle.CENTER, CENTER_COLOR));
    }

    /**
     * Ring, plus an orientation line from the centre to the ring at the object's rotation (0 deg = right, counter-
     * clockwise). While dragging: a wedge from the rotation at the press to now (snapped when snapping), a faint line
     * at the start angle, and a label with this drag's change and the total rotation.
     */
    private void drawRotate(ImDrawList drawList, Transform t, Vector2f p) {
        int ringCol = color(Handle.RING, RING_COLOR);

        if (active == Handle.RING) {
            float sweep = t.rotation - pivotStart.rotation;   // what was applied, snapping included
            // full turns land back on the start angle, so the wedge only shows the part of a turn (sign kept)
            drawWedge(drawList, p, pivotStart.rotation, sweep % 360f, ImGui.getColorU32(RING_COLOR[0], RING_COLOR[1], RING_COLOR[2], 0.25f));
            drawSpoke(drawList, p, pivotStart.rotation, ImGui.getColorU32(RING_COLOR[0], RING_COLOR[1], RING_COLOR[2], 0.5f), 1.5f, 0);
        }

        drawList.addCircle(p.x, p.y, RING_RADIUS, ringCol, 64, RING_THICKNESS);
        drawSpoke(drawList, p, t.rotation, ringCol, ORIENTATION_THICKNESS, ORIENTATION_DOT_RADIUS);

        if (active == Handle.RING) {
            float sweep = t.rotation - pivotStart.rotation;
            String label = String.format("%+.1f°  (%.1f°)", sweep, t.rotation);
            ImVec2 size = ImGui.calcTextSize(label);
            float lx = p.x - size.x * 0.5f, ly = p.y + RING_RADIUS + LABEL_GAP;
            drawList.addRectFilled(lx - 4, ly - 2, lx + size.x + 4, ly + size.y + 2, ImGui.getColorU32(0, 0, 0, 0.6f), 3);
            drawList.addText(lx, ly, ImGui.getColorU32(1, 1, 1, 1), label);
        }
    }

    /** A line from the centre to the ring at a world angle (degrees, counter-clockwise), with an optional end dot. */
    private static void drawSpoke(ImDrawList drawList, Vector2f p, float degrees, int col, float thickness, float dotRadius) {
        double r = Math.toRadians(degrees);
        float ex = p.x + (float) Math.cos(r) * RING_RADIUS, ey = p.y - (float) Math.sin(r) * RING_RADIUS;   // screen y down
        drawList.addLine(p.x, p.y, ex, ey, col, thickness);
        if (dotRadius > 0) drawList.addCircleFilled(ex, ey, dotRadius, col, 16);
    }

    /**
     * Filled wedge from a world angle through sweep degrees (either sign), capped at a full turn. Drawn as fans of at
     * most 90 degrees, since ImGui fills convex shapes only.
     */
    private static void drawWedge(ImDrawList drawList, Vector2f p, float fromDegrees, float sweep, int col) {
        float remaining = Math.max(-360, Math.min(360, sweep));
        float from = fromDegrees;
        while (Math.abs(remaining) > 0.01f) {
            float step = Math.signum(remaining) * Math.min(90, Math.abs(remaining));
            // ImGui arcs run clockwise on screen (y down), world angles counter-clockwise: negate
            float a0 = (float) Math.toRadians(-from), a1 = (float) Math.toRadians(-(from + step));
            drawList.pathLineTo(p.x, p.y);
            drawList.pathArcTo(p.x, p.y, RING_RADIUS, Math.min(a0, a1), Math.max(a0, a1), 16);
            drawList.pathFillConvex(col);
            from += step;
            remaining -= step;
        }
    }

    /** Normal; brighter when hovered; darker while dragged; faded while another handle is dragged. */
    private int color(Handle handle, float[] rgb) {
        float r = rgb[0], g = rgb[1], b = rgb[2], a = 0.85f;
        if (active != Handle.NONE && active != handle) {
            a = 0.25f;
        } else if (active == handle) {
            r *= 0.6f; g *= 0.6f; b *= 0.6f; a = 1.0f;
        } else if (hovered == handle) {
            r += (1 - r) * 0.4f; g += (1 - g) * 0.4f; b += (1 - b) * 0.4f; a = 1.0f;
        }
        return ImGui.getColorU32(r, g, b, a);
    }
}
