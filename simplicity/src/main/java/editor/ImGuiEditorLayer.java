package editor;

import static org.lwjgl.glfw.GLFW.*;
import org.lwjgl.glfw.GLFW;

import editor.panels.*;

import static simplicity.Window.*;
import static simplicity.MouseListener.*;
import static simplicity.KeyListener.*;
import static util.Inputs.*;

import java.util.List;

import static observers.events.EventType.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

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
import renderer.Framebuffer;
import scenes.World2DLayer;
import simplicity.Application;
import simplicity.Application.Layer;
import simplicity.GameObject;
import simplicity.KeyListener;
import simplicity.KeyListener.CharEvent;
import simplicity.KeyListener.KeyEvent;
import simplicity.MouseListener;
import simplicity.MouseListener.MouseButtonEvent;
import simplicity.MouseListener.MouseDroppedPathEvent;
import simplicity.MouseListener.MouseScrollEvent;
import simplicity.Window;
import util.IOHelper;
import util.Resources;
import util.Settings;
import static logger.Log.LogLevel.*;
import simplicity.Application.RenderContext;

public abstract class ImGuiEditorLayer implements Layer {

    protected Application context;
    protected boolean isFrozen = false;
    protected boolean isActive = true;
    protected boolean isHidden = false;
    protected boolean darkAdobe = true;

    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private ImGuiIO io;
    protected ImGuiStyle style;
    private boolean initialized = false;

    private static final String PAYLOAD_FILES = "FILES";
    private static final int EXTERNAL_DROP_MAX_FRAMES = 10;
    private String[] externalDrop = null;
    private int externalDropFrames = 0;

    protected abstract void onRenderMenuBar();
    protected abstract void onRenderEditor(RenderContext renderContext);
    protected abstract void onUpdateEditor(float dt);
    protected abstract void onInitEditor();
    protected abstract void onDestroyEditor();
    protected abstract void onNotifyEditor(Event event);

    /**
     * True when the editor shows the world's frame as an ImGui image (e.g. a viewport panel in framebuffer mode).
     * ImGui then renders into its own frame instead of over the world's, since it can't sample the texture it draws into,
     * and the dockspace stays opaque. False: ImGui draws over the world and the dockspace's central node is see-through.
     */
    protected boolean rendersWorldAsImage() { return false; }

    /** True while the mouse is over the part of the editor showing the world, so mouse input still reaches the world. */
    protected boolean isMouseOverWorld() { return false; }

    // ImGui's own render target, only used while rendersWorldAsImage()
    private Framebuffer uiFrame;
    @Override
    public final void onUpdate(float dt) {
        if (!initialized) return;
        onUpdateEditor(dt);
    }

    protected boolean shouldRenderDefaultDockspace() { return true; }

    @Override
    public final void onRender(RenderContext renderContext) {
        if (!initialized) return;

        // decided once per frame, so the dockspace style and the render target always agree
        final boolean worldAsImage = rendersWorldAsImage();
        final boolean dockspace = shouldRenderDefaultDockspace();
        final Framebuffer worldFrame = renderContext.framebuffer();

        imGuiGlfw.newFrame();            // mouse position, display size, dt (Option A)
        ImGui.newFrame();

        submitExternalDrop();            // before any panel, so targets can accept it this frame

        if (dockspace) {
            beginDockspace(!worldAsImage);
            if (ImGui.beginMenuBar()) {
                onRenderMenuBar();
                ImGui.endMenuBar();
            }
            endDockspace();
        }
        onRenderEditor(renderContext);   // panels read the world's frame from renderContext.framebuffer()

        ImGui.render();
        Framebuffer target;
        if (worldAsImage) {
            // ImGui samples worldFrame as a texture, so it must draw somewhere else
            target = getUiFrame(worldFrame.getWidth(), worldFrame.getHeight());
            target.bind();
            glClearColor(0, 0, 0, 1);
            glClear(GL_COLOR_BUFFER_BIT);
        } else {
            // overlay mode: draw over the chain's frame
            target = worldFrame;
            target.bind();
        }
        imGuiGl3.renderDrawData(ImGui.getDrawData());
        target.unbind();
        if (worldAsImage) renderContext.setFramebuffer(target);   // the next layer / present() gets the editor image

        if (io.hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            long backup = glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            glfwMakeContextCurrent(backup);
        }
    }

    /** Creates or resizes ImGui's own render target to match the world's frame. */
    private Framebuffer getUiFrame(int width, int height) {
        if (uiFrame == null || uiFrame.getWidth() != width || uiFrame.getHeight() != height) {
            if (uiFrame != null) uiFrame.destroy();
            uiFrame = new Framebuffer(width, height);
        }
        return uiFrame;
    }

    private void beginDockspace(boolean dockTransparent) {
        if (!dockTransparent) {
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
            // renderMenuBar(renderContext);
        } else {
            ImGuiViewport vp = ImGui.getMainViewport();
            ImGui.setNextWindowPos(vp.getWorkPosX(), vp.getWorkPosY());
            ImGui.setNextWindowSize(vp.getWorkSizeX(), vp.getWorkSizeY());
            ImGui.setNextWindowViewport(vp.getID());

            int windowFlags = ImGuiWindowFlags.MenuBar | ImGuiWindowFlags.NoDocking
                | ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus
                | ImGuiWindowFlags.NoBackground;                      // host window draws nothing

            ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);
            ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
            ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.0f, 0.0f);   // no gap around the dockspace
            ImGui.begin("Dockspace Demo", new ImBoolean(true), windowFlags);
            ImGui.popStyleVar(3);

            ImGui.dockSpace(ImGui.getID("Dockspace"), 0, 0, ImGuiDockNodeFlags.PassthruCentralNode);
            // menuBar.imgui(deltaTime);
            // renderMenuBar(renderContext);
        }
    }

    private void endDockspace() {
        ImGui.end();
    }
    
    @Override
    public final void destroy() {
        if (!initialized) return;
        onDestroyEditor();
        if (uiFrame != null) {
            uiFrame.destroy();
            uiFrame = null;
        }
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
        initialized = false;
    }

    @Override
    public final void onAttach(Application context) {
        this.context = context;
        if (!initialized) {
            initImGui();
        }
    }

    @Override
    public final void onDetach() {
        this.context = null;
        if (initialized) {
            io.clearInputKeys();
            io.clearInputCharacters();
        }
    }

    @Override
    public final void onNotify(Event event) {
        if (!initialized) return;

        if (event.type == KeyInput) {
            KeyEvent keyEvent = (KeyEvent) event;
            boolean isPressed = keyEvent.action != KEY_RELEASE;

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
            // imGuiGlfw.newFrame() sets MouseDown from its own "just pressed" latch or the live GLFW state, so
            // io.setMouseDown here would be overwritten; its callback sets the latch, which also keeps a press and
            // release that arrive before the same frame from being lost
            imGuiGlfw.mouseButtonCallback(context.window().ptr(), mouseBtn.button, mouseBtn.action, mouseBtn.mods);

            if (io.getWantCaptureMouse() && !isMouseOverWorld()) event.stopPropagate();
        }

        if (event.type == MouseScroll) {
            MouseScrollEvent mouseScrl = (MouseScrollEvent) event;
            io.setMouseWheel(io.getMouseWheel() + (float) mouseScrl.scrollY);
            io.setMouseWheelH(io.getMouseWheelH() + (float) mouseScrl.scrollX);

            if (io.getWantCaptureMouse() && !isMouseOverWorld()) event.stopPropagate();
        }

        if (event.type == MouseDroppedPath) {
            MouseDroppedPathEvent drop = (MouseDroppedPathEvent) event;
            externalDrop = drop.paths;
            externalDropFrames = 0;
            context.window().focusWindow();
        }
        onNotifyEditor(event);
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

    public String[] acceptFileDrop() {
        String[] files = null;
        if (ImGui.beginDragDropTarget()) {
            files = ImGui.acceptDragDropPayload(PAYLOAD_FILES);
            ImGui.endDragDropTarget();
        }
        if (files != null) externalDrop = null;
        return files;
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

    /**
     * Merges the Font Awesome icons into the font added just before, so icons are text: FontAwesomeIcons.Folder + " Assets".
     * Must run right after that font's addFontFromMemoryTTF: merge mode adds to the most recently added font.
     */
    private static void mergeIconFont(ImFontAtlas fontAtlas, float textSize) {
        final ImFontConfig iconConfig = new ImFontConfig();
        iconConfig.setMergeMode(true);
        iconConfig.setPixelSnapH(true);
        iconConfig.setGlyphMinAdvanceX(textSize);   // every icon at least one text-height wide, so icon columns line up
        fontAtlas.addFontFromMemoryTTF(IOHelper.ResToByteArray(Resources.FONT_AWESOME_SOLID), textSize * 0.9f,
                                       iconConfig, FontAwesomeIcons._IconRange);
        iconConfig.destroy();
    }

    private void initImGui() {
        // initialize ImGui
        ImGui.createContext();
        io = ImGui.getIO();
        
        // =======================================================
        // ImGui settings
        // =======================================================
        ImGui.loadIniSettingsFromMemory(util.IOHelper.ResToString(util.Resources.Editor.IMGUI_INI));
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
            mergeIconFont(fontAtlas, 16f);
        } else {
            fontAtlas.addFontFromMemoryTTF(IOHelper.ResToByteArray(Resources.FONT_RETHINK), Settings.FONT_SIZE, fontConfig);
            mergeIconFont(fontAtlas, Settings.FONT_SIZE);
        }
        fontConfig.destroy();

        // =======================================================


        imGuiGlfw.init(context.window().ptr(), false);
        imGuiGl3.init("#version 460 core");
        // initComponents();

        onInitEditor();
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

    public Application appContext() {
        return context;
    }
}

/* 
    // renderDockspace(): host + dockspace
    int hostFlags =  your existing flags  | ImGuiWindowFlags.NoBackground;
    ImGui.begin("Dockspace", new ImBoolean(true), hostFlags);
    ImGui.dockSpace(ImGui.getID("Dockspace"), 0, 0, ImGuiDockNodeFlags.PassthruCentralNode);
    ImGui.end();

    // the see-through panel
    private boolean viewportHovered = false;

    private void renderViewportWindow() {
        ImGui.begin("Viewport", ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.NoScrollbar);
        viewportHovered = ImGui.isWindowHovered();
        ImGui.end();
    }
    
*/