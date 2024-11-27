package editor;

import org.joml.Vector2f;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiWindowFlags;
import observers.EventSystem;
import observers.events.Event;
import observers.events.EventType;
import simplicity.MouseListener;
import simplicity.Window;

public class GameViewWindow {
    
    private float leftX, rightX, topY, bottomY;
    private boolean  isPlaying = false;

    public void imgui() {
        ImGui.begin("Game Viewport", ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse | ImGuiWindowFlags.MenuBar);

        ImGui.beginMenuBar();
        if(ImGui.menuItem("Play", "", isPlaying, !isPlaying)) {
            isPlaying = true;
            EventSystem.notify(null, new Event(EventType.GameEngineStartPlay));
        }
        if(ImGui.menuItem("Stop", "", !isPlaying, isPlaying)) {
            isPlaying = false;
            EventSystem.notify(null, new Event(EventType.GameEngineStopPlay));
        }
        ImGui.endMenuBar();

        ImGui.setCursorPos(ImGui.getCursorPosX(), ImGui.getCursorPosY());
        // System.out.println(ImGui.getCursorPosX() + " | " + ImGui.getCursorPosY());

        ImVec2 windowSize = getLargestSizeForViewport();
        ImVec2 windowPos = getCenteredPositionForViewport(windowSize);
        ImGui.setCursorPos(windowPos.x, windowPos.y);

        // ImVec2 topLeft = new ImVec2();
        // ImGui.getCursorScreenPos(topLeft);
        // topLeft.x -= ImGui.getScrollX();
        // topLeft.y -= ImGui.getScrollY();

        float val = 210;

        leftX = windowPos.x + val;
        bottomY = windowPos.y;
        rightX = windowPos.x + windowSize.x + val;
        topY = windowPos.y + windowSize.y;

        int textureId = Window.getFramebuffer().getTexId();
        ImGui.image(textureId, windowSize.x, windowSize.y, 0, 1, 1, 0);

        MouseListener.setGameViewportPos(new Vector2f(windowPos.x + val, windowPos.y));
        MouseListener.setGameViewportSize(new Vector2f(windowSize.x, windowSize.y));

        ImGui.end();
    }

    private ImVec2 getCenteredPositionForViewport(ImVec2 aspectSize) {
        ImVec2 windowSize = new ImVec2();
        ImGui.getContentRegionAvail(windowSize);
        // windowSize.x -= ImGui.getScrollX();
        // windowSize.y -= ImGui.getScrollY();
        float viewportX = (windowSize.x / 2.0f) - (aspectSize.x / 2.0f);
        float viewportY = (windowSize.y / 2.0f) - (aspectSize.y / 2.0f);

        return new ImVec2(viewportX + ImGui.getCursorPosX(), viewportY + ImGui.getCursorPosY());
    }

    private ImVec2 getLargestSizeForViewport() {
        ImVec2 windowSize = new ImVec2();
        ImGui.getContentRegionAvail(windowSize);
        // windowSize.x -= ImGui.getScrollX();
        // windowSize.y -= ImGui.getScrollY();
        float aspectWidth = windowSize.x;
        float aspectHeight = aspectWidth / Window.getTargetAspectRatio();
        if(aspectHeight > windowSize.y) {
            aspectHeight = windowSize.y;
            aspectWidth = aspectHeight * Window.getTargetAspectRatio();
        }

        return new ImVec2(aspectWidth, aspectHeight);
    }

    public boolean getWantCaptureMouse() {
        return MouseListener.getX() >= leftX && MouseListener.getX() <= rightX &&
        MouseListener.getY() >= bottomY && MouseListener.getY() <= topY;
    }
}
