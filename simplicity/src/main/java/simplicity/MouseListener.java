package simplicity;

import static org.lwjgl.glfw.GLFW.*;
import org.joml.*;
import org.lwjgl.glfw.GLFWDropCallback;

import observers.EventSystem;
import observers.events.Event;
import static observers.events.EventType.*;

public class MouseListener {

    public static class MouseMovedEvent extends Event {
        public final int x, y, xScreen, yScreen;
        public final boolean isDragging;

        public MouseMovedEvent(int x, int y, int xScreen, int yScreen, boolean isDragging) {
            super(MouseMoved);
            this.x = x;
            this.y = y;
            this.xScreen = xScreen;
            this.yScreen = yScreen;
            this.isDragging = isDragging;
        }
    }

    public static class MouseButtonEvent extends Event {
        public final int x, y, xScreen, yScreen, button, action, mods;

        public MouseButtonEvent(int x, int y, int xScreen, int yScreen, int button, int action, int mods) {
            super(MouseButton);
            this.x = x;
            this.y = y;
            this.xScreen = xScreen;
            this.yScreen = yScreen;
            this.button = button;
            this.action = action;
            this.mods = mods;
        }
    }

    public static class MouseScrollEvent extends Event {
        public final double scrollX, scrollY;

        public MouseScrollEvent(double scrollX, double scrollY) {
            super(MouseScroll);
            this.scrollX = scrollX;
            this.scrollY = scrollY;
        }

    }

    public static class MouseDroppedPathEvent extends Event {
        public final String[] paths;
        public final int x, y;

        public MouseDroppedPathEvent(int x, int y, String[] paths) {
            super(MouseDroppedPath);
            this.x = x;
            this.y = y;
            this.paths = paths;
        }

    }

    private static MouseListener instance;
    private boolean mouseButtonPressed[] = new boolean[9];
    private boolean isDragging;
    private int mouseButtonDown = 0;
    private int x, y, xScreen, yScreen;
    private double scrollX, scrollY;

    private MouseListener() {
        this.scrollX = 0;
        this.scrollY = 0;
        this.x = 0;
        this.y = 0;
    }

    public static MouseListener get() {
        if(MouseListener.instance == null) {
            MouseListener.instance = new MouseListener();
        }
        return MouseListener.instance;
    }

    public static void mousePosCallback(Window window, int xpos, int ypos) {
        if(get().mouseButtonDown > 0) {
            get().isDragging = true;
        }
    
        int xScreen = window.getXPos() + xpos;
        int yScreen = window.getYPos() + ypos;
        get().xScreen = xScreen;
        get().yScreen = yScreen;
        get().x = xpos;
        get().y = ypos;
        EventSystem.publishCoalescing(new MouseMovedEvent(xpos, ypos, xScreen, yScreen, get().isDragging));
    }

    public static void mouseButtonCallback(long window, int button, int action, int mods) {
        if(action == GLFW_PRESS){
            get().mouseButtonDown++;
            get().mouseButtonPressed[button] = true;
        } else if (action == GLFW_RELEASE) {
            get().mouseButtonDown = 0;
            get().mouseButtonPressed[button] = false;
            get().isDragging = false;
        }
        EventSystem.publish(new MouseButtonEvent(get().x, get().y, get().xScreen, get().yScreen, button, action, mods));
    }
    
    // DEBUGGING
    // public static void printCoords() {
    //     if (KeyListener.isKeyPressed(GLFW_KEY_SPACE)) {
    //         System.out.println("Screen:\t" + getScreenX() + "\t| " + getScreenY());
    //         System.out.println("World:\t" + getWorldX() + "\t| " + getWorldY());
    //         System.out.println("Coords:\t" + getX() + "\t| " + getY());
    //         System.out.println("-");
    //     }
    // }

    public static void mouseScrollCallback(long window, double xOffset, double yOffset) {
        get().scrollX = xOffset;
        get().scrollY = yOffset;
        EventSystem.publishCoalescing(new MouseScrollEvent(xOffset, yOffset));
    }

    public static void mouseDroppedPathCallback(long windowPtr, int count, long names) {
        String[] paths = new String[count];
        for (int i = 0; i < count; i++) paths[i] =GLFWDropCallback.getName(names, i);
        EventSystem.publish(new MouseDroppedPathEvent(get().x, get().y, paths));
    }

    // public static void endFrame() {
    //     get().scrollX = 0;
    //     get().scrollY = 0;
    //     get(). lastX = get().xPos;
    //     get().lastY = get().yPos;
    //     get(). lastWorldX = get().worldX;
    //     get().lastWorldY = get().worldY;
    // }

    // public static float getX() {
    //     return (float) get().xPos;
    // }

    // public static float getY() {
    //     return (float) get().yPos;
    // }

    // public static float getScreenX() {
    //     float currentX = getX() - get().gameViewportPos.x;
    //     // currentX = (currentX / get().gameViewportSize.x) * (float) Window.SCREEN_WIDTH;
    //     currentX = (currentX / get().gameViewportSize.x) * (float) OldWindow.getWidth();
        
    //     return currentX;
    // }

    // public static float getScreenY() {
    //     float currentY = getY() - get().gameViewportPos.y;
    //     // currentY = ((float) Window.SCREEN_HEIGHT) - ((currentY / get().gameViewportSize.y) * ((float) Window.SCREEN_HEIGHT));
    //     currentY = ((float) OldWindow.getHeight()) - ((currentY / get().gameViewportSize.y) * ((float) OldWindow.getHeight()));

    //     return currentY;
    // }

    // public static float getOrthoX() {
    //     return (float) get().worldX;
    // }

    // public static float getOrthoY() {
    //     return (float) get().worldY;
    // }

    // private static void calcOrthoX () {
    //     float currentX = getX() - get().gameViewportPos.x;
    //     currentX = (currentX / get().gameViewportSize.x) * 2.0f - 1.0f;
    //     Vector4f temp = new Vector4f(currentX, 0, 0, 1);

    //     Camera camera = OldWindow.getScene().camera();
    //     Matrix4f viewProjection = new Matrix4f();
    //     camera.getInverseView().mul(camera.getInverseProjection(), viewProjection);
    //     temp.mul(viewProjection);

    //     get().worldX = temp.x;
    // }

    // private static void calcOrthoY() {
    //     float currentY = getY() - get().gameViewportPos.y;
    //     currentY = -((currentY / get().gameViewportSize.y) * 2.0f - 1.0f);
    //     Vector4f temp = new Vector4f(0, currentY, 0, 1);
        
    //     Camera camera = OldWindow.getScene().camera();
    //     Matrix4f viewProjection = new Matrix4f();
    //     camera.getInverseView().mul(camera.getInverseProjection(), viewProjection);
    //     temp.mul(viewProjection);

    //     get().worldY = temp.y;
    // }

    // public static Vector2f getWorld() {
    //     float currentX = getX() - get().gameViewportPos.x;
    //     currentX = (2.0f * (currentX / get().gameViewportSize.x)) - 1.0f;


    //     float currentY = getY() - get().gameViewportPos.y;
    //     currentY = (2.0f * (1.0f - (currentY / get().gameViewportSize.y))) - 1;

    //     Vector4f temp = new Vector4f(currentX, currentY, 0, 1);
        
    //     Camera camera = OldWindow.getScene().camera();
    //     Matrix4f inverseView = new Matrix4f(camera.getInverseView());
    //     Matrix4f inverseProjection = new Matrix4f(camera.getInverseProjection());
    //     temp.mul(inverseView.mul(inverseProjection));

    //     // get().worldX = temp.x;
    //     // get().worldY = temp.y;

    //     return new Vector2f(temp.x, temp.y);
    // }

    // public static float getWorldX() {
    //     return getWorld().x;
    // }

    // public static float getWorldY() {
    //     return getWorld().y;
    // }

    // public static float getDx() {
    //     return (float) (get().lastX - get().xPos);
    // }

    // public static float getDy() {
    //     return (float) (get().lastY - get().yPos);
    // }

    // public static float getWorldDx() {
    //     return (float) (get().lastWorldX - get().worldX);
    // }

    // public static float getWorldDy() {
    //     return (float) (get().lastWorldY - get().worldY);
    // }

    // public static float getScrollX() {
    //     return (float) get().scrollX;
    // }

    // public static float getScrollY() {
    //     return (float) get().scrollY;
    // }

    // public static boolean isDragging() {
    //     return get().isDragging;
    // }

    public static boolean mouseButtonDown(int button) {
        return get().mouseButtonPressed[button];
    }

    // public static void setGameViewportPos(Vector2f gameViewportPos) {
    //     get().gameViewportPos.set(gameViewportPos);
    // }

    // public static void setGameViewportSize(Vector2f gameViewportSize) {
    //     get().gameViewportSize.set(gameViewportSize);
    // }

}
