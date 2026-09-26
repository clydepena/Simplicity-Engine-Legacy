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

        public WindowCloseEvent(Window window) {
            super(WindowClose);
            this.window = window;
        }

        @Override
        public void onEnd() {
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
        glfwSetWindowSizeLimits(glfwWindow, (int) (SCREEN_WIDTH * 0.75f), (int) (SCREEN_HEIGHT * 0.75f), GLFW_DONT_CARE, GLFW_DONT_CARE);
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

        // take the OpenGL context current
        // glfwMakeContextCurrent(glfwWindow);
        
        // enable v-sync
        // glfwSwapInterval(1);

        // GL.createCapabilities();

        // alpha blending
        // glEnable(GL_BLEND);
        // glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
 
        // this.framebuffer = new Framebuffer(width, height); // TEMP
        // this.pickingTexture = new PickingTexture(width, height); // TEMp
        // glViewport(0, 0, 1920, 1080); // TEMP
        // glfwShowWindow(glfwWindow);
        // glfwMaximizeWindow(glfwWindow);

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
    
    // old code -> never call this
    private void loop() {
        float beginTime = (float) glfwGetTime();
        float endTime;
        float dt = -1.0f;

        // Shader defaultShader = AssetPool.getShaderFromRes(Resources.MAIN_SHADER);
        // Shader pickingShader = AssetPool.getShaderFromRes(Resources.PICKING_SHADER);
        // Shader fontShader = AssetPool.getShader("app/assets/shaders/fontShader.glsl");

        while(!glfwWindowShouldClose(glfwWindow)) {
            // poll events
            // glfwPollEvents();

            // Render pass 1. Render to picking texture
            // glDisable(GL_BLEND);
            // pickingTexture.enableWriting();


            // DEBUGGING
            // MouseListener.printCoords();

            // glViewport(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            // glViewport(0, 0, 1920, 1080);
            // glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            // glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // RendererOld.bindShader(pickingShader);
            // currentScene.render();
            
            // pickingTexture.disableWriting();
            // glEnable(GL_BLEND);

            // Render pass 2. Render actual game
            // DebugDraw.beginFrame(); // TEMP

            // this.framebuffer.bind(); // TEMP

            // glClearColor(bgColor.x, bgColor.y, bgColor.z, bgColor.w);
            // glClear(GL_COLOR_BUFFER_BIT);

            if(dt >= 0) {
                // DebugDraw.draw(); // TEMP
                // RendererOld.bindShader(defaultShader);
                // if(runtimePlaying) {
                //     currentScene.update(dt);
                // } else {
                //     currentScene.editorUpdate(dt);
                // }
                // currentScene.render();
                
            }
            // this.framebuffer.unbind(); // TEMP

            // this.imguiLayer.update(dt, currentScene); // TEMP
            // glfwSwapBuffers(glfwWindow);

            // MouseListener.endFrame();

            endTime = (float) glfwGetTime();
            dt = endTime - beginTime;
            beginTime = endTime;
        }
    }

        // public static void changeScene(SceneInitializer sceneInitializer) {
    //     if(currentScene != null) {
    //         currentScene.destroy();
    //     }
    //     // getImGuiLayer().getPropertiesWindow().setActiveGameObject(null);
    //     // currentScene = new Scene(sceneInitializer);
    //     // currentScene.load();
    //     // currentScene.init();
    //     // currentScene.start();
    // }

    // @SuppressWarnings("static-access")
    // public static Scene getScene() {
    //     return get().currentScene;
    // }

    // public void run() {
    //     // System.out.println("LWJGL VERSION: " + Version.getVersion());
    //     initWindow();
    //     // initImGui();
    //     RefactoredWindowTemp.changeScene(new LevelEditorSceneInitializer());
    //     // loop();
    //     destroy();
    // }

    // public void initImGui() {
    //     this.imguiLayer = new ImGuiLayer(glfwWindow, pickingTexture);
    //     this.imguiLayer.initImGui();
    // }

    // public static void setWindowBgColor(Vector4f color) {
    //     get().bgColor = color;
    // }

    // public static String getGlslVersion() {
    //     return get().glslVersion;
    // }


    // public static ImGuiLayer getImGuiLayer() {
    //     return get().imguiLayer;
    // }

    // @Override
    // public void onNotify(Event event) {
        
    //     switch (event.type) {
    //         case GameEngineStartPlay:
    //             this.runtimePlaying = true;
    //             currentScene.save();
    //             RefactoredWindowTemp.changeScene(new LevelEditorSceneInitializer());
    //             break;
    //         case GameEngineStopPlay:
    //             this.runtimePlaying = false;
    //             RefactoredWindowTemp.changeScene(new LevelEditorSceneInitializer());
    //             break;
    //         case LoadLevel:
    //             RefactoredWindowTemp.changeScene(new LevelEditorSceneInitializer(util.IOHelper.openSingle(window,"json")));
    //             break;
    //         case SaveLevel:
    //             currentScene.save();
    //             break;
    //         case SaveLevelAs:
    //             String path = util.IOHelper.saveFile(window, "level", "json");
    //             currentScene.saveAs(path);
    //             RefactoredWindowTemp.changeScene(new LevelEditorSceneInitializer(path));
    //             break;
    //         default:
    //             break;
    //     }
    // }
}