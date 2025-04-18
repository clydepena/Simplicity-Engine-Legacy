package editor;

import org.joml.Vector2f;
import org.joml.Vector4f;

import imgui.ImGui;
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

}
