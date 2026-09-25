package scenes;

import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.GLFW;
import static simplicity.Window.*;
import static simplicity.NewMouseListener.*;
import static simplicity.KeyListener.*;
import static util.Inputs.*;
import static observers.events.EventType.*;

// import editor.FileExplorerWIndow;
// import editor.GameViewWindow;
// import editor.LoggerWindow;
// import editor.MenuBar;
// import editor.NodeEditorWindow;
// import editor.ProjectExplorerWindow;
// import editor.PropertiesWindow;
// import editor.SceneHierarchyWindow;
// import editor.SpriteSelectorWindow;
// import editor.TextEditorWindow;
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
import renderer.NewFramebuffer;
import renderer.PickingTexture;
import scenes.Scene;
import simplicity.Application;
import simplicity.Application.Layer;
import simplicity.GameObject;
import simplicity.KeyListener;
import simplicity.KeyListener.CharEvent;
import simplicity.KeyListener.KeyEvent;
import simplicity.MouseListener;
import simplicity.NewMouseListener.MouseButtonEvent;
import simplicity.NewMouseListener.MouseDroppedPathEvent;
import simplicity.NewMouseListener.MouseScrollEvent;
import simplicity.Window;
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

import observers.events.Event;
import simplicity.Application;
import simplicity.Application.Layer;
import simplicity.Application.RenderContext;

public class NewImGuiLayer implements Layer {

    private Application context;
    private boolean isFrozen = false;
    private boolean isActive = true;
    private boolean isHidden = false;
    private boolean darkAdobe = true;

    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private ImGuiIO io;
    private ImGuiStyle style;
    private boolean initialized = false;
    private World2DLayer world;

    private static final String PAYLOAD_FILES = "FILES";
    private static final int EXTERNAL_DROP_MAX_FRAMES = 10;
    private String[] externalDrop = null;
    private int externalDropFrames = 0;
    private float deltaTime = 0f;

    // private GameObject editorObject;
    // private GameViewWindow gameViewWindow;
    // private PropertiesWindow propertiesWindow;
    // private LoggerWindow loggerWindow;
    // private MenuBar menuBar;
    // private SceneHierarchyWindow sceneHierarchyWindow;
    // private TextEditorWindow textEditorWindow;
    // private SpriteSelectorWindow spriteSelectorWindow;
    // private NodeEditorWindow nodeEditorWindow;
    // private FileExplorerWIndow tempWindow;
    // private ProjectExplorerWindow projectExplorerWindow;
    // private boolean tmpOnce = true;
    // private PickingTexture pickingTexture;


    @Override
    public void onUpdate(float dt) {
        if (!initialized) return;
        this.deltaTime = dt;
    }

    @Override
    public void onRender(RenderContext renderContext) {
        if (!initialized) return;

        imGuiGlfw.newFrame();            // mouse position, display size, dt (Option A)
        ImGui.newFrame();

        submitExternalDrop();            // before any panel, so targets can accept it this frame

        // ... dockspace + panels ...
        renderDockspace();
        ImGui.showDemoWindow();
        // renderDropTestWindow();
        

        ImGui.render();
        NewFramebuffer target = renderContext.framebuffer();   // overlay mode: draw over the chain's frame
        target.bind();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
        target.unbind();

        if (io.hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            long backup = glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            glfwMakeContextCurrent(backup);
        }
    }

    private void renderDockspace() {
        int windowFlags = ImGuiWindowFlags.MenuBar | ImGuiWindowFlags.NoDocking;
        Window window = context.window();

        ImGuiViewport mainViewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(mainViewport.getWorkPosX(), mainViewport.getWorkPosY());
        // ImGui.setNextWindowSize(mainViewport.getWorkSizeX(), mainViewport.getWorkSizeY());
        ImGui.setNextWindowViewport(mainViewport.getID());

        ImGui.setNextWindowPos(window.getXPos(), window.getYPos());
        ImGui.setNextWindowSize(window.getWidth(), window.getHeight());
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
        windowFlags |= ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize 
            | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoBringToFrontOnFocus 
            | ImGuiWindowFlags.NoNavFocus;

        ImGui.begin("Dockspace Demo", new ImBoolean(true), windowFlags);
        ImGui.popStyleVar(2);

        // dockspace
        ImGui.dockSpace(ImGui.getID("Dockspace"));
        // menuBar.imgui(deltaTime);
        ImGui.end();
    }

    @Override
    public void destroy() {
        if (!initialized) return;
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
        initialized = false;
    }

    @Override
    public void onAttach(Application context) {
        World2DLayer world = context.getLayer(World2DLayer.class);
        if (world == null) throw new IllegalStateException(this.getClass().getSimpleName() + " requires a World2DLayer to be pushed first");
        this.world = world;
        this.context = context;
        if (!initialized) {
            initImGui();
        }
    }

    @Override
    public void onDetach() {
        this.context = null;
        this.world = null;
        if (initialized) {
            io.clearInputKeys();
            io.clearInputCharacters();
        }
    }

    @Override
    public void onNotify(Event event) {
        if (!initialized) return;

        if (event.type == KeyInput) {
            KeyEvent keyEvent = (KeyEvent) event;
            boolean isPressed = keyEvent.action != INPUT_RELEASE;

            io.setKeysDown(keyEvent.key, isPressed);

            io.setKeyCtrl((keyEvent.mods & MOD_CONTROL) != 0);
            io.setKeyShift((keyEvent.mods & MOD_SHIFT) != 0);
            io.setKeyAlt((keyEvent.mods & MOD_ALT) != 0);
            io.setKeySuper((keyEvent.mods & MOD_SUPER) != 0);

            if (io.getWantCaptureKeyboard()) event.stopPropagate();
        }

        if (event.type == CharInput) {
            CharEvent charEvent = (CharEvent) event;
            io.addInputCharacter(charEvent.charr);

            if (io.getWantTextInput()) event.stopPropagate();
        }

        if (event.type == MouseButton) {
            MouseButtonEvent mouseBtn = (MouseButtonEvent) event;
            if (mouseBtn.button < 5) {
                io.setMouseDown(mouseBtn.button, mouseBtn.action != INPUT_RELEASE);
            }

            if (io.getWantCaptureMouse()) event.stopPropagate();
        }

        if (event.type == MouseScroll) {
            MouseScrollEvent mouseScrl = (MouseScrollEvent) event;
            io.setMouseWheel(io.getMouseWheel() + (float) mouseScrl.scrollY);
            io.setMouseWheelH(io.getMouseWheelH() + (float) mouseScrl.scrollX);

            if (io.getWantCaptureMouse()) event.stopPropagate();
        }

        if (event.type == MouseDroppedPath) {
            MouseDroppedPathEvent drop = (MouseDroppedPathEvent) event;
            externalDrop = drop.paths;
            externalDropFrames = 0;
            context.window().focusWindow();
        }
    }

    private void submitExternalDrop() {
        if (externalDrop == null) return;
        if (externalDropFrames++ > EXTERNAL_DROP_MAX_FRAMES) {
            externalDrop = null;
            return;
        }
        if (ImGui.beginDragDropSource(ImGuiDragDropFlags.SourceExtern)) {
            ImGui.setDragDropPayload(PAYLOAD_FILES, externalDrop);
            ImGui.endDragDropSource();
        }
    }

    private String[] acceptFileDrop() {
        String[] files = null;
        if (ImGui.beginDragDropTarget()) {
            files = ImGui.acceptDragDropPayload(PAYLOAD_FILES);
            ImGui.endDragDropTarget();
        }
        if (files != null) externalDrop = null;
        return files;
    }

    private String lastDropped = "Drop files here";

    private void renderDropTestWindow() {
        ImGui.begin("Drop Test");
        ImGui.textWrapped(lastDropped);
        ImVec2 avail = ImGui.getContentRegionAvail();
        ImGui.invisibleButton("##dropzone", Math.max(avail.x, 1f), Math.max(avail.y, 1f));
        String[] files = acceptFileDrop();
        if (files != null) lastDropped = "Dropped:\n" + String.join("\n", files);
        ImGui.end();
    }

    @Override
    public void setFrozen(boolean bool) {
        this.isFrozen = bool;
    }

    @Override
    public boolean isFrozen() {
        return isFrozen;
    }

    @Override
    public void setActive(boolean bool) {
        this.isActive = bool;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void setHidden(boolean bool) {
        this.isHidden = bool;
    }

    @Override
    public boolean isHidden() {
        return isHidden;
    }

    public void initImGui() {
        // initialize ImGui
        ImGui.createContext();
        io = ImGui.getIO();
        
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

        if (darkAdobe) {
            // Adobe panels are only dragged by their header/tab, always show a tab, and
            // turn see-through while being docked
            io.setConfigWindowsMoveFromTitleBarOnly(true);
            io.setConfigDockingAlwaysTabBar(true);
            io.setConfigDockingTransparentPayload(true);
            io.setConfigViewportsNoTaskBarIcon(true);   // popped-out panels stay out of the taskbar
        }

        this.style = ImGui.getStyle();
        


        // =======================================================
        // ui sizes & ui colors
        // =======================================================
        if (darkAdobe) {
            // compact spacing
            style.setWindowPadding(6.0f, 6.0f);
            style.setFramePadding(6.0f, 3.0f);
            style.setCellPadding(4.0f, 2.0f);
            style.setItemSpacing(6.0f, 4.0f);
            style.setItemInnerSpacing(4.0f, 4.0f);
            style.setIndentSpacing(14.0f);
            style.setScrollbarSize(10.0f);
            style.setGrabMinSize(10.0f);
            style.setWindowMinSize(32.0f, 32.0f);

            // flat panels, barely rounded controls
            style.setWindowRounding(0.0f);
            style.setChildRounding(0.0f);
            style.setPopupRounding(2.0f);
            style.setFrameRounding(2.0f);
            style.setScrollbarRounding(2.0f);
            style.setGrabRounding(2.0f);
            style.setTabRounding(2.0f);

            // thin hairline borders, including around input fields
            style.setWindowBorderSize(1.0f);
            style.setChildBorderSize(1.0f);
            style.setPopupBorderSize(1.0f);
            style.setFrameBorderSize(1.0f);
            style.setTabBorderSize(0.0f);

            // headers: left-aligned title, no collapse arrow; close button only on the active tab
            style.setWindowTitleAlign(0.0f, 0.5f);
            style.setWindowMenuButtonPosition(ImGuiDir.None);
            style.setTabMinWidthForCloseButton(Float.MAX_VALUE);
            style.setColorButtonPosition(ImGuiDir.Left);

            setAllUiColors(Settings.getStyleColors(5));
        } else {
            style.setGrabMinSize(8.0f);
            style.setWindowPadding(2.0f, 4.0f);
            style.setTabRounding(2.0f);
            style.setScrollbarRounding(2.0f);
            style.setFrameRounding(4.0f);
            style.setGrabRounding(4.0f);
            style.setWindowMenuButtonPosition(-1);
            setAllUiColors(Settings.getStyleColors(0));
        }

        // =======================================================
        // ImGui fonts
        // =======================================================
        final ImFontAtlas fontAtlas = io.getFonts();
        final ImFontConfig fontConfig = new ImFontConfig();

        fontConfig.setGlyphRanges(fontAtlas.getGlyphRangesDefault());
        fontConfig.setPixelSnapH(true);
        if (darkAdobe) {
            fontConfig.setOversampleH(3);
            fontConfig.setOversampleV(2);
            fontAtlas.addFontFromMemoryTTF(IOHelper.ResToByteArray(Resources.FONT_SOURCE_SANS), 16f, fontConfig);
        } else {
            fontAtlas.addFontFromMemoryTTF(IOHelper.ResToByteArray(Resources.FONT_RETHINK), Settings.FONT_SIZE, fontConfig);
        }
        fontConfig.destroy();

        // =======================================================


        imGuiGlfw.init(context.window().ptr(), false);
        imGuiGl3.init("#version 460 core");
        // initComponents();

        initialized = true;
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
}
