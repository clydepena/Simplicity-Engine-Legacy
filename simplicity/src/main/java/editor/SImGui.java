package editor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import asset.Asset;
import components.Sprite;
import imgui.ImDrawList;
import imgui.ImGui;
import renderer.Texture;
import imgui.ImGuiViewport;
import imgui.ImVec4;
import imgui.flag.*;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import util.Settings;

public class SImGui {

    /*
     * Property grid: labelled controls in a two-column table (label | value), as an inspector shows them.
     *
     *     if (SImGui.beginProperties("transform")) {
     *         SImGui.vec2("Position", transform.position, 0.0f);
     *         if (SImGui.dragFloat("Rotation", rotation)) ...;
     *         SImGui.endProperties();
     *     }
     *
     * Every control returns true on the frame its value changes (JOML vectors are changed in place; single values
     * through a one-element array or an ImGui type, like ImGui itself). Each is one ImGui group, so right after it
     * ImGui.isItemActive() tells whether the user is still editing it (dragging, typing), e.g. to record one undo
     * step per edit rather than one per frame.
     */

    private static final float LABEL_COLUMN_WEIGHT = 0.4f;
    private static final float VECTOR_SPEED = 0.01f;

    // axis colours: X red, Y green, Z blue, W grey
    private static final float[][] AXIS_COLORS = {
        {0.80f, 0.15f, 0.15f}, {0.20f, 0.65f, 0.20f}, {0.20f, 0.40f, 0.85f}, {0.45f, 0.45f, 0.45f}
    };
    private static final String[] AXIS_NAMES = {"X", "Y", "Z", "W"};

    /** Starts a property grid; draw controls only if it returns true, then call endProperties(). */
    public static boolean beginProperties(String id) {
        int flags = ImGuiTableFlags.Resizable | ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingStretchProp;
        if (!ImGui.beginTable(id, 2, flags)) return false;
        ImGui.tableSetupColumn("Name", ImGuiTableColumnFlags.WidthStretch, LABEL_COLUMN_WEIGHT);
        ImGui.tableSetupColumn("Value", ImGuiTableColumnFlags.WidthStretch, 1.0f - LABEL_COLUMN_WEIGHT);
        return true;
    }

    public static void endProperties() {
        ImGui.endTable();
    }

    /**
     * A collapsible row for nested values (an object inside a component, a list); its rows follow, indented.
     * If it returns true, draw them, then call endPropertyGroup().
     */
    public static boolean beginPropertyGroup(String label) {
        ImGui.tableNextRow();
        ImGui.tableSetColumnIndex(0);
        return ImGui.treeNodeEx(label, ImGuiTreeNodeFlags.SpanFullWidth);
    }

    public static void endPropertyGroup() {
        ImGui.treePop();
    }

    public static boolean dragInt(String label, int[] value) {
        beginProperty(label);
        boolean changed = ImGui.dragInt("##value", value, 0.1f);
        return endProperty(changed);
    }

    public static boolean dragFloat(String label, float[] value) {
        return dragFloat(label, value, VECTOR_SPEED);
    }

    public static boolean dragFloat(String label, float[] value, float speed) {
        beginProperty(label);
        boolean changed = ImGui.dragFloat("##value", value, speed);
        return endProperty(changed);
    }

    public static boolean checkbox(String label, ImBoolean value) {
        beginProperty(label);
        boolean changed = ImGui.checkbox("##value", value);
        return endProperty(changed);
    }

    /** Changes as you type (live); the caller decides when the edit counts as finished (see isItemActive above). */
    public static boolean inputText(String label, ImString value) {
        beginProperty(label);
        boolean changed = ImGui.inputText("##value", value);
        return endProperty(changed);
    }

    public static boolean combo(String label, ImInt index, String[] items) {
        beginProperty(label);
        boolean changed = ImGui.combo("##value", index, items, items.length);
        return endProperty(changed);
    }

    /** RGBA colour with an alpha bar; click the swatch for a picker. */
    public static boolean color(String label, Vector4f color) {
        beginProperty(label);
        float[] rgba = {color.x, color.y, color.z, color.w};
        boolean changed = ImGui.colorEdit4("##value", rgba, ImGuiColorEditFlags.AlphaBar | ImGuiColorEditFlags.AlphaPreviewHalf);
        if (changed) color.set(rgba[0], rgba[1], rgba[2], rgba[3]);
        return endProperty(changed);
    }

    /** A value that can't be edited here, shown greyed out. */
    public static void readOnly(String label, String text) {
        beginProperty(label);
        ImGui.textDisabled(text);
        endProperty(false);
    }

    /** X / Y fields with coloured axis buttons; clicking an axis button sets that axis to resetValue. */
    public static boolean vec2(String label, Vector2f v, float resetValue) {
        float[] values = {v.x, v.y};
        boolean changed = axes(label, values, resetValue);
        if (changed) v.set(values[0], values[1]);
        return changed;
    }

    public static boolean vec3(String label, Vector3f v, float resetValue) {
        float[] values = {v.x, v.y, v.z};
        boolean changed = axes(label, values, resetValue);
        if (changed) v.set(values[0], values[1], values[2]);
        return changed;
    }

    public static boolean vec4(String label, Vector4f v, float resetValue) {
        float[] values = {v.x, v.y, v.z, v.w};
        boolean changed = axes(label, values, resetValue);
        if (changed) v.set(values[0], values[1], values[2], values[3]);
        return changed;
    }

    /** One drag field per axis, each after its axis button, sharing the value column's width evenly. */
    private static boolean axes(String label, float[] values, float resetValue) {
        beginProperty(label);
        boolean changed = false;
        int count = values.length;
        float spacing = ImGui.getStyle().getItemInnerSpacingX();
        float buttonWidth = ImGui.getFrameHeight();
        float fieldWidth = Math.max(1.0f, (ImGui.getContentRegionAvailX() - count * buttonWidth - (count - 1) * spacing) / count);

        for (int i = 0; i < count; i++) {
            ImGui.pushID(i);
            float[] c = AXIS_COLORS[i];
            ImGui.pushStyleColor(ImGuiCol.Button, c[0], c[1], c[2], 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, c[0] + 0.1f, c[1] + 0.1f, c[2] + 0.1f, 1.0f);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, c[0], c[1], c[2], 1.0f);
            if (ImGui.button(AXIS_NAMES[i], buttonWidth, ImGui.getFrameHeight())) {
                changed |= values[i] != resetValue;
                values[i] = resetValue;
            }
            ImGui.popStyleColor(3);
            if (ImGui.isItemHovered()) ImGui.setTooltip("Reset " + AXIS_NAMES[i] + " to " + resetValue);

            ImGui.sameLine(0, 0);   // the field touches its button
            ImGui.setNextItemWidth(fieldWidth);
            float[] axis = {values[i]};
            if (ImGui.dragFloat("##axis", axis, VECTOR_SPEED, 0, 0, "%.3f")) {
                values[i] = axis[0];
                changed = true;
            }
            ImGui.popID();
            if (i + 1 < count) ImGui.sameLine(0, spacing);
        }
        return endProperty(changed);
    }

    /** The label in the first column, then the value column, full width; the value is drawn as one group. */
    private static void beginProperty(String label) {
        ImGui.tableNextRow();
        ImGui.tableSetColumnIndex(0);
        ImGui.alignTextToFramePadding();
        ImGui.textUnformatted(label);
        if (ImGui.isItemHovered() && ImGui.calcTextSize(label).x > ImGui.getColumnWidth()) ImGui.setTooltip(label);
        ImGui.tableSetColumnIndex(1);
        ImGui.pushID(label);
        ImGui.setNextItemWidth(-Float.MIN_VALUE);
        ImGui.beginGroup();
    }

    private static boolean endProperty(boolean changed) {
        ImGui.endGroup();
        ImGui.popID();
        return changed;
    }

    /**
     * A text field with a button after it on the same row (e.g. a path and "Browse..."), in the property grid.
     * The text is edited in place; returns true on the frame the button is clicked.
     */
    public static boolean inputTextWithButton(String label, ImString value, String buttonLabel) {
        beginProperty(label);
        float buttonWidth = ImGui.calcTextSize(buttonLabel).x + ImGui.getStyle().getFramePaddingX() * 2;
        float spacing = ImGui.getStyle().getItemInnerSpacingX();
        ImGui.setNextItemWidth(Math.max(1.0f, ImGui.getContentRegionAvailX() - buttonWidth - spacing));
        ImGui.inputText("##value", value);
        ImGui.sameLine(0, spacing);
        boolean clicked = ImGui.button(buttonLabel);
        endProperty(false);
        return clicked;
    }

    // ------------------------------------------------------------------------------------------------
    // Layout: alignment, wrapping, window placement
    // ------------------------------------------------------------------------------------------------

    /**
     * Moves the cursor so the next item, itemWidth wide, sits at alignment across the space left on this row:
     * 0 left, 0.5 centred, 1 right. Works after sameLine() too (e.g. a filter box on the right of a title).
     */
    public static void alignNext(float itemWidth, float alignment) {
        float offset = (ImGui.getContentRegionAvailX() - itemWidth) * Math.max(0.0f, Math.min(1.0f, alignment));
        if (offset > 0.0f) ImGui.setCursorPosX(ImGui.getCursorPosX() + offset);
    }

    /** The width of a button with this label, as ImGui.button(label) draws it. */
    public static float buttonWidth(String label) {
        return ImGui.calcTextSize(label).x + ImGui.getStyle().getFramePaddingX() * 2;
    }

    public static void textAligned(String text, float alignment) {
        alignNext(ImGui.calcTextSize(text).x, alignment);
        ImGui.text(text);
    }

    public static void textColoredAligned(float r, float g, float b, float a, String text, float alignment) {
        alignNext(ImGui.calcTextSize(text).x, alignment);
        ImGui.textColored(r, g, b, a, text);
    }

    /**
     * Call between items of a wrapping row (a grid of buttons): keeps the next item, nextWidth wide, on this row if
     * it still fits in the window, otherwise lets it start a new row.
     */
    public static void sameLineIfFits(float nextWidth) {
        float spacing = ImGui.getStyle().getItemSpacingX();
        float right = ImGui.getWindowPosX() + ImGui.getWindowContentRegionMaxX();
        if (ImGui.getItemRectMaxX() + spacing + nextWidth <= right) ImGui.sameLine();
    }

    /** The next window covers the main window's work area (below its menu bar), e.g. a full-window launcher. */
    public static void fillMainViewport() {
        ImGuiViewport vp = ImGui.getMainViewport();
        ImGui.setNextWindowPos(vp.getWorkPosX(), vp.getWorkPosY());
        ImGui.setNextWindowSize(vp.getWorkSizeX(), vp.getWorkSizeY());
        ImGui.setNextWindowViewport(vp.getID());
    }

    /** The next window is centred on the main window (cond: ImGuiCond.Appearing to let the user move it after). */
    public static void centerNextWindow(int cond) {
        ImGuiViewport vp = ImGui.getMainViewport();
        ImGui.setNextWindowPos(vp.getWorkPosX() + vp.getWorkSizeX() * 0.5f, vp.getWorkPosY() + vp.getWorkSizeY() * 0.5f, cond, 0.5f, 0.5f);
    }

    // ------------------------------------------------------------------------------------------------
    // Messages and text icons
    // ------------------------------------------------------------------------------------------------

    private static final float[] WARNING_COLOR = {0.90f, 0.67f, 0.24f, 1.0f};
    private static final float[] ERROR_COLOR = {1.00f, 0.35f, 0.35f, 1.0f};

    /** "! message" in the warning colour: something to fix, nothing failed yet. */
    public static void warningText(String message) {
        ImGui.textColored(WARNING_COLOR[0], WARNING_COLOR[1], WARNING_COLOR[2], WARNING_COLOR[3], "! " + message);
    }

    /** "! message" in the error colour: something failed. */
    public static void errorText(String message) {
        ImGui.textColored(ERROR_COLOR[0], ERROR_COLOR[1], ERROR_COLOR[2], ERROR_COLOR[3], "! " + message);
    }

    /**
     * A text icon (e.g. a FontAwesomeIcons glyph) that works as a button: no frame, its colour brightens while
     * hovered, the hand cursor and a tooltip show. Colours are packed (ImGui.colorConvertFloat4ToU32); tooltip may
     * be null. Returns true when clicked.
     */
    public static boolean iconButton(String id, String icon, int color, int hoveredColor, String tooltip) {
        float width = ImGui.calcTextSize(icon).x, height = ImGui.getTextLineHeight();
        boolean clicked = ImGui.invisibleButton(id, width, height);
        boolean hovered = ImGui.isItemHovered();
        ImGui.getWindowDrawList().addText(ImGui.getItemRectMinX(), ImGui.getItemRectMinY(), hovered ? hoveredColor : color, icon);
        if (hovered) {
            ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
            if (tooltip != null) ImGui.setTooltip(tooltip);
        }
        return clicked;
    }

    public static boolean imageButtonClear(int textureId, float width, float height, int args) {
        ImVec4 btnHovered = Settings.colorsCustom[0];
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, btnHovered.x, btnHovered.y, btnHovered.z, btnHovered.w);
        ImGui.pushStyleColor(ImGuiCol.Button, 0f, 0f, 0f, 0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0f, 0f, 0f, 0f);
        boolean result = ImGui.imageButton(textureId, width, height, 0f, 1f, 1f, 0f, args);
        ImGui.popStyleColor(3);
        return result;
    }

    /*
     * Asset<Texture> versions. Textures are stored bottom row first (stb flips them for OpenGL) while ImGui draws
     * top row first, so texture coordinates are flipped: uv0 = (0, 1), uv1 = (1, 0).
     * ImGui 1.86 uses the texture as an image button's id: the id parameter keeps two buttons with the same
     * icon in one window apart (e.g. "play", "file_" + index).
     */

    /** Draws the texture; an empty space of the same size while it isn't loaded, so layouts don't jump. */
    public static void image(Asset<Texture> texture, float width, float height) {
        int id = textureId(texture);
        if (id <= 0) {
            ImGui.dummy(width, height);
            return;
        }
        ImGui.image(id, width, height, 0, 1, 1, 0);
    }

    /** A clickable image; a "?" button of the same size while the texture isn't loaded. */
    public static boolean imageButton(String id, Asset<Texture> texture, float width, float height) {
        int texId = textureId(texture);
        ImGui.pushID(id);
        boolean clicked = texId <= 0
            ? ImGui.button("?", width + 2 * ImGui.getStyle().getFramePaddingX(), height + 2 * ImGui.getStyle().getFramePaddingY())
            : ImGui.imageButton(texId, width, height, 0, 1, 1, 0);
        ImGui.popID();
        return clicked;
    }

    /** imageButtonClear (no background until hovered) for an Asset<Texture>, with an id like imageButton's. */
    public static boolean imageButtonClear(String id, Asset<Texture> texture, float width, float height, int framePadding) {
        int texId = textureId(texture);
        ImGui.pushID(id);
        boolean clicked = texId <= 0
            ? ImGui.button("?", width + 2 * framePadding, height + 2 * framePadding)
            : imageButtonClear(texId, width, height, framePadding);
        ImGui.popID();
        return clicked;
    }

    /**
     * A clickable sprite: its region of the sheet's texture, at width x height pixels; a "?" button of the same
     * size while the texture isn't loaded. Sprite texture coordinates run [0] top-right ... [2] bottom-left, while
     * ImGui wants top-left then bottom-right, hence the mix of corners.
     */
    public static boolean spriteButton(String id, Sprite sprite, float width, float height) {
        int texId = sprite.getTexId();
        Vector2f[] uv = sprite.getTexCoords();
        ImGui.pushID(id);
        boolean clicked = texId <= 0
            ? ImGui.button("?", width + 2 * ImGui.getStyle().getFramePaddingX(), height + 2 * ImGui.getStyle().getFramePaddingY())
            : ImGui.imageButton(texId, width, height, uv[2].x, uv[0].y, uv[0].x, uv[2].y);
        ImGui.popID();
        return clicked;
    }

    private static int textureId(Asset<Texture> texture) {
        Texture t = texture == null ? null : texture.get();   // get(): the texture, or the placeholder, or null
        return t == null ? 0 : t.getId();
    }

    // ------------------------------------------------------------------------------------------------
    // Icon browser: every FontAwesomeIcons constant in a grid
    // ------------------------------------------------------------------------------------------------

    private static List<String[]> iconList;                      // {constant name, glyph}, sorted by name
    private static final ImString iconFilter = new ImString(64);
    private static final float[] iconColor = { 1f, 1f, 1f, 1f };   // RGBA 0..1, applied to every icon in the grid
    private static final float[] iconSize = { 16f };                // pixels; the font is rasterised at text size, so big sizes get soft
    private static final float ICON_CELL_PADDING = 14f;

    /** What was answered in unsavedChangesModal(); NONE while it's still waiting, or isn't open. */
    public enum SaveChoice { NONE, SAVED, DONT_SAVE, CANCEL }

    /**
     * A modal "Save All / Don't Save / Cancel" popup, centred on the main window. Call it every frame while it may be
     * open; pass open = true on the frame it should appear (ImGui only opens popups while it builds a frame).
     * Save All runs saveAll: the popup closes only if it returns true, otherwise it stays open showing error.
     * Escape counts as Cancel.
     */
    public static SaveChoice unsavedChangesModal(String id, boolean open, String question, List<String> unsaved,
                                                 String error, BooleanSupplier saveAll) {
        if (open) ImGui.openPopup(id);
        centerNextWindow(ImGuiCond.Appearing);
        if (!ImGui.beginPopupModal(id, ImGuiWindowFlags.AlwaysAutoResize)) return SaveChoice.NONE;

        ImGui.text(question);
        ImGui.spacing();
        for (String name : unsaved) ImGui.bulletText(name);
        if (error != null) {
            ImGui.spacing();
            ImGui.textColored(1.0f, 0.4f, 0.4f, 1.0f, error);
        }
        ImGui.spacing();
        ImGui.separator();

        SaveChoice choice = SaveChoice.NONE;
        if (ImGui.button("Save All") && saveAll.getAsBoolean()) choice = SaveChoice.SAVED;
        ImGui.sameLine();
        if (ImGui.button("Don't Save")) choice = SaveChoice.DONT_SAVE;
        ImGui.sameLine();
        if (ImGui.button("Cancel") || ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.Escape))) choice = SaveChoice.CANCEL;

        if (choice != SaveChoice.NONE) ImGui.closeCurrentPopup();
        ImGui.endPopup();
        return choice;
    }

    /**
     * A window with every Font Awesome icon in a grid. Hover an icon for its name; click it to copy
     * "FontAwesomeIcons.Name" to the clipboard. Call once per frame, like ImGui.showDemoWindow().
     */
    public static void showIconsExample() {
        if (iconList == null) iconList = loadIconList();

        ImGui.setNextWindowSize(640, 480, ImGuiCond.FirstUseEver);
        if (!ImGui.begin(FontAwesomeIcons.Icons + "  Icons")) {     // collapsed: draw nothing, but end() is still required
            ImGui.end();
            return;
        }

        String needle = iconFilter.get().trim().toLowerCase(Locale.ROOT);
        int shown = 0;
        for (String[] icon : iconList) if (matches(icon[0], needle)) shown++;

        ImGui.setNextItemWidth(250);
        ImGui.inputTextWithHint("##iconFilter", FontAwesomeIcons.MagnifyingGlass + "  filter by name...", iconFilter);
        ImGui.sameLine();
        // a colour swatch: click it for the picker; the colour applies to every icon below
        ImGui.colorEdit4("Color##iconColor", iconColor, ImGuiColorEditFlags.NoInputs | ImGuiColorEditFlags.AlphaBar);
        ImGui.sameLine();
        ImGui.setNextItemWidth(140);
        ImGui.sliderFloat("Size##iconSize", iconSize, 8f, 96f, "%.0f px");
        ImGui.sameLine();
        if (ImGui.smallButton("Reset##iconReset")) {
            iconColor[0] = iconColor[1] = iconColor[2] = iconColor[3] = 1f;
            iconSize[0] = ImGui.getFontSize();
        }
        ImGui.textDisabled(shown + " / " + iconList.size() + " icons   (hover: name, click: copy)");
        ImGui.separator();

        ImGui.beginChild("##iconGrid");
        float size = iconSize[0];
        float cell = size + ICON_CELL_PADDING;
        int color = ImGui.colorConvertFloat4ToU32(iconColor[0], iconColor[1], iconColor[2], iconColor[3]);
        float sizeRatio = size / ImGui.getFontSize();          // the glyphs' width at the base size, scaled to the chosen one
        ImDrawList drawList = ImGui.getWindowDrawList();
        boolean first = true;
        for (String[] icon : iconList) {
            if (!matches(icon[0], needle)) continue;
            if (!first) sameLineIfFits(cell);
            first = false;

            // an unlabelled button gives the cell, its hover/press background and the click ("##name": a unique id) ...
            boolean clicked = ImGui.button("##" + icon[0], cell, cell);
            if (clicked) ImGui.setClipboardText("FontAwesomeIcons." + icon[0]);
            if (ImGui.isItemHovered()) ImGui.setTooltip(icon[0]);

            // ... and the icon is drawn on top of it at the chosen size and colour, centred in the cell
            float glyphWidth = ImGui.calcTextSize(icon[1]).x * sizeRatio;
            float x = ImGui.getItemRectMinX() + (cell - glyphWidth) / 2;
            float y = ImGui.getItemRectMinY() + (cell - size) / 2;
            drawList.addText(ImGui.getFont(), size, x, y, color, icon[1]);
        }
        ImGui.endChild();
        ImGui.end();
    }

    private static boolean matches(String name, String needle) {
        return needle.isEmpty() || name.toLowerCase(Locale.ROOT).contains(needle);
    }

    /** Every public static String in FontAwesomeIcons, found by reflection so a regenerated class needs no change here. */
    private static List<String[]> loadIconList() {
        List<String[]> list = new ArrayList<>();
        for (Field field : FontAwesomeIcons.class.getFields()) {
            if (field.getType() != String.class || !Modifier.isStatic(field.getModifiers())) continue;
            try {
                list.add(new String[] { field.getName(), (String) field.get(null) });
            } catch (IllegalAccessException e) {
                // public fields: can't happen
            }
        }
        list.sort(Comparator.comparing(icon -> icon[0]));
        return list;
    }

}
