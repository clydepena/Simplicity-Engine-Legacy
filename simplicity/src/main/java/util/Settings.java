package util;

import imgui.ImVec4;
import imgui.flag.ImGuiCol;

public class Settings {
    public static float GRID_WIDTH = 0.25f;
    public static float GRID_HEIGHT = 0.25f;

    public static final float FONT_SIZE = 16f;
    public static final int STYLE_COLOR_DEF = 0;
    public static final int STYLE_COLOR_BLUE = 1;
    public static final int STYLE_COLOR_PURPLE = 2;
    public static final int STYLE_COLOR_LIGHT = 3;
    public static final int STYLE_COLOR_GRAY = 4;

    public static ImVec4[] colorsCustom;

    public static ImVec4[] getStyleColors(int style) {
        ImVec4[] colors = new ImVec4[ImGuiCol.COUNT];
        switch (style) {
            case 0:
                getDefaultColors(colors);
                break;
            case 1:
                getBlueColors(colors);
                break;
            case 2:
                getPurpleColors(colors);
                break;
            case 3:
                getLightColors(colors);
                break;
            case 4:
                getGrayColors(colors);
                break;
            default:
                getDefaultColors(colors);
                break;
        }
        setCustomColors(colors);
        return colors;
    }

    private static void setCustomColors(ImVec4[] colors) {
        colorsCustom = new ImVec4[1];
        ImVec4 tmp = colors[ImGuiCol.ButtonHovered];
        colorsCustom[0] = new ImVec4(tmp.x, tmp.y, tmp.z, tmp.w * 0.25f);
    }

    private static ImVec4[] getDefaultColors(ImVec4[] colors) {
        colors[0]	= new ImVec4(1.0f,	1.0f,	1.0f,	1.0f);
        colors[1]	= new ImVec4(0.5f,	0.5f,	0.5f,	1.0f);
        colors[2]	= new ImVec4(0.06f,	0.06f,	0.06f,	0.94f);
        colors[3]	= new ImVec4(0.0f,	0.0f,	0.0f,	0.0f);
        colors[4]	= new ImVec4(0.08f,	0.08f,	0.08f,	0.94f);
        colors[5]	= new ImVec4(0.43f,	0.43f,	0.5f,	0.5f);
        colors[6]	= new ImVec4(0.0f,	0.0f,	0.0f,	0.0f);
        colors[7]	= new ImVec4(0.25f,	0.3f,	0.37f,	0.54f);
        colors[8]	= new ImVec4(0.25f,	0.3f,	0.37f,	0.74f);
        colors[9]	= new ImVec4(0.25f,	0.3f,	0.37f,	0.9f);
        colors[10]	= new ImVec4(0.04f,	0.04f,	0.04f,	1.0f);
        colors[11]	= new ImVec4(0.08f,	0.08f,	0.08f,	1.0f);
        colors[12]	= new ImVec4(0.0f,	0.0f,	0.0f,	0.51f);
        colors[13]	= new ImVec4(0.14f,	0.14f,	0.14f,	1.0f);
        colors[14]	= new ImVec4(0.02f,	0.02f,	0.02f,	0.53f);
        colors[15]	= new ImVec4(0.31f,	0.31f,	0.31f,	1.0f);
        colors[16]	= new ImVec4(0.41f,	0.41f,	0.41f,	1.0f);
        colors[17]	= new ImVec4(0.51f,	0.51f,	0.51f,	1.0f);
        colors[18]	= new ImVec4(0.58f,	0.63f,	0.69f,	0.7f);
        colors[19]	= new ImVec4(0.58f,	0.63f,	0.69f,	0.7f);
        colors[20]	= new ImVec4(0.66f,	0.71f,	0.78f,	1.0f);
        colors[21]	= new ImVec4(0.25f,	0.3f,	0.37f,	0.74f);
        colors[22]	= new ImVec4(0.58f,	0.63f,	0.69f,	0.7f);
        colors[23]	= new ImVec4(0.58f,	0.63f,	0.69f,	1.0f);
        colors[24]	= new ImVec4(0.58f,	0.63f,	0.69f,	0.3f);
        colors[25]	= new ImVec4(0.58f,	0.63f,	0.69f,	0.5f);
        colors[26]	= new ImVec4(0.58f,	0.63f,	0.69f,	0.7f);
        colors[27]	= new ImVec4(0.14f,	0.14f,	0.14f,	0.94f);
        colors[28]	= new ImVec4(0.24f,	0.24f,	0.24f,	1.0f);
        colors[29]	= new ImVec4(0.59f,	0.29f,	0.0f,	1.0f);
        colors[30]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.2f);
        colors[31]	= new ImVec4(0.25f,	0.3f,	0.37f,	0.9f);
        colors[32]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.95f);
        colors[33]	= new ImVec4(0.29f,	0.35f,	0.43f,	0.54f);
        colors[34]	= new ImVec4(0.49f,	0.24f,	0.0f,	1.0f);
        colors[35]	= new ImVec4(0.59f,	0.29f,	0.0f,	1.0f);
        colors[36]	= new ImVec4(0.12f,	0.15f,	0.18f,	0.68f);
        colors[37]	= new ImVec4(0.14f,	0.14f,	0.14f,	0.94f);
        colors[38]	= new ImVec4(0.75f,	0.37f,	0.0f,	0.69f);
        colors[39]	= new ImVec4(0.2f,	0.2f,	0.2f,	1.0f);
        colors[40]	= new ImVec4(0.61f,	0.61f,	0.61f,	1.0f);
        colors[41]	= new ImVec4(1.0f,	0.43f,	0.35f,	1.0f);
        colors[42]	= new ImVec4(0.9f,	0.7f,	0.0f,	1.0f);
        colors[43]	= new ImVec4(1.0f,	0.6f,	0.0f,	1.0f);
        colors[44]	= new ImVec4(0.19f,	0.19f,	0.2f,	1.0f);
        colors[45]	= new ImVec4(0.31f,	0.31f,	0.35f,	1.0f);
        colors[46]	= new ImVec4(0.23f,	0.23f,	0.25f,	1.0f);
        colors[47]	= new ImVec4(0.0f,	0.0f,	0.0f,	0.0f);
        colors[48]	= new ImVec4(1.0f,	1.0f,	1.0f,	0.06f);
        colors[49]	= new ImVec4(0.75f,	0.37f,	0.0f,	0.39f);
        colors[50]	= new ImVec4(1.0f,	1.0f,	0.0f,	0.9f);
        colors[51]	= new ImVec4(0.58f,	0.63f,	0.69f,	1.0f);
        colors[52]	= new ImVec4(1.0f,	1.0f,	1.0f,	0.7f);
        colors[53]	= new ImVec4(0.8f,	0.8f,	0.8f,	0.2f);
        colors[54]	= new ImVec4(0.8f,	0.8f,	0.8f,	0.35f);
        return colors;
    }

    private static ImVec4[] getGrayColors(ImVec4[] colors) {
        colors[ImGuiCol.Text]                   = new ImVec4(1.00f, 1.00f, 1.00f, 1.00f);
        colors[ImGuiCol.TextDisabled]           = new ImVec4(0.50f, 0.50f, 0.50f, 1.00f);
        colors[ImGuiCol.WindowBg]               = new ImVec4(0.06f, 0.06f, 0.06f, 0.94f);
        colors[ImGuiCol.ChildBg]                = new ImVec4(0.00f, 0.00f, 0.00f, 0.00f);
        colors[ImGuiCol.PopupBg]                = new ImVec4(0.08f, 0.08f, 0.08f, 0.94f);
        colors[ImGuiCol.Border]                 = new ImVec4(0.43f, 0.43f, 0.50f, 0.50f);
        colors[ImGuiCol.BorderShadow]           = new ImVec4(0.00f, 0.00f, 0.00f, 0.00f);
        colors[ImGuiCol.FrameBg]                = new ImVec4(0.25f, 0.30f, 0.37f, 0.54f);
        colors[ImGuiCol.FrameBgHovered]         = new ImVec4(0.26f, 0.59f, 0.98f, 0.40f);
        colors[ImGuiCol.FrameBgActive]          = new ImVec4(0.26f, 0.59f, 0.98f, 0.67f);
        colors[ImGuiCol.TitleBg]                = new ImVec4(0.04f, 0.04f, 0.04f, 1.00f);
        colors[ImGuiCol.TitleBgActive]          = new ImVec4(0.16f, 0.29f, 0.48f, 1.00f);
        colors[ImGuiCol.TitleBgCollapsed]       = new ImVec4(0.00f, 0.00f, 0.00f, 0.51f);
        colors[ImGuiCol.MenuBarBg]              = new ImVec4(0.14f, 0.14f, 0.14f, 1.00f);
        colors[ImGuiCol.ScrollbarBg]            = new ImVec4(0.02f, 0.02f, 0.02f, 0.53f);
        colors[ImGuiCol.ScrollbarGrab]          = new ImVec4(0.31f, 0.31f, 0.31f, 1.00f);
        colors[ImGuiCol.ScrollbarGrabHovered]   = new ImVec4(0.41f, 0.41f, 0.41f, 1.00f);
        colors[ImGuiCol.ScrollbarGrabActive]    = new ImVec4(0.51f, 0.51f, 0.51f, 1.00f);
        colors[ImGuiCol.CheckMark]              = new ImVec4(0.26f, 0.59f, 0.98f, 1.00f);
        colors[ImGuiCol.SliderGrab]             = new ImVec4(0.24f, 0.52f, 0.88f, 1.00f);
        colors[ImGuiCol.SliderGrabActive]       = new ImVec4(0.26f, 0.59f, 0.98f, 1.00f);
        colors[ImGuiCol.Button]                 = new ImVec4(0.26f, 0.59f, 0.98f, 0.40f);
        colors[ImGuiCol.ButtonHovered]          = new ImVec4(0.26f, 0.59f, 0.98f, 1.00f);
        colors[ImGuiCol.ButtonActive]           = new ImVec4(0.06f, 0.53f, 0.98f, 1.00f);
        colors[ImGuiCol.Header]                 = new ImVec4(0.58f, 0.63f, 0.69f, 0.30f);
        colors[ImGuiCol.HeaderHovered]          = new ImVec4(0.58f, 0.63f, 0.69f, 0.50f);
        colors[ImGuiCol.HeaderActive]           = new ImVec4(0.58f, 0.63f, 0.69f, 0.50f);
        colors[ImGuiCol.Separator]              = new ImVec4(0.43f, 0.43f, 0.50f, 0.50f);
        colors[ImGuiCol.SeparatorHovered]       = new ImVec4(0.10f, 0.40f, 0.75f, 0.78f);
        colors[ImGuiCol.SeparatorActive]        = new ImVec4(0.10f, 0.40f, 0.75f, 1.00f);
        colors[ImGuiCol.ResizeGrip]             = new ImVec4(0.26f, 0.59f, 0.98f, 0.20f);
        colors[ImGuiCol.ResizeGripHovered]      = new ImVec4(0.26f, 0.59f, 0.98f, 0.67f);
        colors[ImGuiCol.ResizeGripActive]       = new ImVec4(0.26f, 0.59f, 0.98f, 0.95f);
        colors[ImGuiCol.Tab]                    = new ImVec4(0.18f, 0.35f, 0.58f, 0.86f);
        colors[ImGuiCol.TabHovered]             = new ImVec4(0.26f, 0.59f, 0.98f, 0.80f);
        colors[ImGuiCol.TabActive]              = new ImVec4(0.20f, 0.41f, 0.68f, 1.00f);
        colors[ImGuiCol.TabUnfocused]           = new ImVec4(0.07f, 0.10f, 0.15f, 0.97f);
        colors[ImGuiCol.TabUnfocusedActive]     = new ImVec4(0.14f, 0.26f, 0.42f, 1.00f);
        colors[ImGuiCol.DockingPreview]         = new ImVec4(0.26f, 0.59f, 0.98f, 0.70f);
        colors[ImGuiCol.DockingEmptyBg]         = new ImVec4(0.20f, 0.20f, 0.20f, 1.00f);
        colors[ImGuiCol.PlotLines]              = new ImVec4(0.61f, 0.61f, 0.61f, 1.00f);
        colors[ImGuiCol.PlotLinesHovered]       = new ImVec4(1.00f, 0.43f, 0.35f, 1.00f);
        colors[ImGuiCol.PlotHistogram]          = new ImVec4(0.90f, 0.70f, 0.00f, 1.00f);
        colors[ImGuiCol.PlotHistogramHovered]   = new ImVec4(1.00f, 0.60f, 0.00f, 1.00f);
        colors[ImGuiCol.TableHeaderBg]          = new ImVec4(0.19f, 0.19f, 0.20f, 1.00f);
        colors[ImGuiCol.TableBorderStrong]      = new ImVec4(0.31f, 0.31f, 0.35f, 1.00f);
        colors[ImGuiCol.TableBorderLight]       = new ImVec4(0.23f, 0.23f, 0.25f, 1.00f);
        colors[ImGuiCol.TableRowBg]             = new ImVec4(0.00f, 0.00f, 0.00f, 0.00f);
        colors[ImGuiCol.TableRowBgAlt]          = new ImVec4(1.00f, 1.00f, 1.00f, 0.06f);
        colors[ImGuiCol.TextSelectedBg]         = new ImVec4(0.26f, 0.59f, 0.98f, 0.35f);
        colors[ImGuiCol.DragDropTarget]         = new ImVec4(1.00f, 1.00f, 0.00f, 0.90f);
        colors[ImGuiCol.NavHighlight]           = new ImVec4(0.26f, 0.59f, 0.98f, 1.00f);
        colors[ImGuiCol.NavWindowingHighlight]  = new ImVec4(1.00f, 1.00f, 1.00f, 0.70f);
        colors[ImGuiCol.NavWindowingDimBg]      = new ImVec4(0.80f, 0.80f, 0.80f, 0.20f);
        colors[ImGuiCol.ModalWindowDimBg]       = new ImVec4(0.80f, 0.80f, 0.80f, 0.35f);
        return colors;
    }

    private static ImVec4[] getLightColors(ImVec4[] colors) {
        colors[0]	= new ImVec4(0.0f,	0.0f,	0.0f,	1.0f);
        colors[1]	= new ImVec4(0.6f,	0.6f,	0.6f,	1.0f);
        colors[2]	= new ImVec4(0.89f,	0.89f,	0.89f,	0.76f);
        colors[3]	= new ImVec4(0.0f,	0.0f,	0.0f,	0.0f);
        colors[4]	= new ImVec4(1.0f,	1.0f,	1.0f,	0.98f);
        colors[5]	= new ImVec4(0.0f,	0.0f,	0.0f,	0.3f);
        colors[6]	= new ImVec4(0.0f,	0.0f,	0.0f,	0.0f);
        colors[7]	= new ImVec4(0.73f,	0.73f,	0.73f,	0.48f);
        colors[8]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.4f);
        colors[9]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.67f);
        colors[10]	= new ImVec4(0.96f,	0.96f,	0.96f,	1.0f);
        colors[11]	= new ImVec4(0.82f,	0.82f,	0.82f,	1.0f);
        colors[12]	= new ImVec4(1.0f,	1.0f,	1.0f,	0.51f);
        colors[13]	= new ImVec4(0.86f,	0.86f,	0.86f,	1.0f);
        colors[14]	= new ImVec4(0.98f,	0.98f,	0.98f,	0.53f);
        colors[15]	= new ImVec4(0.69f,	0.69f,	0.69f,	0.8f);
        colors[16]	= new ImVec4(0.49f,	0.49f,	0.49f,	0.8f);
        colors[17]	= new ImVec4(0.49f,	0.49f,	0.49f,	1.0f);
        colors[18]	= new ImVec4(0.26f,	0.59f,	0.98f,	1.0f);
        colors[19]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.78f);
        colors[20]	= new ImVec4(0.46f,	0.54f,	0.8f,	0.6f);
        colors[21]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.4f);
        colors[22]	= new ImVec4(0.26f,	0.59f,	0.98f,	1.0f);
        colors[23]	= new ImVec4(0.06f,	0.53f,	0.98f,	1.0f);
        colors[24]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.31f);
        colors[25]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.8f);
        colors[26]	= new ImVec4(0.26f,	0.59f,	0.98f,	1.0f);
        colors[27]	= new ImVec4(0.39f,	0.39f,	0.39f,	0.62f);
        colors[28]	= new ImVec4(0.14f,	0.44f,	0.8f,	0.78f);
        colors[29]	= new ImVec4(0.14f,	0.44f,	0.8f,	1.0f);
        colors[30]	= new ImVec4(0.35f,	0.35f,	0.35f,	0.17f);
        colors[31]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.67f);
        colors[32]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.95f);
        colors[33]	= new ImVec4(0.76f,	0.8f,	0.84f,	0.93f);
        colors[34]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.8f);
        colors[35]	= new ImVec4(0.6f,	0.73f,	0.88f,	1.0f);
        colors[36]	= new ImVec4(0.92f,	0.93f,	0.94f,	0.99f);
        colors[37]	= new ImVec4(0.74f,	0.82f,	0.91f,	1.0f);
        colors[38]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.22f);
        colors[39]	= new ImVec4(0.2f,	0.2f,	0.2f,	1.0f);
        colors[40]	= new ImVec4(0.39f,	0.39f,	0.39f,	1.0f);
        colors[41]	= new ImVec4(1.0f,	0.43f,	0.35f,	1.0f);
        colors[42]	= new ImVec4(0.9f,	0.7f,	0.0f,	1.0f);
        colors[43]	= new ImVec4(1.0f,	0.45f,	0.0f,	1.0f);
        colors[44]	= new ImVec4(0.78f,	0.87f,	0.98f,	1.0f);
        colors[45]	= new ImVec4(0.57f,	0.57f,	0.64f,	1.0f);
        colors[46]	= new ImVec4(0.68f,	0.68f,	0.74f,	1.0f);
        colors[47]	= new ImVec4(0.0f,	0.0f,	0.0f,	0.0f);
        colors[48]	= new ImVec4(0.3f,	0.3f,	0.3f,	0.09f);
        colors[49]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.35f);
        colors[50]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.95f);
        colors[51]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.8f);
        colors[52]	= new ImVec4(0.7f,	0.7f,	0.7f,	0.7f);
        colors[53]	= new ImVec4(0.2f,	0.2f,	0.2f,	0.2f);
        colors[54]	= new ImVec4(0.2f,	0.2f,	0.2f,	0.35f);
        return colors;
    }

    private static ImVec4[] getBlueColors(ImVec4[] colors) {
        colors[0]	= new ImVec4(1.0f,	1.0f,	1.0f,	1.0f);
        colors[1]	= new ImVec4(0.5f,	0.5f,	0.5f,	1.0f);
        colors[2]	= new ImVec4(0.06f,	0.06f,	0.06f,	0.94f);
        colors[3]	= new ImVec4(0.0f,	0.0f,	0.0f,	0.0f);
        colors[4]	= new ImVec4(0.08f,	0.08f,	0.08f,	0.94f);
        colors[5]	= new ImVec4(0.43f,	0.43f,	0.5f,	0.5f);
        colors[6]	= new ImVec4(0.0f,	0.0f,	0.0f,	0.0f);
        colors[7]	= new ImVec4(0.16f,	0.29f,	0.48f,	0.54f);
        colors[8]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.4f);
        colors[9]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.67f);
        colors[10]	= new ImVec4(0.04f,	0.04f,	0.04f,	1.0f);
        colors[11]	= new ImVec4(0.16f,	0.29f,	0.48f,	1.0f);
        colors[12]	= new ImVec4(0.0f,	0.0f,	0.0f,	0.51f);
        colors[13]	= new ImVec4(0.14f,	0.14f,	0.14f,	1.0f);
        colors[14]	= new ImVec4(0.02f,	0.02f,	0.02f,	0.53f);
        colors[15]	= new ImVec4(0.31f,	0.31f,	0.31f,	1.0f);
        colors[16]	= new ImVec4(0.41f,	0.41f,	0.41f,	1.0f);
        colors[17]	= new ImVec4(0.51f,	0.51f,	0.51f,	1.0f);
        colors[18]	= new ImVec4(0.26f,	0.59f,	0.98f,	1.0f);
        colors[19]	= new ImVec4(0.24f,	0.52f,	0.88f,	1.0f);
        colors[20]	= new ImVec4(0.26f,	0.59f,	0.98f,	1.0f);
        colors[21]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.4f);
        colors[22]	= new ImVec4(0.26f,	0.59f,	0.98f,	1.0f);
        colors[23]	= new ImVec4(0.06f,	0.53f,	0.98f,	1.0f);
        colors[24]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.31f);
        colors[25]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.8f);
        colors[26]	= new ImVec4(0.26f,	0.59f,	0.98f,	1.0f);
        colors[27]	= new ImVec4(0.43f,	0.43f,	0.5f,	0.5f);
        colors[28]	= new ImVec4(0.1f,	0.4f,	0.75f,	0.78f);
        colors[29]	= new ImVec4(0.1f,	0.4f,	0.75f,	1.0f);
        colors[30]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.2f);
        colors[31]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.67f);
        colors[32]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.95f);
        colors[33]	= new ImVec4(0.18f,	0.35f,	0.58f,	0.86f);
        colors[34]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.8f);
        colors[35]	= new ImVec4(0.2f,	0.41f,	0.68f,	1.0f);
        colors[36]	= new ImVec4(0.07f,	0.1f,	0.15f,	0.97f);
        colors[37]	= new ImVec4(0.14f,	0.26f,	0.42f,	1.0f);
        colors[38]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.7f);
        colors[39]	= new ImVec4(0.2f,	0.2f,	0.2f,	1.0f);
        colors[40]	= new ImVec4(0.61f,	0.61f,	0.61f,	1.0f);
        colors[41]	= new ImVec4(1.0f,	0.43f,	0.35f,	1.0f);
        colors[42]	= new ImVec4(0.9f,	0.7f,	0.0f,	1.0f);
        colors[43]	= new ImVec4(1.0f,	0.6f,	0.0f,	1.0f);
        colors[44]	= new ImVec4(0.19f,	0.19f,	0.2f,	1.0f);
        colors[45]	= new ImVec4(0.31f,	0.31f,	0.35f,	1.0f);
        colors[46]	= new ImVec4(0.23f,	0.23f,	0.25f,	1.0f);
        colors[47]	= new ImVec4(0.0f,	0.0f,	0.0f,	0.0f);
        colors[48]	= new ImVec4(1.0f,	1.0f,	1.0f,	0.06f);
        colors[49]	= new ImVec4(0.26f,	0.59f,	0.98f,	0.35f);
        colors[50]	= new ImVec4(1.0f,	1.0f,	0.0f,	0.9f);
        colors[51]	= new ImVec4(0.26f,	0.59f,	0.98f,	1.0f);
        colors[52]	= new ImVec4(1.0f,	1.0f,	1.0f,	0.7f);
        colors[53]	= new ImVec4(0.8f,	0.8f,	0.8f,	0.2f);
        colors[54]	= new ImVec4(0.8f,	0.8f,	0.8f,	0.35f);
        return colors;
    }

    private static ImVec4[] getPurpleColors(ImVec4[] colors) {
        colors[0]	= new ImVec4(0.9f,	0.9f,	0.9f,	1.0f);
        colors[1]	= new ImVec4(0.6f,	0.6f,	0.6f,	1.0f);
        colors[2]	= new ImVec4(0.0f,	0.0f,	0.0f,	0.85f);
        colors[3]	= new ImVec4(0.0f,	0.0f,	0.0f,	0.0f);
        colors[4]	= new ImVec4(0.11f,	0.11f,	0.14f,	0.92f);
        colors[5]	= new ImVec4(0.5f,	0.5f,	0.5f,	0.5f);
        colors[6]	= new ImVec4(0.0f,	0.0f,	0.0f,	0.0f);
        colors[7]	= new ImVec4(0.43f,	0.43f,	0.43f,	0.39f);
        colors[8]	= new ImVec4(0.47f,	0.47f,	0.69f,	0.4f);
        colors[9]	= new ImVec4(0.42f,	0.41f,	0.64f,	0.69f);
        colors[10]	= new ImVec4(0.27f,	0.27f,	0.54f,	0.83f);
        colors[11]	= new ImVec4(0.32f,	0.32f,	0.63f,	0.87f);
        colors[12]	= new ImVec4(0.4f,	0.4f,	0.8f,	0.2f);
        colors[13]	= new ImVec4(0.4f,	0.4f,	0.55f,	0.8f);
        colors[14]	= new ImVec4(0.2f,	0.25f,	0.3f,	0.6f);
        colors[15]	= new ImVec4(0.4f,	0.4f,	0.8f,	0.3f);
        colors[16]	= new ImVec4(0.4f,	0.4f,	0.8f,	0.4f);
        colors[17]	= new ImVec4(0.41f,	0.39f,	0.8f,	0.6f);
        colors[18]	= new ImVec4(0.9f,	0.9f,	0.9f,	0.5f);
        colors[19]	= new ImVec4(1.0f,	1.0f,	1.0f,	0.3f);
        colors[20]	= new ImVec4(0.41f,	0.39f,	0.8f,	0.6f);
        colors[21]	= new ImVec4(0.35f,	0.4f,	0.61f,	0.62f);
        colors[22]	= new ImVec4(0.4f,	0.48f,	0.71f,	0.79f);
        colors[23]	= new ImVec4(0.46f,	0.54f,	0.8f,	1.0f);
        colors[24]	= new ImVec4(0.4f,	0.4f,	0.9f,	0.45f);
        colors[25]	= new ImVec4(0.45f,	0.45f,	0.9f,	0.8f);
        colors[26]	= new ImVec4(0.53f,	0.53f,	0.87f,	0.8f);
        colors[27]	= new ImVec4(0.5f,	0.5f,	0.5f,	0.6f);
        colors[28]	= new ImVec4(0.6f,	0.6f,	0.7f,	1.0f);
        colors[29]	= new ImVec4(0.7f,	0.7f,	0.9f,	1.0f);
        colors[30]	= new ImVec4(1.0f,	1.0f,	1.0f,	0.1f);
        colors[31]	= new ImVec4(0.78f,	0.82f,	1.0f,	0.6f);
        colors[32]	= new ImVec4(0.78f,	0.82f,	1.0f,	0.9f);
        colors[33]	= new ImVec4(0.34f,	0.34f,	0.68f,	0.79f);
        colors[34]	= new ImVec4(0.45f,	0.45f,	0.9f,	0.8f);
        colors[35]	= new ImVec4(0.4f,	0.4f,	0.73f,	0.84f);
        colors[36]	= new ImVec4(0.28f,	0.28f,	0.57f,	0.82f);
        colors[37]	= new ImVec4(0.35f,	0.35f,	0.65f,	0.84f);
        colors[38]	= new ImVec4(0.4f,	0.4f,	0.9f,	0.31f);
        colors[39]	= new ImVec4(0.2f,	0.2f,	0.2f,	1.0f);
        colors[40]	= new ImVec4(1.0f,	1.0f,	1.0f,	1.0f);
        colors[41]	= new ImVec4(0.9f,	0.7f,	0.0f,	1.0f);
        colors[42]	= new ImVec4(0.9f,	0.7f,	0.0f,	1.0f);
        colors[43]	= new ImVec4(1.0f,	0.6f,	0.0f,	1.0f);
        colors[44]	= new ImVec4(0.27f,	0.27f,	0.38f,	1.0f);
        colors[45]	= new ImVec4(0.31f,	0.31f,	0.45f,	1.0f);
        colors[46]	= new ImVec4(0.26f,	0.26f,	0.28f,	1.0f);
        colors[47]	= new ImVec4(0.0f,	0.0f,	0.0f,	0.0f);
        colors[48]	= new ImVec4(1.0f,	1.0f,	1.0f,	0.07f);
        colors[49]	= new ImVec4(0.0f,	0.0f,	1.0f,	0.35f);
        colors[50]	= new ImVec4(1.0f,	1.0f,	0.0f,	0.9f);
        colors[51]	= new ImVec4(0.45f,	0.45f,	0.9f,	0.8f);
        colors[52]	= new ImVec4(1.0f,	1.0f,	1.0f,	0.7f);
        colors[53]	= new ImVec4(0.8f,	0.8f,	0.8f,	0.2f);
        colors[54]	= new ImVec4(0.2f,	0.2f,	0.2f,	0.35f);
        return colors;
    }
}
