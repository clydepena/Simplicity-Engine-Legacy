package editor;

import imgui.ImGui;
import observers.EventSystem;
import observers.events.Event;
import observers.events.EventType;
import simplicity.Window;

public class MenuBar extends ImGuiInterface {
    public void imgui() {
        ImGui.beginMenuBar();


        if(ImGui.beginMenu("File")) {

            if(ImGui.menuItem("Save", Window.getScene().getFilename())) {
                EventSystem.notify(null, new Event(EventType.SaveLevel));
            }

            if(ImGui.menuItem("Save As")) {
                EventSystem.notify(null, new Event(EventType.SaveLevelAs));
            }

            if(ImGui.menuItem("Load")) {
                EventSystem.notify(null, new Event(EventType.LoadLevel));
            }

            ImGui.endMenu();
        }

        ImGui.endMenuBar();
    }

    @Override
    public void destroy() {
       
    }
}
