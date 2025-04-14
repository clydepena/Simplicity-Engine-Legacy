package simplicity;

// import org.lwjgl.Version;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.*;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWNativeCocoa.*;
import static org.lwjgl.glfw.GLFWNativeWin32.*;
import static org.lwjgl.glfw.GLFWNativeX11.*;
import static org.lwjgl.opengl.GL11C.*;
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
import observers.*;
import observers.events.*;
import renderer.*;
import scenes.*;
import util.*;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
// import static org.lwjgl.opengl.GL46.*;
import org.joml.*;


public class Window implements Observer {
    private int width, height;
    private int xPos, yPos;
    private String title;
    private String glslVersion = "#version 460";
    private long glfwWindow;
    public static int SCREEN_WIDTH, SCREEN_HEIGHT;
    public static float SCALE = 0.5f;
    private Vector4f bgColor;
    private long audioContext, audioDevice;
    private static Window window;
    private static Scene currentScene;
    private boolean runtimePlaying = false;
    public static long custom_cursor;
    private ImGuiLayer imguiLayer;
    private Framebuffer framebuffer;
    private PickingTexture pickingTexture;
    private long handleWindow;
    private int handleType;

    private Window() {
        // this.width = SCREEN_WIDTH;
        // this.height = SCREEN_HEIGHT;
        this.title = "Legacy Simplicity @D.P.";
        this.bgColor = new Vector4f(0.0f, 0.0f,0.0f,0.0f);
        EventSystem.addObserver(this);
    }

    public static void changeScene(SceneInitializer sceneInitializer) {
        if(currentScene != null) {
            currentScene.destroy();
        }
        getImGuiLayer().getPropertiesWindow().setActiveGameObject(null);
        currentScene = new Scene(sceneInitializer);
        currentScene.load();
        currentScene.init();
        currentScene.start();
    }

    public static Window get() {
        if(Window.window == null) {
            Window.window = new Window();
        }
        return Window.window;
    }

    @SuppressWarnings("static-access")
    public static Scene getScene() {
        return get().currentScene;
    }

    public void run() {
        // System.out.println("LWJGL VERSION: " + Version.getVersion());
        initWindow();
        initImGui();
        Window.changeScene(new LevelEditorSceneInitializer());
        loop();
        destroy();
    }

    private void destroy() {
        // destroy ImGui
        imguiLayer.destroy();
    
        // free memory
        alcDestroyContext(audioContext);
        alcCloseDevice(audioDevice);
        glfwFreeCallbacks(glfwWindow);
        glfwDestroyWindow(glfwWindow);
        
        // terminate GLFW and the free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public void initWindow() {
        // setup an error callback
        GLFWErrorCallback.createPrint(System.err).set();

        // initialize GLFW
        if(!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW.");
        }

        // configure GLFW

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);

        //Window hints
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_MAXIMIZED, 0);
        glfwWindowHint(GLFW_DECORATED, GLFW_TRUE);

        GLFWVidMode mode = glfwGetVideoMode(glfwGetPrimaryMonitor());        

        SCREEN_WIDTH = mode.width();
        SCREEN_HEIGHT = mode.height();
        width = SCREEN_WIDTH;
        height = SCREEN_HEIGHT;
        int tmpWidth = (int) (this.width * 0.75);
        int tmpHeight = (int) (this.height * 0.75);


        // create window
        glfwWindow = glfwCreateWindow(tmpWidth, tmpHeight, this.title, NULL, NULL);
        if(glfwWindow == NULL) {
            throw new IllegalStateException("Failed to create new GLFW window.");
        }

        // System.out.println("Monitor size: " + mode.width() + " | " + mode.height() + " [" + tmpWidth + " | " + tmpHeight + "]");

        // set icon
        setIcon(Resources.ICON, Resources.ICON_SMALL);
        //set screensize to monitor
        glfwSetWindowSizeLimits(glfwWindow, (int) (SCREEN_WIDTH * 0.75f), (int) (SCREEN_HEIGHT * 0.75f), GLFW_DONT_CARE, GLFW_DONT_CARE);
        // glfwSetWindowSizeLimits(glfwWindow, width, height, width, height);

        setWindowPos((SCREEN_WIDTH - tmpWidth) / 2, ((SCREEN_HEIGHT - tmpHeight) / 2));
        
        // set listeners
        setListeners();

        // Audio
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
        glfwMakeContextCurrent(glfwWindow);
        
        // enable v-sync
        glfwSwapInterval(1);

        GL.createCapabilities();

        // alpha blending
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
 
        this.framebuffer = new Framebuffer(width, height); // TEMP
        this.pickingTexture = new PickingTexture(width, height); // TEMp
        glViewport(0, 0, 1920, 1080); // TEMP
        glfwShowWindow(glfwWindow);
        glfwMaximizeWindow(glfwWindow);

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

    public void setWindowPos(int x, int y) {
        xPos = x;
        yPos = y;
        glfwSetWindowPos(glfwWindow, x, y);
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
        glfwSetCursorPosCallback(glfwWindow, MouseListener::mousePosCallback);
        glfwSetMouseButtonCallback(glfwWindow, MouseListener::mouseButtonCallback);
        glfwSetScrollCallback(glfwWindow, MouseListener::mouseScrollCallback);
        glfwSetKeyCallback(glfwWindow, (w, key, scancode, action, mods) -> {
            KeyListener.keyCallback(w, key, scancode, action, mods, this);
        });

        
        glfwSetWindowSizeCallback(glfwWindow, (w, newWidth, newHeight) -> {
            get().width = newWidth;
            get().height = newHeight;

            // System.out.println("Window size: " + width + " | " + height);
        });
        glfwSetWindowPosCallback(glfwWindow, (w, newXPos, newYPos) -> {
            get().xPos = newXPos;
            get().yPos = newYPos;

            // System.out.println("Window pos: " + xPos + " | " + yPos);
        });
    }

    public void initImGui() {
        this.imguiLayer = new ImGuiLayer(glfwWindow, pickingTexture);
        this.imguiLayer.initImGui();
    }

    public void loop() {
        float beginTime = (float) glfwGetTime();
        float endTime;
        float dt = -1.0f;

        Shader defaultShader = AssetPool.getShaderFromRes(Resources.MAIN_SHADER);
        Shader pickingShader = AssetPool.getShaderFromRes(Resources.PICKING_SHADER);
        // Shader fontShader = AssetPool.getShader("app/assets/shaders/fontShader.glsl");

        while(!glfwWindowShouldClose(glfwWindow)) {
            // poll events
            glfwPollEvents();

            // Render pass 1. Render to picking texture
            glDisable(GL_BLEND);
            pickingTexture.enableWriting();


            // DEBUGGING
            MouseListener.printCoords();

            // glViewport(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            glViewport(0, 0, 1920, 1080);
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            Renderer.bindShader(pickingShader);
            currentScene.render();
            
            pickingTexture.disableWriting();
            glEnable(GL_BLEND);

            // Render pass 2. Render actual game
            DebugDraw.beginFrame(); // TEMP

            this.framebuffer.bind(); // TEMP

            glClearColor(bgColor.x, bgColor.y, bgColor.z, bgColor.w);
            glClear(GL_COLOR_BUFFER_BIT);

            if(dt >= 0) {
                DebugDraw.draw(); // TEMP
                Renderer.bindShader(defaultShader);
                if(runtimePlaying) {
                    currentScene.update(dt);
                } else {
                    currentScene.editorUpdate(dt);
                }
                currentScene.render();;
                
            }
            this.framebuffer.unbind(); // TEMP

            this.imguiLayer.update(dt, currentScene); // TEMP
            glfwSwapBuffers(glfwWindow);

            MouseListener.endFrame();

            endTime = (float) glfwGetTime();
            dt = endTime - beginTime;
            beginTime = endTime;
        }
    }

    public static void setWindowBgColor(Vector4f color) {
        get().bgColor = color;
    }

    public static String getGlslVersion() {
        return get().glslVersion;
    }

    public static int getWidth() {
        return get().width;
    }

    public static int getHeight() {
        return get().height;
    }

    public static int getXPos() {
        return get().xPos;
    }

    public static int getYPos() {
        return get().yPos;
    }

    public static Framebuffer getFramebuffer() {
        return get().framebuffer;
    }

    public static float getTargetAspectRatio() {
        return 16.0f / 9.0f;
    }

    public static ImGuiLayer getImGuiLayer() {
        return get().imguiLayer;
    }

    @Override
    public void onNotify(Object obj, Event event) {
        
        switch (event.type) {
            case GameEngineStartPlay:
                this.runtimePlaying = true;
                currentScene.save();
                Window.changeScene(new LevelEditorSceneInitializer());
                break;
            case GameEngineStopPlay:
                this.runtimePlaying = false;
                Window.changeScene(new LevelEditorSceneInitializer());
                break;
            case LoadLevel:
                Window.changeScene(new LevelEditorSceneInitializer(util.IOHelper.openSingle(window,"json")));
                break;
            case SaveLevel:
                currentScene.save();
                break;
            case SaveLevelAs:
                String path = util.IOHelper.saveFile(window, "level", "json");
                currentScene.saveAs(path);
                Window.changeScene(new LevelEditorSceneInitializer(path));
                break;
            default:
                break;
        }
    }

    public long getHandleWin() {
        return this.handleWindow;
    }

    public int getHandleType() {
        return this.handleType;
    }
}