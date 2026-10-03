package simplicity;

// import org.lwjgl.Version;
import org.lwjgl.glfw.*;
import org.lwjgl.system.*;


import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWNativeCocoa.*;
import static org.lwjgl.glfw.GLFWNativeWin32.*;
import static org.lwjgl.glfw.GLFWNativeX11.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.nfd.NativeFileDialog.*;

import org.lwjgl.openal.*;
import static org.lwjgl.openal.ALC10.ALC_DEFAULT_DEVICE_SPECIFIER;
import static org.lwjgl.openal.ALC10.alcCloseDevice;
import static org.lwjgl.openal.ALC10.alcCreateContext;
import static org.lwjgl.openal.ALC10.alcDestroyContext;
import static org.lwjgl.openal.ALC10.alcGetString;
import static org.lwjgl.openal.ALC10.alcMakeContextCurrent;
import static org.lwjgl.openal.ALC10.alcOpenDevice;
import static org.lwjgl.openal.ALC11.*;
import static simplicity.MouseListener.MouseDroppedPathEvent;

import observers.*;
import observers.events.*;
import static observers.events.EventType.*;
import renderer.*;
import simplicity.MouseListener.MouseDroppedPathEvent;
import util.*;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;

// import scenes.*;
// import org.joml.*;
// import editor.ImGuiLayer;

public class Window {

    public static class WindowCloseEvent extends Event {
        public final Window window;

        private boolean cancelled = false;

        public WindowCloseEvent(Window window) {
            super(WindowClose);
            this.window = window;
        }

        /** Keeps the window open, e.g. to ask about unsaved changes first; whoever cancels closes it later. */
        public void cancel() {
            cancelled = true;
        }

        @Override
        public void onEnd() {
            if (cancelled) return;
            window.setVisible(false);
            window.confirmClose();
        }
    }

    public static class FramebufferResizeEvent extends Event {
        public final int width, height;

        public FramebufferResizeEvent(int width, int height) {
            super(FramebufferResize);
            this.width = width;
            this.height = height;
        }
    }

    private int width, height, fbWidth, fbHeight;
    private int xPos, yPos;
    private String title;
    private long glfwWindow;
    public static int SCREEN_WIDTH, SCREEN_HEIGHT;
    public static float SCALE = 0.5f;
    private long audioContext, audioDevice;
    private static Window window;
    public static long custom_cursor;
    private long handleWindow;
    private int handleType;
    private boolean running = true;
    private boolean isMinimized = false;

    // private static Scene currentScene;
    // private ImGuiLayer imguiLayer;
    // private Framebuffer framebuffer;

    private Window(String title) {
        // this.width = SCREEN_WIDTH;
        // this.height = SCREEN_HEIGHT;
        this.title = title;
        // EventSystem.addObserver(this);
    }

    public static Window create(String title) {
        if(Window.window == null) {
            Window.window = new Window(title);
        }
        return Window.window;
    }
    
    public static Window get() {
        return Window.window;
    }

    public void destroy() {
        // destroy ImGui
        // imguiLayer.destroy();
    
        // free memory
        alcDestroyContext(audioContext);
        alcCloseDevice(audioDevice);
        glfwFreeCallbacks(glfwWindow);
        glfwDestroyWindow(glfwWindow);
    }

    public void init() {
        GLFWVidMode mode = glfwGetVideoMode(glfwGetPrimaryMonitor());        

        SCREEN_WIDTH = mode.width();
        SCREEN_HEIGHT = mode.height();
        width = SCREEN_WIDTH;
        height = SCREEN_HEIGHT;
        int tmpWidth = (int) (this.width * 0.75);
        int tmpHeight = (int) (this.height * 0.75);
        width = tmpWidth;
        height = tmpHeight;

        // create window
        glfwWindow = glfwCreateWindow(tmpWidth, tmpHeight, this.title, NULL, NULL);
        if(glfwWindow == NULL) {
            throw new IllegalStateException("Failed to create new GLFW window.");
        }


        setIcon(Resources.ICON, Resources.ICON_SMALL);
        restoreDefaultWindowSizeLimits();
        setWindowPos((SCREEN_WIDTH - tmpWidth) / 2, ((SCREEN_HEIGHT - tmpHeight) / 2));
        
        int[] fw = new int[1], fh = new int[1];
        glfwGetFramebufferSize(glfwWindow, fw, fh);
        fbWidth = fw[0]; fbHeight = fh[0];

        setListeners();

        String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
        audioDevice = alcOpenDevice(defaultDeviceName);

        int[] attributes = {0};
        audioContext = alcCreateContext(audioDevice, attributes);
        alcMakeContextCurrent(audioContext);

        ALCCapabilities alcCapabilities = ALC.createCapabilities(audioDevice);
        ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);

        if (!alCapabilities.OpenAL11) {
            assert false : "Audio library not supported.";
        }

        switch (Platform.get()) {
            case FREEBSD:
            case LINUX:
                handleType = NFD_WINDOW_HANDLE_TYPE_X11;
                handleWindow = glfwGetX11Window(glfwWindow);
                break;
            case MACOSX:
                handleType = NFD_WINDOW_HANDLE_TYPE_COCOA;
                handleWindow = glfwGetCocoaWindow(glfwWindow);
                break;
            case WINDOWS:
                handleType = NFD_WINDOW_HANDLE_TYPE_WINDOWS;
                handleWindow = glfwGetWin32Window(glfwWindow);
                break;
            default:
                handleType = NFD_WINDOW_HANDLE_TYPE_UNSET;
                handleWindow = NULL;
        }
    }

    public void setCursorImg(String filepath) {
        GLFWImage cursorImg = GLFWImage.malloc(); 
        IOHelper.LoadedByteImg cursor = IOHelper.GenResImg(filepath);
        cursorImg.set(cursor.getWidth(), cursor.getHeight(), cursor.getImg());
        
        long cursorAddress = glfwCreateCursor(cursorImg, 0, 0);
        if (cursorAddress == MemoryUtil.NULL) 
            throw new RuntimeException("Error creating cursor");
 
        glfwSetCursor(glfwWindow, cursorAddress);
    }

    private void setIcon(String filepath, String filepath2) {
        try {
            GLFWImage.Buffer imagebf = GLFWImage.malloc(2);
            GLFWImage iconImg = GLFWImage.malloc(); 
            IOHelper.LoadedByteImg icon = IOHelper.GenResImg(filepath);
            iconImg.set(icon.getWidth(), icon.getHeight(), icon.getImg());
            imagebf.put(0, iconImg);

            GLFWImage iconImg2 = GLFWImage.malloc(); 
            IOHelper.LoadedByteImg icon2 = IOHelper.GenResImg(filepath2);
            iconImg2.set(icon2.getWidth(), icon2.getHeight(), icon2.getImg());
            imagebf.put(1, iconImg2);

            glfwSetWindowIcon(glfwWindow, imagebf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setListeners() {
        glfwSetCursorPosCallback(glfwWindow, (w, x, y) -> {
            MouseListener.mousePosCallback(this, (int) x, (int) y);
        });
        glfwSetMouseButtonCallback(glfwWindow, MouseListener::mouseButtonCallback);
        glfwSetScrollCallback(glfwWindow, MouseListener::mouseScrollCallback);
        glfwSetKeyCallback(glfwWindow, KeyListener::keyCallback);
        glfwSetCharCallback(glfwWindow, KeyListener::charCallback);

        glfwSetWindowSizeCallback(glfwWindow, (w, newWidth, newHeight) -> {
            get().width = newWidth;
            get().height = newHeight;
        });
        glfwSetWindowPosCallback(glfwWindow, (w, newXPos, newYPos) -> {
            get().xPos = newXPos;
            get().yPos = newYPos;
        });

        glfwSetFramebufferSizeCallback(glfwWindow, (w, fw, fh) -> {
            fbWidth = fw;
            fbHeight = fh;
            EventSystem.publishCoalescing(new FramebufferResizeEvent(fw, fh));
        });

        glfwSetWindowIconifyCallback(glfwWindow, (w, iconified) -> {
            isMinimized = iconified;
        });

        glfwSetDropCallback(glfwWindow, MouseListener::mouseDroppedPathCallback);

        glfwSetWindowCloseCallback(glfwWindow, (w) -> EventSystem.publish(new WindowCloseEvent(this)));
    }

    public void setVisible(boolean bool) {
        if (bool) {
            glfwShowWindow(glfwWindow);
        } else {
            glfwHideWindow(glfwWindow);
        }
    }

    public void setWindowPos(int x, int y) {
        xPos = x;
        yPos = y;
        glfwSetWindowPos(glfwWindow, x, y);
    }

    public long getHandleWin() {
        return this.handleWindow;
    }

    public int getHandleType() {
        return this.handleType;
    }

    public int getWidth() {
        return get().width;
    }

    public int getHeight() {
        return get().height;
    }

    public int getFramebufferWidth()  { 
        return get().fbWidth;
    }
    public int getFramebufferHeight() {
        return get().fbHeight; 
    }

    public int getXPos() {
        return get().xPos;
    }

    public int getYPos() {
        return get().yPos;
    }

    // public static Framebuffer getFramebuffer() {
    //     return get().framebuffer;
    // }

    public static float getTargetAspectRatio() {
        return 16.0f / 9.0f;
    }

    public long ptr() {
        return get().glfwWindow;
    }

    // public void swapBuffers() {
    //     glfwSwapBuffers(glfwWindow);
    // }
    
    public void pollEvents() {
        glfwPollEvents();
    }

    public void awaitEvents() {
        glfwWaitEvents();
    }
    
    public void confirmClose() {
        get().running = false;
    }
    
    public boolean shouldClose() {
        return !get().running;
    }

    public boolean isMinimized() {
        return get().isMinimized;
    }

    public void focusWindow() {
        glfwFocusWindow(glfwWindow);
    }

    public void maximize() {
        glfwMaximizeWindow(glfwWindow);
    }

    public void restore() {
        glfwRestoreWindow(glfwWindow);
    }

    public void setTitle(String title) {
        this.title = title;
        glfwSetWindowTitle(glfwWindow, title);
    }

    public String getTitle() {
        return title;
    }

    public void allowResize(boolean bool) {
        glfwSetWindowAttrib(glfwWindow, GLFW_RESIZABLE, bool ? GLFW_TRUE : GLFW_FALSE);
    }

    public void setWindowSizeLimits(int minWidth, int minHeight, int maxWidth, int maxHeight) {
        glfwSetWindowSizeLimits(glfwWindow, minWidth, minHeight, maxWidth, maxHeight);
    }

    public void setWindowSize(int width, int height) {
        glfwSetWindowSize(glfwWindow, width, height);
    }

    public void restoreDefaultWindowSizeLimits() {
        glfwSetWindowSizeLimits(glfwWindow, (int) (SCREEN_WIDTH * 0.75f), (int) (SCREEN_HEIGHT * 0.75f), GLFW_DONT_CARE, GLFW_DONT_CARE);
    }

    public void centerToMonitor() {
        setWindowPos((SCREEN_WIDTH - width) / 2, ((SCREEN_HEIGHT - height) / 2));
    }
}