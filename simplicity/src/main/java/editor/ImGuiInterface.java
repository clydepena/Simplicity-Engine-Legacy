package editor;

import imgui.*;

public abstract class ImGuiInterface {
    
    protected ImVec2 winPosition;
    protected ImVec2 winSize;
    protected boolean isDocked, isFocused, isHovered, isEnabled = true;

    public abstract void imgui(float dt);
    public abstract void destroy();

    protected void updateCalc() {
        this.winPosition = ImGui.getWindowPos();
        this.winSize = ImGui.getWindowSize();
        this.isDocked = ImGui.isWindowDocked();
        this.isFocused = ImGui.isWindowFocused();
        this.isHovered = ImGui.isWindowHovered();
    }
}
