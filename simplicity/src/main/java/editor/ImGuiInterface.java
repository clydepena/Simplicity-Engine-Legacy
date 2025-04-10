package editor;

import imgui.*;

public abstract class ImGuiInterface {
    
    protected ImVec2 position;
    protected ImVec2 size;
    protected boolean isDocked, isFocused, isHovered;

    public abstract void imgui();

    protected void updateCalc() {
        this.position = ImGui.getWindowPos();
        this.size = ImGui.getWindowSize();
        this.isDocked = ImGui.isWindowDocked();
        this.isFocused = ImGui.isWindowFocused();
        this.isHovered = ImGui.isWindowHovered();
    }
}
