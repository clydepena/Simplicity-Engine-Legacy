package renderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.joml.Vector2f;
import org.joml.Vector4f;

import simplicity.Window;

import static org.lwjgl.opengl.GL46.*;
import util.*;

public class DebugDraw {
    private static int MAX_LINES = 500;
    private static List<Line2D> lines = new ArrayList<>();
    private static float[] vertexArray = new float[MAX_LINES * 7 * 2];
    private static Shader shader = AssetPool.getShaderFromRes(Resources.Editor.SHADER_LINE);

    private static int vboID, vaoID;

    private static boolean started = false;

    private static Vector4f defaultColor = new Vector4f(1, 0 ,0, 1);

    public static void start() {
        // generate vao
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        // vreate vbo & buffer memory
        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, vertexArray.length * Float.BYTES, GL_DYNAMIC_DRAW);

        // enable vertex array attributes
        glVertexAttribPointer(0, 4, GL_FLOAT, false, 7 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 4, GL_FLOAT, false, 7 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glLineWidth(0.5f);
    }

    public static void beginFrame() {
        if(!started) {
            start();
            started = true;
        }

        // remove inactive lines
        for(int i = 0; i < lines.size(); i++) {
            if(lines.get(i).beginFrame() < 0) {
                lines.remove(i);
                i--;
            }
        }
    }

    public static void draw() {
        if(lines.size() <= 0) return;

        int index = 0;
        for(Line2D line: lines) {
            for(int i = 0; i < 2; i++) {
                Vector2f position = i == 0 ? line.getStart() : line.getEnd();
                Vector4f color = line.getColor();

                // System.out.println(color.x + " " + color.y + " " + color.z + " " + color.w);

                // load position
                vertexArray[index] = position.x;
                vertexArray[index + 1] = position.y;
                vertexArray[index + 2] = -10.0f;

                // load color
                vertexArray[index + 3] = color.x;
                vertexArray[index + 4] = color.y;
                vertexArray[index + 5] = color.z;
                vertexArray[index + 6] = color.w;
                index += 7;
            }
        }
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferSubData(GL_ARRAY_BUFFER, 0, Arrays.copyOfRange(vertexArray, 0, lines.size() * 7 * 2));

        // use shader
        shader.use();
        shader.uploadMat4f("uProjection", Window.getScene().camera().getProjectionMatrix());
        shader.uploadMat4f("uView", Window.getScene().camera().getViewMatrix());

        // bind vao
        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        // draw batch
        glDrawArrays(GL_LINES, 0, lines.size() * 7 * 2);

        // disable location
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);

        // unbind shader
        shader.detach();
    }

    // =============================================================
    // LINE 2D METHODS
    // =============================================================

    public static void addLine2D(Vector2f start, Vector2f end, Vector4f color, int lifetime) {
        if(lines.size() >= MAX_LINES) return;
        DebugDraw.lines.add(new Line2D(start, end, color, lifetime));
    }

    public static void addLine2D(Vector2f start, Vector2f end) {
        addLine2D(start, end, defaultColor, 1);
    }

    public static void addLine2D(Vector2f start, Vector2f end, int lifetime) {
        addLine2D(start, end, defaultColor, lifetime);
    }

    // =============================================================
    // BOX 2D METHODS
    // =============================================================

    public static void addBox2D(Vector2f center, Vector2f dimensions, float rotation, Vector4f color, int lifetime) {
        Vector2f min = new Vector2f(center).sub(new Vector2f(dimensions).mul(0.5f));
        Vector2f max = new Vector2f(center).add(new Vector2f(dimensions).mul(0.5f));

        Vector2f[] vertices = {
            new Vector2f(min.x, min.y), new Vector2f(min.x, max.y),
            new Vector2f(max.x, max.y), new Vector2f(max.x, min.y)
        };

        if(rotation != 0.0f) {
            for(Vector2f vert: vertices) {
                SMath.rotate(vert, rotation, center);
            }
        }

        addLine2D(vertices[0], vertices[1], color, lifetime);
        addLine2D(vertices[0], vertices[3], color, lifetime);
        addLine2D(vertices[1], vertices[2], color, lifetime);
        addLine2D(vertices[2], vertices[3], color, lifetime);
    }

    public static void addBox2D(Vector2f center, Vector2f dimensions) {
        addBox2D(center, dimensions, 0.0f, defaultColor, 1);
    }

    public static void addBox2D(Vector2f center, Vector2f dimensions, int lifetime) {
        addBox2D(center, dimensions, 0.0f, defaultColor, lifetime);
    }

    public static void addBox2D(Vector2f center, Vector2f dimensions, float rotation) {
        addBox2D(center, dimensions, rotation, defaultColor, 1);
    }

    public static void addBox2D(Vector2f center, Vector2f dimensions, float rotation, int lifetime) {
        addBox2D(center, dimensions, rotation, defaultColor, lifetime);
    }

    // =============================================================
    // CIRCLE METHODS
    // =============================================================

    public static void addCircle(Vector2f center, float radius, Vector4f color, int lifetime) {
        Vector2f[] points = new Vector2f[32];
        float increment = 360.0f / points.length;
        float currentAngle = 0;

        for(int i = 0; i < points.length; i++) {
            Vector2f temp = new Vector2f(0, radius);
            SMath.rotate(temp, currentAngle, new Vector2f());
            points[i] = new Vector2f(temp).add(center);

            if(i > 0) {
                addLine2D(points[i - 1], points[i], color, lifetime);
            }
            currentAngle += increment;
        }
        addLine2D(points[points.length - 1], points[0], color, lifetime);
    }

    public static void addCircle(Vector2f center, float radius, int lifetime) {
        addCircle(center, radius, defaultColor, lifetime);
    }

    public static void addCircle(Vector2f center, float radius) {
        addCircle(center, radius, defaultColor, 1);
    }
}
