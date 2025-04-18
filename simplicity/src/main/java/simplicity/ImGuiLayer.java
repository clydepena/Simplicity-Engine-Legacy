package simplicity;

import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.GLFW;
import editor.*;
import imgui.*;
import imgui.callback.ImStrConsumer;
import imgui.callback.ImStrSupplier;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import logger.Log;
import logger.Logger;
import observers.EventSystem;
import observers.Observer;
import observers.events.Event;
import renderer.PickingTexture;
import scenes.Scene;
import util.IOHelper;
import util.Resources;
import util.Settings;
import observers.EventSystem;
import observers.Observer;
import observers.events.Event;
import static logger.Log.LogLevel.*;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
// import static org.lwjgl.opengl.GL46.*;

public class ImGuiLayer implements Observer {

    // private final long[] mouseCursors = new long[ImGuiMouseCursor.COUNT];
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private long glfwWindow;
    private static GameObject editorObject;
    private ImGuiStyle style;
    private GameViewWindow gameViewWindow;
    private PropertiesWindow propertiesWindow;
    private LoggerWindow loggerWindow;
    private MenuBar menuBar;
    private SceneHierarchyWindow sceneHierarchyWindow;
    private TextEditorWindow textEditorWindow;
    private SpriteSelectorWindow spriteSelectorWindow;
    private NodeEditorWindow nodeEditorWindow;
    private FileExplorerWIndow tempWindow;
    private ProjectExplorerWindow projectExplorerWindow;
    private boolean tmpOnce = true;
    private PickingTexture pickingTexture;

    public ImGuiLayer(long glfwWindow, PickingTexture pickingTexture) {
        EventSystem.addObserver(this);
        this.glfwWindow = glfwWindow;
        this.pickingTexture = pickingTexture;
    }

    private void initComponents() {
        this.gameViewWindow = new GameViewWindow();
        this.propertiesWindow = new PropertiesWindow(pickingTexture);
        this.menuBar = new MenuBar();
        this.sceneHierarchyWindow = new SceneHierarchyWindow();
        this.loggerWindow = new LoggerWindow();
        this.textEditorWindow = new TextEditorWindow();
        this.spriteSelectorWindow = new SpriteSelectorWindow();
        this.nodeEditorWindow = new NodeEditorWindow();
        this.tempWindow = new FileExplorerWIndow();
        this.projectExplorerWindow = new ProjectExplorerWindow();
    }

    public void update(float dt, Scene currentScene) {
        startFrame(dt);

        setupDockspace(dt);
        ImGui.showDemoWindow();
        // ImGui.showAboutWindow();
        currentScene.imgui();
        gameViewWindow.imgui(dt);
        propertiesWindow.update(dt, currentScene);
        propertiesWindow.imgui(dt);
        sceneHierarchyWindow.imgui(dt);
        loggerWindow.imgui(dt);
        textEditorWindow.imgui(dt);
        spriteSelectorWindow.imgui(dt);
        nodeEditorWindow.imgui(dt);
        tempWindow.imgui(dt);
        projectExplorerWindow.imgui(dt);
        endFrame();
    }

    private void destroyWindows() {
        textEditorWindow.destroy();
    }

    public void setEditorGameObject(GameObject editorObject) {
        ImGuiLayer.editorObject = editorObject;
    }

    public GameObject getEditorGameObject() {
        return ImGuiLayer.editorObject;
    }

    private void setupDockspace(float dt) {
        int windowFlags = ImGuiWindowFlags.MenuBar | ImGuiWindowFlags.NoDocking;

        ImGuiViewport mainViewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(mainViewport.getWorkPosX(), mainViewport.getWorkPosY());
        // ImGui.setNextWindowSize(mainViewport.getWorkSizeX(), mainViewport.getWorkSizeY());
        ImGui.setNextWindowViewport(mainViewport.getID());

        ImGui.setNextWindowPos(Window.getXPos(), Window.getYPos());
        ImGui.setNextWindowSize(Window.getWidth(), Window.getHeight());
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
        windowFlags |= ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize 
            | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoBringToFrontOnFocus 
            | ImGuiWindowFlags.NoNavFocus;

        ImGui.begin("Dockspace Demo", new ImBoolean(true), windowFlags);
        ImGui.popStyleVar(2);

        // dockspace
        ImGui.dockSpace(ImGui.getID("Dockspace"));
        menuBar.imgui(dt);
        ImGui.end();
    }

    public void setUIColors(int style) {
        setAllUiColors(Settings.getStyleColors(style));
    }

    private void setAllUiColors(ImVec4[] colorVec4arr) {
        for (int i = 0; i < colorVec4arr.length; i++) {
            if (!colorVec4arr[i].equals(null)) {
                setUIColor(i, colorVec4arr[i]);
            }
        }
    }

    private void setUIColor(int id, ImVec4 color) {
        this.style.setColor(id, color.x, color.y, color.z, color.w);
    }

    public void initImGui() {
       
        // initialize ImGui
        ImGui.createContext();
        final ImGuiIO io = ImGui.getIO();
        
        // =======================================================
        // ImGui settings
        // =======================================================
        ImGui.loadIniSettingsFromMemory(util.IOHelper.ResToString(util.Resources.IMGUI_INI));
        io.setIniFilename(null); // saves window config

        // io.setIniFilename("imgui.ini"); // saves window config
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);
        // io.setBackendFlags(ImGuiBackendFlags.HasMouseCursors);
        io.setBackendPlatformName("imgui_java_impl_glfw");
        this.style = ImGui.getStyle();
        
        // =======================================================
        // ui sizes
        // =======================================================
        style.setGrabMinSize(8.0f);
        style.setWindowPadding(2.0f, 4.0f);
        style.setTabRounding(2.0f);
        style.setScrollbarRounding(2.0f);
        style.setFrameRounding(4.0f);
        style.setGrabRounding(4.0f);
        style.setWindowMenuButtonPosition(-1);


        // =======================================================
        // ui colors
        // =======================================================
        setAllUiColors(Settings.getStyleColors(0));

        // =======================================================
        // keyboard mapping
        // =======================================================
        
        // final int[] keyMap = new int[ImGuiKey.COUNT];
        // keyMap[ImGuiKey.Tab] = GLFW_KEY_TAB;
        // keyMap[ImGuiKey.LeftArrow] = GLFW_KEY_LEFT;
        // keyMap[ImGuiKey.RightArrow] = GLFW_KEY_RIGHT;
        // keyMap[ImGuiKey.UpArrow] = GLFW_KEY_UP;
        // keyMap[ImGuiKey.DownArrow] = GLFW_KEY_DOWN;
        // keyMap[ImGuiKey.PageUp] = GLFW_KEY_PAGE_UP;
        // keyMap[ImGuiKey.PageDown] = GLFW_KEY_PAGE_DOWN;
        // keyMap[ImGuiKey.Home] = GLFW_KEY_HOME;
        // keyMap[ImGuiKey.End] = GLFW_KEY_END;
        // keyMap[ImGuiKey.Insert] = GLFW_KEY_INSERT;
        // keyMap[ImGuiKey.Delete] = GLFW_KEY_DELETE;
        // keyMap[ImGuiKey.Backspace] = GLFW_KEY_BACKSPACE;
        // keyMap[ImGuiKey.Space] = GLFW_KEY_SPACE;
        // keyMap[ImGuiKey.Enter] = GLFW_KEY_ENTER;
        // keyMap[ImGuiKey.Escape] = GLFW_KEY_ESCAPE;
        // keyMap[ImGuiKey.KeyPadEnter] = GLFW_KEY_KP_ENTER;
        // keyMap[ImGuiKey.A] = GLFW_KEY_A;
        // keyMap[ImGuiKey.C] = GLFW_KEY_C;
        // keyMap[ImGuiKey.V] = GLFW_KEY_V;
        // keyMap[ImGuiKey.X] = GLFW_KEY_X;
        // keyMap[ImGuiKey.Y] = GLFW_KEY_Y;
        // keyMap[ImGuiKey.Z] = GLFW_KEY_Z;
        // io.setKeyMap(keyMap);
        

        // =======================================================
        // mouse cursors mapping
        // =======================================================
        // mouseCursors[ImGuiMouseCursor.Arrow] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
        // mouseCursors[ImGuiMouseCursor.TextInput] = glfwCreateStandardCursor(GLFW_IBEAM_CURSOR);
        // mouseCursors[ImGuiMouseCursor.ResizeAll] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
        // mouseCursors[ImGuiMouseCursor.ResizeNS] = glfwCreateStandardCursor(GLFW_VRESIZE_CURSOR);
        // mouseCursors[ImGuiMouseCursor.ResizeEW] = glfwCreateStandardCursor(GLFW_HRESIZE_CURSOR);
        // mouseCursors[ImGuiMouseCursor.ResizeNESW] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
        // mouseCursors[ImGuiMouseCursor.ResizeNWSE] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
        // mouseCursors[ImGuiMouseCursor.Hand] = glfwCreateStandardCursor(GLFW_HAND_CURSOR);
        // mouseCursors[ImGuiMouseCursor.NotAllowed] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);

        // =======================================================
        // GLFW callbacks to handle user input
        // =======================================================
        glfwSetKeyCallback(glfwWindow, (w, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                io.setKeysDown(key, true);
            } else if (action == GLFW_RELEASE) {
                io.setKeysDown(key, false);
            }

            io.setKeyCtrl(io.getKeysDown(GLFW_KEY_LEFT_CONTROL) || io.getKeysDown(GLFW_KEY_RIGHT_CONTROL));
            io.setKeyShift(io.getKeysDown(GLFW_KEY_LEFT_SHIFT) || io.getKeysDown(GLFW_KEY_RIGHT_SHIFT));
            io.setKeyAlt(io.getKeysDown(GLFW_KEY_LEFT_ALT) || io.getKeysDown(GLFW_KEY_RIGHT_ALT));
            io.setKeySuper(io.getKeysDown(GLFW_KEY_LEFT_SUPER) || io.getKeysDown(GLFW_KEY_RIGHT_SUPER));

            if (!io.getWantCaptureKeyboard() || gameViewWindow.getWantCaptureMouse()) {
                KeyListener.keyCallback(w, key, scancode, action, mods, "IMGUI");
            }
        });

        glfwSetCharCallback(glfwWindow, (w, c) -> {
            if (c != GLFW_KEY_DELETE) {
                io.addInputCharacter(c);
            }
        });

        glfwSetMouseButtonCallback(glfwWindow, (w, button, action, mods) -> {
            final boolean[] mouseDown = new boolean[5];

            mouseDown[0] = button == GLFW_MOUSE_BUTTON_1 && action != GLFW_RELEASE;
            mouseDown[1] = button == GLFW_MOUSE_BUTTON_2 && action != GLFW_RELEASE;
            mouseDown[2] = button == GLFW_MOUSE_BUTTON_3 && action != GLFW_RELEASE;
            mouseDown[3] = button == GLFW_MOUSE_BUTTON_4 && action != GLFW_RELEASE;
            mouseDown[4] = button == GLFW_MOUSE_BUTTON_5 && action != GLFW_RELEASE;

            io.setMouseDown(mouseDown);

            if (!io.getWantCaptureMouse() && mouseDown[1]) {
                ImGui.setWindowFocus(null);
            }

            if (!io.getWantCaptureMouse() || gameViewWindow.getWantCaptureMouse()) {
                MouseListener.mouseButtonCallback(w, button, action, mods);
            }
        });

        glfwSetScrollCallback(glfwWindow, (w, xOffset, yOffset) -> {
            io.setMouseWheelH(io.getMouseWheelH() + (float) xOffset);
            io.setMouseWheel(io.getMouseWheel() + (float) yOffset);
            if (!io.getWantCaptureMouse() || gameViewWindow.getWantCaptureMouse()) {
                MouseListener.mouseScrollCallback(w, xOffset, yOffset);
            }

        });

        io.setSetClipboardTextFn(new ImStrConsumer() {
            @Override
            public void accept(final String s) {
                glfwSetClipboardString(glfwWindow, s);
            }
        });

        io.setGetClipboardTextFn(new ImStrSupplier() {
            @Override
            public String get() {
                final String clipboardString = glfwGetClipboardString(glfwWindow);
                if (clipboardString != null) {
                    return clipboardString;
                } else {
                    return "";
                }
            }
        });

        // =======================================================
        // ImGui fonts
        // =======================================================
        final ImFontAtlas fontAtlas = io.getFonts();
        final ImFontConfig fontConfig = new ImFontConfig();

        fontConfig.setGlyphRanges(fontAtlas.getGlyphRangesDefault());

        fontConfig.setPixelSnapH(true);
        // fontAtlas.addFontFromFileTTF("app/assets/fonts/OpenSans.ttf", 12, fontConfig);
        fontAtlas.addFontFromMemoryTTF(IOHelper.ResToByteArray(Resources.FONT_RETHINK), Settings.FONT_SIZE, fontConfig);
        fontConfig.destroy();

        // fontAtlas.setFlags(ImGuiFreeTypeBuilderFlags.LightHinting);
        // fontAtlas.build();

        // =======================================================


        imGuiGlfw.init(glfwWindow, false);
        imGuiGl3.init(Window.getGlslVersion());
        initComponents();
    }

    private void startFrame(final float deltaTime) {
        // // get window properties and mouse position
        // float[] winWidth = {Window.getWidth()};
        // float[] winHeight = {Window.getHeight()};
        // double[] mousePosX = {0};
        // double[] mousePosY = {0};
        // glfwGetCursorPos(glfwWindow, mousePosX, mousePosY);

        // // we should call those methods to update Dear ImGui state for the current frame
        // final ImGuiIO io = ImGui.getIO();
        // io.setDisplaySize(winWidth[0], winHeight[0]);
        // io.setDisplayFramebufferScale(1f, 1f);
        // io.setMousePos((float) mousePosX[0], (float) mousePosY[0]);
        // io.setDeltaTime(deltaTime);

        // // update the mouse cursor
        // final int imguiCursor = ImGui.getMouseCursor();
        // glfwSetCursor(glfwWindow, mouseCursors[imguiCursor]);
        // glfwSetInputMode(glfwWindow, GLFW_CURSOR, GLFW_CURSOR_NORMAL);

        imGuiGlfw.newFrame();
        ImGui.newFrame();
    }

    private void endFrame() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, Window.getWidth(), Window.getHeight());
        glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT);


        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final long backupWindowPtr = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            GLFW.glfwMakeContextCurrent(backupWindowPtr);
        }
        // ImGui.end();
        if (tmpOnce) {
            gameViewWindow.makeFocused();
            tmpOnce = false;
        }
    }

    public void destroy() {
        destroyWindows();
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
    }

    public PropertiesWindow getPropertiesWindow() {
        return this.propertiesWindow;
    }

    @Override
    public void onNotify(Event event) {
        switch (event.type) {
            case EventLogged:
                loggerWindow.log((Log) event.getObject());
                break;
            default:
                break;
        }
    }

}
