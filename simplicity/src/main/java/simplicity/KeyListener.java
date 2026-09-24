package simplicity;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import observers.EventSystem;
import observers.events.Event;
import static observers.events.EventType.*;

public class KeyListener {
    private static KeyListener instance;
    private boolean keyPressed[] = new boolean[350];
    // private boolean keyBeginPress[] = new boolean[350];

    public static class KeyEvent extends Event {
        public final int key;
        public final int scancode;
        public final int action;
        public final int mods;

        public KeyEvent(int key, int scancode, int action, int mods) {
            super(KeyInput,null);
            this.key = key;
            this.scancode = scancode;
            this.action = action;
            this.mods = mods;
        }
    }

     public static class CharEvent extends Event {
        public final char charr;

        public CharEvent(char charr) {
            super(CharInput,null);
            this.charr = charr;
        }
    }

    private KeyListener() {}

    public static KeyListener get() {
        if(KeyListener.instance == null) {
            KeyListener.instance = new KeyListener();
        }
        return KeyListener.instance;
    }

    public static void keyCallback(long window, int key, int scancode, int action, int mods) {
        keyCallback(window, key, scancode, action, mods, "");
    }

    public static void keyCallback(long window, int key, int scancode, int action, int mods, Object location) {
        if(action == GLFW_PRESS) {
            get().keyPressed[key] = true;
            // get().keyBeginPress[key] = true;
        } else if (action == GLFW_RELEASE) {
            get().keyPressed[key] = false;
            // get().keyBeginPress[key] = false;
        }
        EventSystem.publish(new KeyEvent(key, scancode, action, mods));
        // System.out.println("KEY: " + key + "\t| MOD: " + mods + "\t| LOC: " + location.getClass().getName());
    }

    public static void charCallback(long window, int charr) {
        EventSystem.publish(new CharEvent((char) charr));
    }

    public static boolean isKeyPressed(int keyCode) {
        return get().keyPressed[keyCode];
    }

    // public static boolean keyBeginPress(int keyCode) {
    //     boolean result = get().keyBeginPress[keyCode];
    //     if (result) {
    //         get().keyBeginPress[keyCode] = false;
    //     }
    //     return result;
    // }
}
