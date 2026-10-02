package editor.panels;

import editor.SimplicityEditorContext;
import imgui.ImGui;
import observers.events.Event;
import simplicity.Application.RenderContext;

public class LauncherPanel extends SimplicityPanel {

    public LauncherPanel(SimplicityEditorContext editorContext) {
        super(editorContext);
    }

    @Override
    public void onUpdate(float dt) {

    }

    @Override
    public void onRender(RenderContext renderContext) {
        ImGui.showDemoWindow();
    }

    @Override
    public void destroy() {

    }

    @Override
    public void onEvent(Event event) {

    }
}