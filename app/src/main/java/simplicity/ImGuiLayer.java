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
import renderer.PickingTexture;
import scenes.Scene;
import static org.lwjgl.opengl.GL46.*;

public class ImGuiLayer {

    // private final long[] mouseCursors = new long[ImGuiMouseCursor.COUNT];
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private long glfwWindow;

    private GameViewWindow gameViewWindow;
    private PropertiesWindow propertiesWindow;
    private MenuBar menuBar;
    private SceneHierarchyWindow sceneHierarchyWindow;


    public ImGuiLayer(long glfwWindow, PickingTexture pickingTexture) {
        this.glfwWindow = glfwWindow;
        this.gameViewWindow = new GameViewWindow();
        this.propertiesWindow = new PropertiesWindow(pickingTexture);
        this.menuBar = new MenuBar();
        this.sceneHierarchyWindow = new SceneHierarchyWindow();
    }

    private void imgui() {

        // ImGui.showDemoWindow();
    }

    public void update(float dt, Scene currentScene) {
        startFrame(dt);
        
        setupDockspace();

        currentScene.imgui();
        imgui();
        gameViewWindow.imgui();
        propertiesWindow.update(dt, currentScene);
        propertiesWindow.imgui();
        sceneHierarchyWindow.imgui();

        endFrame();

    }

    private void setupDockspace() {
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


        menuBar.imgui();

        ImGui.end();
    }

    public void initImGui() {
       


        // initialize ImGui
        ImGui.createContext();
        final ImGuiIO io = ImGui.getIO();

        // =======================================================
        // ImGui settings
        // =======================================================
        io.setIniFilename("imgui.ini"); // saves window config
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);
        // io.setBackendFlags(ImGuiBackendFlags.HasMouseCursors);
        io.setBackendPlatformName("imgui_java_impl_glfw");

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
        // glfwSetKeyCallback(glfwWindow, (w, key, scancode, action, mods) -> {
        //     if (action == GLFW_PRESS) {
        //         io.setKeysDown(key, true);
        //     } else if (action == GLFW_RELEASE) {
        //         io.setKeysDown(key, false);
        //     }

        //     io.setKeyCtrl(io.getKeysDown(GLFW_KEY_LEFT_CONTROL) || io.getKeysDown(GLFW_KEY_RIGHT_CONTROL));
        //     io.setKeyShift(io.getKeysDown(GLFW_KEY_LEFT_SHIFT) || io.getKeysDown(GLFW_KEY_RIGHT_SHIFT));
        //     io.setKeyAlt(io.getKeysDown(GLFW_KEY_LEFT_ALT) || io.getKeysDown(GLFW_KEY_RIGHT_ALT));
        //     io.setKeySuper(io.getKeysDown(GLFW_KEY_LEFT_SUPER) || io.getKeysDown(GLFW_KEY_RIGHT_SUPER));

        //     if (!io.getWantCaptureKeyboard() || gameViewWindow.getWantCaptureMouse()) {
        //         KeyListener.keyCallback(w, key, scancode, action, mods);
        //     }
        // });

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
        fontAtlas.addFontFromFileTTF("app/assets/fonts/OpenSans.ttf", 12, fontConfig);

        fontConfig.destroy();

        // fontAtlas.setFlags(ImGuiFreeTypeBuilderFlags.LightHinting);
        // fontAtlas.build();

        // =======================================================


        imGuiGlfw.init(glfwWindow, false);
        imGuiGl3.init(Window.getGlslVersion());
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
    }

    public void destroy() {
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
    }

    public PropertiesWindow getPropertiesWindow() {
        return this.propertiesWindow;
    }

}
