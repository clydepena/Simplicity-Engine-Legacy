package simplicity;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import scenes.*;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

public class MouseListener {
    private static MouseListener instance;
    private double scrollX, scrollY;
    private double xPos, yPos, lastX, lastY, worldX, worldY, lastWorldX, lastWorldY;
    private boolean mouseButtonPressed[] = new boolean[9];
    private boolean isDragging;
    private int mouseButtonDown = 0;

    private Vector2f gameViewportPos = new Vector2f();
    private Vector2f gameViewportSize = new Vector2f();

    private MouseListener() {
        this.scrollX = 0.0;
        this.scrollY = 0.0;
        this.xPos = 0.0;
        this.yPos = 0.0;
        this.lastX = 0.0;
        this.lastY = 0.0;
    }

    public static MouseListener get() {
        if(MouseListener.instance == null) {
            MouseListener.instance = new MouseListener();
        }
        return MouseListener.instance;
    }

    public static void mousePosCallback(long window, double xpos, double ypos) {
        if(get().mouseButtonDown > 0) {
            get().isDragging = true;
        }
        get().lastX = get().xPos;
        get().lastY = get().yPos;
        get().lastWorldX = get().worldX;
        get().lastWorldY = get().worldY;
        get().xPos = xpos;
        get().yPos = ypos;
        calcOrthoX();
        calcOrthoY();
        // get().isDragging = get().mouseButtonPressed[0] || get().mouseButtonPressed[1] || get().mouseButtonPressed[2];
    }

    public static void mouseButtonCallback(long window, int button, int action, int mods) {
        if(action == GLFW_PRESS){
            get().mouseButtonDown++;
            if(button < get().mouseButtonPressed.length) {
                get().mouseButtonPressed[button] = true;
            }
        } else if (action == GLFW_RELEASE) {
            get().mouseButtonDown--;
            if(button < get().mouseButtonPressed.length) {
                get().mouseButtonPressed[button] = false;
                get().isDragging = false;
            }

            // DEBUGGING
            System.out.println("Screen:\t" + getScreenX() + "\t| " + getScreenY());
            System.out.println("World:\t" + getWorldX() + "\t| " + getWorldY());
            System.out.println("Coords:\t" + getX() + "\t| " + getY());
            System.out.println("-");



        }
    }

    public static void mouseScrollCallback(long window, double xOffset, double yOffset) {
        get().scrollX = xOffset;
        get().scrollY = yOffset;
    }

    public static void endFrame() {
        get().scrollX = 0;
        get().scrollY = 0;
        get(). lastX = get().xPos;
        get().lastY = get().yPos;
        get(). lastWorldX = get().worldX;
        get().lastWorldY = get().worldY;
    }

    public static float getX() {
        return (float) get().xPos;
    }

    public static float getY() {
        return (float) get().yPos;
    }

    public static float getScreenX() {
        float currentX = getX() - get().gameViewportPos.x;
        // currentX = (currentX / get().gameViewportSize.x) * (float) Window.SCREEN_WIDTH;
        currentX = (currentX / get().gameViewportSize.x) * (float) Window.getWidth();
        
        return currentX;
    }

    public static float getScreenY() {
        float currentY = getY() - get().gameViewportPos.y;
        // currentY = ((float) Window.SCREEN_HEIGHT) - ((currentY / get().gameViewportSize.y) * ((float) Window.SCREEN_HEIGHT));
        currentY = ((float) Window.getHeight()) - ((currentY / get().gameViewportSize.y) * ((float) Window.getHeight()));

        return currentY;
    }

    public static float getOrthoX() {
        return (float) get().worldX;
    }

    public static float getOrthoY() {
        return (float) get().worldY;
    }

    private static void calcOrthoX () {
        float currentX = getX() - get().gameViewportPos.x;
        currentX = (currentX / get().gameViewportSize.x) * 2.0f - 1.0f;
        Vector4f temp = new Vector4f(currentX, 0, 0, 1);

        Camera camera = Window.getScene().camera();
        Matrix4f viewProjection = new Matrix4f();
        camera.getInverseView().mul(camera.getInverseProjection(), viewProjection);
        temp.mul(viewProjection);

        get().worldX = temp.x;
    }

    private static void calcOrthoY() {
        float currentY = getY() - get().gameViewportPos.y;
        currentY = -((currentY / get().gameViewportSize.y) * 2.0f - 1.0f);
        Vector4f temp = new Vector4f(0, currentY, 0, 1);
        
        Camera camera = Window.getScene().camera();
        Matrix4f viewProjection = new Matrix4f();
        camera.getInverseView().mul(camera.getInverseProjection(), viewProjection);
        temp.mul(viewProjection);

        get().worldY = temp.y;
    }

    public static Vector2f getWorld() {
        float currentX = getX() - get().gameViewportPos.x;
        currentX = (2.0f * (currentX / get().gameViewportSize.x)) - 1.0f;


        float currentY = getY() - get().gameViewportPos.y;
        currentY = (2.0f * (1.0f - (currentY / get().gameViewportSize.y))) - 1;

        Vector4f temp = new Vector4f(currentX, currentY, 0, 1);
        
        Camera camera = Window.getScene().camera();
        Matrix4f inverseView = new Matrix4f(camera.getInverseView());
        Matrix4f inverseProjection = new Matrix4f(camera.getInverseProjection());
        temp.mul(inverseView.mul(inverseProjection));

        // get().worldX = temp.x;
        // get().worldY = temp.y;

        return new Vector2f(temp.x, temp.y);
    }

    public static float getWorldX() {
        return getWorld().x;
    }

    public static float getWorldY() {
        return getWorld().y;
    }

    public static float getDx() {
        return (float) (get().lastX - get().xPos);
    }

    public static float getDy() {
        return (float) (get().lastY - get().yPos);
    }

    public static float getWorldDx() {
        return (float) (get().lastWorldX - get().worldX);
    }

    public static float getWorldDy() {
        return (float) (get().lastWorldY - get().worldY);
    }

    public static float getScrollX() {
        return (float) get().scrollX;
    }

    public static float getScrollY() {
        return (float) get().scrollY;
    }

    public static boolean isDragging() {
        return get().isDragging;
    }

    public static boolean mouseButtonDown(int button) {
        if(button < get().mouseButtonPressed.length) {
            return get().mouseButtonPressed[button];
        } else {
            return false;
        }
    }

    public static void setGameViewportPos(Vector2f gameViewportPos) {
        get().gameViewportPos.set(gameViewportPos);
    }

    public static void setGameViewportSize(Vector2f gameViewportSize) {
        get().gameViewportSize.set(gameViewportSize);
    }

    

}
