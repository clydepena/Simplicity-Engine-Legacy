package simplicity;

import Fonts.SFont;
import renderer.Shader;
import util.AssetPool;

import org.joml.Vector2f;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class WindowFont {

    private long window;
    private SFont font;
    private int vao;
    private final float SCALE = 0.5f;

    private float[] vertices = {
            // x, y,        r, g, b              ux, uy
            0.5f, 0.5f,     1.0f, 0.2f, 0.11f,   1.0f, 0.0f,
            0.5f, -0.5f,    1.0f, 0.2f, 0.11f,   1.0f, 1.0f,
            -0.5f, -0.5f,   1.0f, 0.2f, 0.11f,   0.0f, 1.0f,
            -0.5f, 0.5f,    1.0f, 0.2f, 0.11f,   0.0f, 0.0f
    };

    private int[] indices = {
            0, 1, 3,
            1, 2, 3
    };

    public WindowFont() {
        init();
        font = new SFont("C:/Users/User/Documents/JavaProjects/2d_game_2/app/assets/fonts/PixelifySans.ttf", 64);
    }

    private void init() {
        glfwInit();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow((int) (1080 * SCALE), (int) (1920 * SCALE), "Font Rendering", NULL, NULL);
        if (window == NULL) {
            System.out.println("Could not create window.");
            glfwTerminate();
            return;
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        // Initialize gl functions for windows using GLAD
        GL.createCapabilities();
    }

    private void uploadSquare() {
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        int stride = 7 * Float.BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);
    }

    public void run() {
        Vector2f[] texCoords = font.getCharacter('A').textureCoordinates;
        vertices[5] = texCoords[0].x; vertices[6] = texCoords[0].y;
        vertices[12] = texCoords[1].x; vertices[13] = texCoords[1].y;
        vertices[19] = texCoords[2].x; vertices[20] = texCoords[2].y;
        vertices[26] = texCoords[3].x; vertices[27] = texCoords[3].y;

        uploadSquare();
        Shader fontShader = AssetPool.getShader("app/assets/shaders/fontShader.glsl");

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT);
            glClearColor(1, 1, 1, 1);

            fontShader.use();
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_BUFFER, font.textureId);
            fontShader.uploadTexture("uFontTexture", 0);

            glBindVertexArray(vao);

            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }
}