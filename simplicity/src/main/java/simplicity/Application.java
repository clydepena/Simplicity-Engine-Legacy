package simplicity;

import java.util.*;
import org.lwjgl.glfw.*;

import observers.EventSystem;
import observers.Observer;
import observers.events.Event;
import renderer.*;
import simplicity.Application.RenderContext;
import simplicity.KeyListener.*;
import simplicity.NewMouseListener.*;
import simplicity.Window.FramebufferResizeEvent;
import simplicity.Window.WindowCloseEvent;

import static org.lwjgl.glfw.GLFW.*;


public abstract class Application implements Observer {

    static {
        GLFWErrorCallback.createPrint(System.err).set();
        if(!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW.");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_MAXIMIZED, 0);
        glfwWindowHint(GLFW_DECORATED, GLFW_TRUE);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            glfwTerminate();
            glfwSetErrorCallback(null).free();
        }));
    }

    public static final class RenderContext {
        private final Renderer renderer;
        private NewFramebuffer framebuffer;

        public RenderContext(Renderer renderer, NewFramebuffer framebuffer) {
            this.renderer = renderer;
            this.framebuffer = framebuffer;
        }

        public Renderer renderer() {return renderer;};
        public NewFramebuffer framebuffer() {return framebuffer;};
        public void setFramebuffer(NewFramebuffer framebuffer) {this.framebuffer = framebuffer;};
    }

    public static interface Layer {        
        abstract void onUpdate(float dt);
        abstract void onRender(RenderContext renderContext);
        abstract void destroy();
        abstract void onAttach(Application context);
        abstract void onDetach();
        abstract void onNotify(Event event);
        abstract void setFrozen(boolean bool);
        abstract boolean isFrozen();
        abstract void setActive(boolean bool);
        abstract boolean isActive();
        abstract void setHidden(boolean bool);
        abstract boolean isHidden();
    }

    protected List<Layer> layerStack = new ArrayList<>();
    protected Window window;
    protected Renderer renderer;
    protected boolean running = false;
    protected String title;
    protected NewFramebuffer mainFramebuffer;
    protected final Queue<Runnable> layerCommands = new ArrayDeque<>();

    public Application(String title) {
        EventSystem.addObserver(this);
        this.title = title;
        this.window = Window.create(title);
        this.renderer = new Renderer(window);
    }

    @Override
    public void onNotify(Event event) {
        if (event instanceof FramebufferResizeEvent resize) {
            if (!window.isMinimized()) {
                mainFramebuffer.destroy();
                mainFramebuffer = new NewFramebuffer(resize.width, resize.height);
            }
        }
        if (event instanceof FramebufferResizeEvent ||
            event instanceof WindowCloseEvent ||
            event instanceof KeyEvent ||
            event instanceof CharEvent ||
            event instanceof MouseButtonEvent ||
            event instanceof MouseMovedEvent || 
            event instanceof MouseScrollEvent
        ) {
            int i = layerStack.size() - 1;
            while (i >= 0) {
                if (event.stopPropagate) break;
                Layer layer = layerStack.get(i);
                if(layer.isActive() && !layer.isFrozen()) layer.onNotify(event);
                i--;
            }
            event.stopPropagate();
        }
    }

    public void pushLayer(Layer layer) {
        layerCommands.add(() -> { layerStack.add(layer); layer.onAttach(this); });
    }

    public void popLayer() {
        if (layerStack.isEmpty()) return;
        layerCommands.add(() -> { Layer l = layerStack.removeLast(); l.onDetach(); });
    }

    public <T extends Layer> T getLayer(Class<T> type) {
        for (Layer layer : layerStack) if (type.isInstance(layer)) return type.cast(layer);
        return null;
    }

    public void removeLayer(Layer layer) {
        layerCommands.add(() -> { if (layerStack.remove(layer)) { layer.onDetach(); } });
    }

    public void swapLayer(Layer layer1, Layer layer2) {
        layerCommands.add(() -> { 
           if (layerStack.contains(layer1) && layerStack.contains(layer2)) {
                // take both indices before writing: after the first set, indexOf(layer2) would find the new copy
                int i = layerStack.indexOf(layer1);
                int j = layerStack.indexOf(layer2);
                layerStack.set(i, layer2);
                layerStack.set(j, layer1);
           } else if (layerStack.contains(layer1) && !layerStack.contains(layer2)) {
                layer1.onDetach();
                layerStack.set(layerStack.indexOf(layer1), layer2);
                layer2.onAttach(this);
           } else if (!layerStack.contains(layer1) && layerStack.contains(layer2)) {
                layer2.onDetach();
                layerStack.set(layerStack.indexOf(layer2), layer1);
                layer1.onAttach(this);
           }
        });
    }

    private void applyLayerCommands() {
        while (!layerCommands.isEmpty()) layerCommands.poll().run();
    }

    public void run() {
        window.init();
        renderer.init();

        mainFramebuffer = new NewFramebuffer(window.getFramebufferWidth(), window.getFramebufferHeight());

        applyLayerCommands();

        window.setVisible(true);
        float beginTime = getTime(), endTime, dt = 0f;
        while (!window.shouldClose()) {
            window.pollEvents();
            EventSystem.processEvents();

            for (Layer layer : layerStack) {
                if (!layer.isFrozen() && layer.isActive()) layer.onUpdate(dt);
            }
            renderer.onUpdate(dt);

            if (!window.isMinimized()) {
                mainFramebuffer.bind();
                renderer.clear();
                mainFramebuffer.unbind();
                NewFramebuffer currFramebuffer = mainFramebuffer;
                for (Layer layer : layerStack) {
                    if (layer.isHidden() || !layer.isActive()) continue;
                    renderer.setFramebuffer(currFramebuffer);
                    RenderContext renderContext = new RenderContext(renderer, currFramebuffer);
                    layer.onRender(renderContext);
                    currFramebuffer = renderContext.framebuffer();
                }
                renderer.present(currFramebuffer);
                renderer.swapBuffers();
            } else {
                window.awaitEvents();
            }

            applyLayerCommands();
            endTime = getTime();
            dt = Math.min(endTime - beginTime, 0.1f);
            beginTime = endTime;
        }

        while (!layerStack.isEmpty()) {
            Layer layer = layerStack.removeLast();
            layer.onDetach();
            layer.destroy();
        }

        mainFramebuffer.destroy();
        renderer.destroy();
        window.destroy();
        onClose();
    }

    public void close() {
        window.confirmClose();
    }

    public float getTime() {
        return (float) glfwGetTime();
    }

    public String title() {
        return title;
    }

    public Window window() {
        return window;
    }

    public Renderer renderer() {
        return renderer;
    }

    protected abstract void onClose();
}
