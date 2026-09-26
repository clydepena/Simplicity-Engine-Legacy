package editor.panels;

import editor.EditorContext;
import editor.ImGuiEditorLayer;
import imgui.ImGui;
import imgui.ImVec2;
import observers.events.Event;
import simplicity.Application.RenderContext;

public abstract class EditorPanel<L extends ImGuiEditorLayer, C extends EditorContext<L>> {

    public final C editorContext;
    public ImVec2 winPosition;
    public ImVec2 winSize;
    public boolean isDocked, isFocused, isHovered, isEnabled = true;

    public EditorPanel(C editorContext) {
        this.editorContext = editorContext;
    }
    
    public abstract void onUpdate(float dt);
    public abstract void onRender(RenderContext renderContext);
    public abstract void destroy();
    public abstract void onEvent(Event event);

    protected void updateCalc() {
        this.winPosition = ImGui.getWindowPos();
        this.winSize = ImGui.getWindowSize();
        this.isDocked = ImGui.isWindowDocked();
        this.isFocused = ImGui.isWindowFocused();
        this.isHovered = ImGui.isWindowHovered();
    }
}
