package editor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.joml.Vector2f;
import org.joml.Vector4f;

import asset.Asset;
import imgui.ImDrawList;
import imgui.ImGui;
import renderer.Texture;
import imgui.ImGuiStyle;
import imgui.ImVec4;
import imgui.flag.*;
import imgui.type.ImString;
import util.Settings;

public class SImGui {

    private static float defaultColumnWidth = 80.0f;
    
    public static void drawVec2fControl(String label, Vector2f values) {
        drawVec2fControl(label, values, 0.0f, defaultColumnWidth);
    }

    public static void drawVec2fControl(String label, Vector2f values, float resetValue) {
        drawVec2fControl(label, values, resetValue, defaultColumnWidth);
    }

    public static void drawVec2fControl(String label, Vector2f values, float resetValue, float columnWidth) {
        ImGui.pushID(label);
        ImGui.columns(2);
        ImGui.setColumnWidth(0, columnWidth);
        ImGui.text(label);
        ImGui.nextColumn();

        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0, 0);
        float lineHeight = ImGui.getFontSize() + ImGui.getStyle().getFramePaddingY() * 1.8f;
        Vector2f buttonSize = new Vector2f(lineHeight + 3.0f, lineHeight);
        float widthEach = (ImGui.calcItemWidth() - buttonSize.x * 2.0f) / 2.0f;

        ImGui.pushItemWidth(widthEach);
        ImGui.pushStyleColor(ImGuiCol.Button, 0.8f, 0.1f, 0.15f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.9f, 0.2f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.8f, 0.1f, 0.15f, 1.0f);
        if(ImGui.button("X", buttonSize.x, buttonSize.y)) {
            values.x = resetValue;
        }
        ImGui.popStyleColor(3);
        ImGui.sameLine();
        float[] vecValuesX = {values.x};
        ImGui.dragFloat("##x", vecValuesX, 0.1f);
        ImGui.popItemWidth();
        ImGui.sameLine();



        ImGui.pushItemWidth(widthEach);
        ImGui.pushStyleColor(ImGuiCol.Button, 0.1f, 0.8f, 0.15f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.2f, 0.9f, 0.2f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.1f, 0.8f, 0.15f, 1.0f);
        if(ImGui.button("Y", buttonSize.x, buttonSize.y)) {
            values.y = resetValue;
        }
        ImGui.popStyleColor(3);
        ImGui.sameLine();
        float[] vecValuesY = {values.y};
        ImGui.dragFloat("##y", vecValuesY, 0.1f);
        ImGui.popItemWidth();
        ImGui.sameLine();

        ImGui.nextColumn();

        values.x = vecValuesX[0];
        values.y = vecValuesY[0];

        ImGui.popStyleVar();
        ImGui.columns(1);
        ImGui.popID();
    }

    public static float dragFloat(String label, float values) {
        ImGui.pushID(label);
        ImGui.columns(2);
        ImGui.setColumnWidth(0, defaultColumnWidth);
        ImGui.text(label);
        ImGui.nextColumn();

        float[] valArr = {values};
        ImGui.dragFloat("##dragFloat", valArr, 0.1f);

        ImGui.columns(1);
        ImGui.popID();

        return valArr[0];
    }

    public static int dragInt(String label, int values) {
        ImGui.pushID(label);
        ImGui.columns(2);
        ImGui.setColumnWidth(0, defaultColumnWidth);
        ImGui.text(label);
        ImGui.nextColumn();

        int[] valArr = {values};
        ImGui.dragInt("##dragInt", valArr, 0.1f);

        ImGui.columns(1);
        ImGui.popID();

        return valArr[0];
    }

    public static boolean colorPicker4(String label, Vector4f color) {
        boolean res = false;
        ImGui.pushID(label);
        ImGui.columns(2);
        ImGui.setColumnWidth(0, defaultColumnWidth);
        ImGui.text(label);
        ImGui.nextColumn();

        float[] imColor = {color.x, color.y, color.z, color.w};
        if(ImGui.colorEdit4("##colorPicker", imColor, ImGuiColorEditFlags.AlphaBar)) {
            color.set(imColor[0], imColor[1], imColor[2], imColor[3]);
            res = true;
        }

        ImGui.columns(1);
        ImGui.popID();

        return res;
    }

    public static String inputText(String label, String text) {
        ImGui.pushID(label);
        ImGui.columns(2);
        ImGui.setColumnWidth(0, defaultColumnWidth);
        ImGui.text(label);
        ImGui.nextColumn();

        ImString outString = new ImString(text, 256);
        if (ImGui.inputText("##" + label, outString, ImGuiInputTextFlags.EnterReturnsTrue)) {
            ImGui.columns(1);
            ImGui.popID();

            return outString.get();
        }

        ImGui.columns(1);
        ImGui.popID();

        return text;
    }

    public static void textColoredAligned(float r, float g, float b, float a, String text, float alignment) {
        if (alignment > 0.0f) {
            ImGuiStyle style = ImGui.getStyle();
            float size = ImGui.calcTextSize(text).x + style.getFramePaddingX() * 2.0f;
            float avail = ImGui.getContentRegionAvail().x;
            float off = (avail - size) * (alignment > 1.0f ? 1.0f : alignment);
            if (off > 0.0f) {
                ImGui.setCursorPosX(ImGui.getCursorPosX() + off);
            }
        }
        ImGui.textColored(r, g, b, a, text);
    }

    public static void textAligned(String text, float alignment) {
        if (alignment > 0.0f) {
            ImGuiStyle style = ImGui.getStyle();
            float size = ImGui.calcTextSize(text).x + style.getFramePaddingX() * 2.0f;
            float avail = ImGui.getContentRegionAvail().x;
            float off = (avail - size) * (alignment > 1.0f ? 1.0f : alignment);
            if (off > 0.0f) {
                ImGui.setCursorPosX(ImGui.getCursorPosX() + off);
            }
        }
        ImGui.text(text);
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
        float spacing = ImGui.getStyle().getItemSpacingX();
        int columns = Math.max(1, (int) ((ImGui.getContentRegionAvailX() + spacing) / (cell + spacing)));
        int color = ImGui.colorConvertFloat4ToU32(iconColor[0], iconColor[1], iconColor[2], iconColor[3]);
        float sizeRatio = size / ImGui.getFontSize();          // the glyphs' width at the base size, scaled to the chosen one
        ImDrawList drawList = ImGui.getWindowDrawList();
        int column = 0;
        for (String[] icon : iconList) {
            if (!matches(icon[0], needle)) continue;
            if (column > 0) ImGui.sameLine();

            // an unlabelled button gives the cell, its hover/press background and the click ("##name": a unique id) ...
            boolean clicked = ImGui.button("##" + icon[0], cell, cell);
            if (clicked) ImGui.setClipboardText("FontAwesomeIcons." + icon[0]);
            if (ImGui.isItemHovered()) ImGui.setTooltip(icon[0]);

            // ... and the icon is drawn on top of it at the chosen size and colour, centred in the cell
            float glyphWidth = ImGui.calcTextSize(icon[1]).x * sizeRatio;
            float x = ImGui.getItemRectMinX() + (cell - glyphWidth) / 2;
            float y = ImGui.getItemRectMinY() + (cell - size) / 2;
            drawList.addText(ImGui.getFont(), size, x, y, color, icon[1]);

            column = (column + 1) % columns;
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
