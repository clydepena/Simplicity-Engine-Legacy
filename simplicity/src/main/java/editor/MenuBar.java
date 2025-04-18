package editor;

import imgui.ImGui;
import observers.EventSystem;
import observers.events.Event;
import observers.events.EventType;
import simplicity.Window;

public class MenuBar extends ImGuiInterface {
    public void imgui(float dt) {
        ImGui.beginMenuBar();


        if(ImGui.beginMenu("File")) {

            if(ImGui.menuItem("Save", Window.getScene().getFilename())) {
                EventSystem.notify(new Event(EventType.SaveLevel));
            }

            if(ImGui.menuItem("Save As")) {
                EventSystem.notify(new Event(EventType.SaveLevelAs));
            }

            if(ImGui.menuItem("Load")) {
                EventSystem.notify(new Event(EventType.LoadLevel));
            }

            ImGui.endMenu();
        }

        if(ImGui.beginMenu("Editor")) {

            if(ImGui.beginMenu("Preferences")) {

                if(ImGui.beginMenu("Theme")) {

                    if (ImGui.menuItem("Default")) {
                        Window.getImGuiLayer().setUIColors(0);
                    }

                    if (ImGui.menuItem("Dark blue")) {
                        Window.getImGuiLayer().setUIColors(1);
                    }

                    if (ImGui.menuItem("Dark purple")) {
                        Window.getImGuiLayer().setUIColors(2);
                    }

                    if (ImGui.menuItem("Light")) {
                        Window.getImGuiLayer().setUIColors(3);
                    }

                    if (ImGui.menuItem("Gray")) {
                        Window.getImGuiLayer().setUIColors(4);
                    }

                    ImGui.endMenu();
                }

                ImGui.endMenu();
            }

            ImGui.endMenu();
        }

        ImGui.endMenuBar();
    }

    @Override
    public void destroy() {
       
    }
}
