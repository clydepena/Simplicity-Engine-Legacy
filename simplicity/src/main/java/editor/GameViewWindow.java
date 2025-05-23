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

public class GameViewWindow extends ImGuiInterface {
    
    private float leftX, rightX, topY, bottomY;
    private boolean  isPlaying = false;
    private boolean tempFocused = true;
    
    public void imgui(float dt) {
        if (tempFocused) {
            ImGui.setNextWindowFocus();
            tempFocused = false;
        }
        ImGui.begin("Viewport", ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse | ImGuiWindowFlags.MenuBar);
        updateCalc();

        ImGui.beginMenuBar();
        if(ImGui.menuItem("Play", "", isPlaying, !isPlaying)) {
            isPlaying = true;
            EventSystem.notify(new Event(EventType.GameEngineStartPlay));
        }
        if(ImGui.menuItem("Stop", "", !isPlaying, isPlaying)) {
            isPlaying = false;
            EventSystem.notify(new Event(EventType.GameEngineStopPlay));
        }
        ImGui.endMenuBar();

        ImGui.setCursorPos(ImGui.getCursorPosX(), ImGui.getCursorPosY());
        ImVec2 windowSize = getLargestSizeForViewport();
        ImVec2 windowPos = getCenteredPositionForViewport(windowSize);
        ImGui.setCursorPos(windowPos.x, windowPos.y);
        ImVec2 relativePos = new ImVec2(winPosition.x - Window.getXPos(), winPosition.y - Window.getYPos());

        leftX = windowPos.x + relativePos.x;
        bottomY = windowPos.y + windowSize.y + relativePos.y;
        rightX = windowPos.x + windowSize.x + relativePos.x;
        topY = windowPos.y + relativePos.y;

        int textureId = Window.getFramebuffer().getTexId();
        ImGui.image(textureId, windowSize.x, windowSize.y, 0, 1, 1, 0);

        MouseListener.setGameViewportPos(new Vector2f(windowPos.x + relativePos.x, windowPos.y + relativePos.y));
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
        MouseListener.getY() <= bottomY && MouseListener.getY() >= topY && isDocked && isHovered;
    }

    public void makeFocused() {
        tempFocused = true;
    }

    @Override
    public void destroy() {
        
    }
}
