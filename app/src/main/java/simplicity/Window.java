package simplicity;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import observers.EventSystem;
import observers.Observer;
import observers.events.Event;
import observers.events.EventType;
import renderer.*;
import scenes.LevelEditorSceneInitializer;
// import scenes.LevelScene;
import scenes.Scene;
import scenes.SceneInitializer;
import util.AssetPool;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.joml.Vector4f;

public class Window implements Observer {
    private int width, height;
    private String title;
    private String glslVersion = null;
    private long glfwWindow;
    public static final int SCREEN_WIDTH = 1920, SCREEN_HEIGHT = 1080;
    public static float SCALE = 0.5f;
    private Vector4f bgColor;

    private static Window window;
    private static Scene currentScene;
    private boolean runtimePlaying = false;

    private ImGuiLayer imguiLayer;
    private Framebuffer framebuffer;
    private PickingTexture pickingTexture;

    private Window() {
        this.width = SCREEN_WIDTH;
        this.height = SCREEN_HEIGHT;
        this.title = "Simplicity Engine @clydeskye_";
        this.bgColor = new Vector4f(0.0f, 0.0f,0.0f,0.0f);
        EventSystem.addObserver(this);
    }

    public static void changeScene(SceneInitializer sceneInitializer) {
        if(currentScene != null) {
            currentScene.destroy();
        }
        getImGuiLayer().getPropertiesWindow().setActiveGameObject(null);
        currentScene = new Scene(sceneInitializer);
        currentScene.laod();
        currentScene.init();
        currentScene.start();
    }

    public static Window get() {
        if(Window.window == null) {
            Window.window = new Window();
        }
        return Window.window;
    }

    public static Scene getScene() {
        return get().currentScene;
    }

    public void run() {
        System.out.println("LWJGL VERSION: " + Version.getVersion());
        
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

        // comfigure GLFW

        this.glslVersion = "#version 460";
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);

        // create window
        glfwWindow = glfwCreateWindow(this.width, this.height, this.title, NULL, NULL);
        if(glfwWindow == NULL) {
            throw new IllegalStateException("Failed to create new GLFW window.");
        }

        // set listeners
        glfwSetCursorPosCallback(glfwWindow, MouseListener::mousePosCallback);
        glfwSetMouseButtonCallback(glfwWindow, MouseListener::mouseButtonCallback);
        glfwSetScrollCallback(glfwWindow, MouseListener::mouseScrollCallback);
        glfwSetKeyCallback(glfwWindow, KeyListener::keyCallback);
        glfwSetWindowSizeCallback(glfwWindow, (w, newWidth, newHeight) -> {
            Window.setWidth(newWidth);
            Window.setHeight(newHeight);
        });

        // take the OpenGL context current
        glfwMakeContextCurrent(glfwWindow);
        
        // enable v-sync
        glfwSwapInterval(1);

        // make window visible
        glfwShowWindow(glfwWindow);

        GL.createCapabilities();

        // alpha blending
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        this.framebuffer = new Framebuffer(SCREEN_WIDTH, SCREEN_HEIGHT); // TEMP
        this.pickingTexture = new PickingTexture(SCREEN_WIDTH, SCREEN_HEIGHT); // TEMp
        glViewport(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT); // TEMP

    }

    public void initImGui() {
        this.imguiLayer = new ImGuiLayer(glfwWindow, pickingTexture);
        this.imguiLayer.initImGui();
    }

    public void loop() {
        float beginTime = (float) glfwGetTime();
        float endTime;
        float dt = -1.0f;

        Shader defaultShader = AssetPool.getShader("app/assets/shaders/default.glsl");
        Shader pickingShader = AssetPool.getShader("app/assets/shaders/pickingShader.glsl");
        // Shader fontShader = AssetPool.getShader("app/assets/shaders/fontShader.glsl");

        while(!glfwWindowShouldClose(glfwWindow)) {
            // poll events
            glfwPollEvents();

            // Render pass 1. Render to picking texture
            glDisable(GL_BLEND);
            pickingTexture.enableWriting();

            glViewport(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
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

    public static void setWidth(int newWidth) {
        get().width = newWidth;
    }

    public static void setHeight(int newHeight) {
        get().height = newHeight;
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
    public void onNotify(GameObject obj, Event event) {
        
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
                Window.changeScene(new LevelEditorSceneInitializer());
            case SaveLevel:
                currentScene.save();
        }
    }
}