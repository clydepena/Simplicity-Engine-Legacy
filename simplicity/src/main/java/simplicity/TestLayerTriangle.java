package simplicity;

import static org.lwjgl.opengl.GL33.GL_FALSE;
import static org.lwjgl.opengl.GL33.GL_FLOAT;
import static org.lwjgl.opengl.GL33.GL_TRIANGLES;
import static org.lwjgl.opengl.GL33.glDrawArrays;
import static org.lwjgl.opengl.GL33.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL33.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL33.glBindBuffer;
import static org.lwjgl.opengl.GL33.glBufferData;
import static org.lwjgl.opengl.GL33.glDeleteBuffers;
import static org.lwjgl.opengl.GL33.glGenBuffers;
import static org.lwjgl.opengl.GL33.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL33.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL33.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL33.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL33.glAttachShader;
import static org.lwjgl.opengl.GL33.glCompileShader;
import static org.lwjgl.opengl.GL33.glCreateProgram;
import static org.lwjgl.opengl.GL33.glCreateShader;
import static org.lwjgl.opengl.GL33.glDeleteProgram;
import static org.lwjgl.opengl.GL33.glDeleteShader;
import static org.lwjgl.opengl.GL33.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL33.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL33.glGetProgrami;
import static org.lwjgl.opengl.GL33.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL33.glGetShaderi;
import static org.lwjgl.opengl.GL33.glLinkProgram;
import static org.lwjgl.opengl.GL33.glShaderSource;
import static org.lwjgl.opengl.GL33.glUseProgram;
import static org.lwjgl.opengl.GL33.glVertexAttribPointer;
import static org.lwjgl.opengl.GL33.glBindVertexArray;
import static org.lwjgl.opengl.GL33.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL33.glGenVertexArrays;
import static org.lwjgl.opengl.GL33.*;

import observers.events.Event;
import simplicity.Application.Layer;
import simplicity.Application.RenderContext;
import renderer.*;
import simplicity.KeyListener.KeyEvent;
import static util.Inputs.*;

public class TestLayerTriangle implements Layer {

    private static final String VERTEX_SRC = """
        #version 460 core
        layout (location = 0) in vec2 aPos;
        layout (location = 1) in vec3 aColor;

        out vec3 fColor;

        void main() {
            fColor = aColor;
            gl_Position = vec4(aPos, 0.0, 1.0);
        }
        """;

    private static final String FRAGMENT_SRC = """
        #version 460 core
        in vec3 fColor;

        out vec4 color;

        void main() {
            color = vec4(fColor, 1.0);
        }
        """;

    // pos (normalized device coords)   // color
    private static final float[] VERTICES = {
         0.0f,  0.5f,                    1.0f, 0.0f, 0.0f,
        -0.5f, -0.5f,                    0.0f, 1.0f, 0.0f,
         0.5f, -0.5f,                    0.0f, 0.0f, 1.0f,
    };

    private Application context;
    private boolean isFrozen = false;
    private boolean isACtive = true;
    private boolean isHidden = false;

    private int programID, vaoID, vboID;

    float ctr = 0;

    @Override
    public void onUpdate(float dt) {
        ctr += dt;
        if (ctr >= 1) {

            ctr = 0;
        }
    }

    @Override
    public void onRender(RenderContext renderContext) {
        Renderer renderer = renderContext.renderer();
        renderer.begin();                   // binds the chain's framebuffer

        glUseProgram(programID);            // replaces the renderer's shader for this draw
        glBindVertexArray(vaoID);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindVertexArray(0);

        renderer.end();                     // detaches the shader, unbinds the framebuffer
    }

    @Override
    public void destroy() {
        glDeleteBuffers(vboID);
        glDeleteVertexArrays(vaoID);
        glDeleteProgram(programID);
        System.out.println("Destroyed " + this.getClass().getName());
    }

    @Override
    public void onAttach(Application context) {
        this.context = context;

        // runs after renderer.init() (layer commands are queued), so the GL context is current
        programID = createProgram(VERTEX_SRC, FRAGMENT_SRC);

        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, VERTICES, GL_STATIC_DRAW);

        int stride = 5 * Float.BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        System.out.println("Attached to " + context.title());
    }

    @Override
    public void onDetach() {
        System.out.println("Detached from " + context.title());
        context = null;
    }

    @Override
    public void onNotify(Event event) {
        // System.out.println(event);
        if (event instanceof KeyEvent) {
            KeyEvent keyEvent = ((KeyEvent) event);
            if (keyEvent.action == KEY_RELEASE) {
                if (keyEvent.key == KEY_ENTER) {
                    isHidden = !isHidden;
                }
                if (keyEvent.key == KEY_BACKSPACE) {
                    context.removeLayer(this);
                }
                if (keyEvent.key == KEY_ESCAPE) {
                    context.close();
                }
            }
        }
    }

    @Override
    public boolean isFrozen() {
        return isFrozen;
    }

    @Override
    public void setFrozen(boolean bool) {
        isFrozen = bool;
    }

    private static int createProgram(String vertexSrc, String fragmentSrc) {
        int vertex = compileShader(GL_VERTEX_SHADER, vertexSrc);
        int fragment = compileShader(GL_FRAGMENT_SHADER, fragmentSrc);

        int program = glCreateProgram();
        glAttachShader(program, vertex);
        glAttachShader(program, fragment);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Triangle program link failed:\n" + glGetProgramInfoLog(program));
        }

        glDeleteShader(vertex);
        glDeleteShader(fragment);
        return program;
    }

    private static int compileShader(int type, String src) {
        int shader = glCreateShader(type);
        glShaderSource(shader, src);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Triangle shader compile failed:\n" + glGetShaderInfoLog(shader));
        }
        return shader;
    }

    @Override
    public void setActive(boolean bool) {
        isACtive = bool;
    }

    @Override
    public boolean isActive() {
        return  isACtive;
    }

    @Override
    public void setHidden(boolean bool) {
        isHidden = bool;
    }

    @Override
    public boolean isHidden() {
        return isHidden;
    }
}
