package renderer;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import static org.lwjgl.opengl.GL33.*;
import components.SpriteRenderer;
import simplicity.Transform;
import java.util.*;

public class NewRenderBatch implements Comparable<NewRenderBatch> {

    // vertex
    // ======
    // pos              // color                        // texture coords   // texture id   // entity id
    // float, float,    float, float, float, float      float, float        float           float

    private static final int POS_SIZE = 2;
    private static final int COLOR_SIZE = 4;
    private static final int TEX_COORDS_SIZE = 2;
    private static final int TEX_ID_SIZE = 1;
    private static final int ENTITY_ID_SIZE = 1;

    private static final int POS_OFFSET = 0;
    private static final int COLOR_OFFSET = POS_OFFSET + POS_SIZE * Float.BYTES;
    private static final int TEX_COORDS_OFFSET = COLOR_OFFSET + COLOR_SIZE * Float.BYTES;
    private static final int TEX_ID_OFFSET = TEX_COORDS_OFFSET + TEX_COORDS_SIZE * Float.BYTES;
    private static final int ENTITY_ID_OFFSET = TEX_ID_OFFSET + TEX_ID_SIZE * Float.BYTES;

    private static final int VERTEX_SIZE = 10;
    private static final int VERTEX_SIZE_BYTES = VERTEX_SIZE * Float.BYTES;

    // slot 0 is reserved for "no texture", so a batch holds at most 7 textures (units 1..7)
    private static final int MAX_TEXTURES = 7;
    private static final int[] TEX_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7};

    private SpriteRenderer[] sprites;
    private int numSprites;
    private boolean hasRoom;
    private float[] vertices;

    private List<Texture> textures;
    private int vaoID, vboID, eboID;
    private int maxBatchSize;
    private int zIndex;

    // reused by loadVertexProperties to avoid allocating per sprite
    private final Matrix4f transformMatrix = new Matrix4f();
    private final Vector4f currentPos = new Vector4f();

    public NewRenderBatch(int maxBatchSize, int zIndex) {
        this.zIndex = zIndex;
        this.sprites = new SpriteRenderer[maxBatchSize];
        this.maxBatchSize = maxBatchSize;

        // 4 vertices quads
        vertices = new float[maxBatchSize * 4 * VERTEX_SIZE];

        this.numSprites = 0;
        this.hasRoom = true;
        this.textures = new ArrayList<>();
    }

    public void start() {
        // generate and bind a vertex array obj
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        // allocate space for vertices
        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, vertices.length * Float.BYTES, GL_DYNAMIC_DRAW);

        // create & upload indices buffer
        eboID = glGenBuffers();
        int[] indices = generateIndices();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        // enable buffer attribute pointers (the VAO remembers these)
        glVertexAttribPointer(0, POS_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, POS_OFFSET);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, COLOR_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, COLOR_OFFSET);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, TEX_COORDS_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, TEX_COORDS_OFFSET);
        glEnableVertexAttribArray(2);

        glVertexAttribPointer(3, TEX_ID_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, TEX_ID_OFFSET);
        glEnableVertexAttribArray(3);

        glVertexAttribPointer(4, ENTITY_ID_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, ENTITY_ID_OFFSET);
        glEnableVertexAttribArray(4);

        glBindVertexArray(0);
    }

    public void destroy() {
        glDeleteVertexArrays(vaoID);
        glDeleteBuffers(vboID);
        glDeleteBuffers(eboID);
    }

    public void addSprite(SpriteRenderer spr) {
        // get index & add renderObject
        int index = this.numSprites;
        this.sprites[index] = spr;
        this.numSprites++;

        if (spr.getTexture() != null) {
            if (!textures.contains(spr.getTexture())) {
                textures.add(spr.getTexture());
            }
        }

        // add properties to local vertices array
        loadVertexProperties(index);

        if (numSprites >= this.maxBatchSize) {
            this.hasRoom = false;
        }
    }

    /**
     * Draws this batch with the shader already in use (bound and given its camera uniforms by Renderer.begin()).
     * Sprites whose zIndex no longer matches this batch are removed and added to movedOut;
     * the caller re-adds them after all batches have been drawn.
     */
    public void render(Shader shader, List<SpriteRenderer> movedOut) {
        boolean rebufferData = false;
        for (int i = 0; i < numSprites; i++) {
            SpriteRenderer spr = sprites[i];

            if (spr.gameObject.transform.zIndex != this.zIndex) {
                movedOut.add(spr);
                removeAt(i);
                i--;
                continue;
            }

            if (spr.isDirty()) {
                loadVertexProperties(i);
                spr.setClean();
                rebufferData = true;
            }
        }

        if (numSprites == 0) return;

        if (rebufferData) {
            glBindBuffer(GL_ARRAY_BUFFER, vboID);
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        }

        for (int i = 0; i < textures.size(); i++) {
            glActiveTexture(GL_TEXTURE0 + i + 1);
            textures.get(i).bind();
        }
        shader.uploadIntArray("uTextures", TEX_SLOTS);

        glBindVertexArray(vaoID);
        glDrawElements(GL_TRIANGLES, this.numSprites * 6, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);

        for (int i = 0; i < textures.size(); i++) {
            glActiveTexture(GL_TEXTURE0 + i + 1);
            textures.get(i).unbind();
        }
        glActiveTexture(GL_TEXTURE0);
    }

    private void loadVertexProperties(int index) {
        SpriteRenderer sprite = this.sprites[index];
        Transform transform = sprite.gameObject.transform;

        // find offset within array (4 vertices per sprite)
        int offset = index * 4 * VERTEX_SIZE;

        Vector4f color = sprite.getColor();
        Vector2f[] texCoords = sprite.getTexCoords();

        int texId = 0;
        if (sprite.getTexture() != null) {
            for (int i = 0; i < textures.size(); i++) {
                if (textures.get(i).equals(sprite.getTexture())) {
                    texId = i + 1;
                    break;
                }
            }
        }

        boolean isRotated = transform.rotation != 0.0f;
        if (isRotated) {
            transformMatrix.identity()
                .translate(transform.position.x, transform.position.y, 0)
                .rotate((float) Math.toRadians(transform.rotation), 0, 0, 1)
                .scale(transform.scale.x, transform.scale.y, 1);
        }

        // add vertices with the appropriate properties
        float xAdd = 0.5f;
        float yAdd = 0.5f;
        for (int i = 0; i < 4; i++) {
            if (i == 1) {
                yAdd = -0.5f;
            } else if (i == 2) {
                xAdd = -0.5f;
            } else if (i == 3) {
                yAdd = 0.5f;
            }

            if (isRotated) {
                currentPos.set(xAdd, yAdd, 0, 1).mul(transformMatrix);
            } else {
                currentPos.set(
                    transform.position.x + (xAdd * transform.scale.x),
                    transform.position.y + (yAdd * transform.scale.y),
                    0,
                    1
                );
            }

            // load position
            vertices[offset] = currentPos.x;
            vertices[offset + 1] = currentPos.y;

            // load color
            vertices[offset + 2] = color.x;
            vertices[offset + 3] = color.y;
            vertices[offset + 4] = color.z;
            vertices[offset + 5] = color.w;

            // load texture coordinates
            vertices[offset + 6] = texCoords[i].x;
            vertices[offset + 7] = texCoords[i].y;

            // load texture id
            vertices[offset + 8] = texId;

            // load entity id
            vertices[offset + 9] = sprite.gameObject.getUid() + 1;

            offset += VERTEX_SIZE;
        }
    }

    private int[] generateIndices() {
        // 6 indices per quad (3 per triangle)
        int[] elements = new int[6 * maxBatchSize];
        for (int i = 0; i < maxBatchSize; i++) {
            loadElementIndices(elements, i);
        }
        return elements;
    }

    private void loadElementIndices(int[] elements, int index) {
        int offsetArrayIndex = 6 * index;
        int offset = 4 * index;

        // 3, 2, 0, 0, 2, 1         7, 6, 4, 4, 6, 5
        // triangle 1
        elements[offsetArrayIndex] = offset + 3;
        elements[offsetArrayIndex + 1] = offset + 2;
        elements[offsetArrayIndex + 2] = offset + 0;

        // triangle 2
        elements[offsetArrayIndex + 3] = offset + 0;
        elements[offsetArrayIndex + 4] = offset + 2;
        elements[offsetArrayIndex + 5] = offset + 1;
    }

    public boolean hasRoom() {
        return this.hasRoom;
    }

    public boolean hasTextureRoom() {
        return this.textures.size() < MAX_TEXTURES;
    }

    public boolean hasTexture(Texture tex) {
        return this.textures.contains(tex);
    }

    public int getZIndex() {
        return this.zIndex;
    }

    public boolean isEmpty() {
        return this.numSprites == 0;
    }

    @Override
    public int compareTo(NewRenderBatch o) {
        return Integer.compare(this.zIndex, o.getZIndex());
    }

    public boolean remove(SpriteRenderer sprite) {
        for (int i = 0; i < numSprites; i++) {
            if (sprites[i] == sprite) {
                removeAt(i);
                return true;
            }
        }
        return false;
    }

    // shifts the following sprites down one slot and marks them dirty so their vertices are reloaded
    private void removeAt(int index) {
        for (int j = index; j < numSprites - 1; j++) {
            sprites[j] = sprites[j + 1];
            sprites[j].setDirty(true);
        }
        numSprites--;
        sprites[numSprites] = null;
        hasRoom = true;
    }
}
