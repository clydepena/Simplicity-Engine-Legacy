package renderer;

import static org.lwjgl.opengl.GL33.*;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.lwjgl.system.MemoryUtil;

/**
 * A set of entity ids stored as a lookup texture: texel id is 1 if the id is in the set, 0 otherwise.
 * Ids are laid out in rows of ROW_WIDTH, so id lives at (id % ROW_WIDTH, id / ROW_WIDTH); shaders read the
 * row width from textureSize() and must treat ids past the last row as "not in the set".
 * The texture grows to fit the largest id and is only re-uploaded when the set changes.
 */
public class IdSetTexture {

    public static final int ROW_WIDTH = 1024;

    private int texId = 0;
    private int rows = 0;
    private ByteBuffer data;

    // what is currently uploaded, to skip uploads when nothing changed
    private int[] uploaded = new int[0];
    private int uploadedCount = -1;

    public IdSetTexture() {
        texId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, 0);
        allocate(1);
    }

    /** Makes the set exactly ids[0..count). Negative ids are ignored. Uploads only if the set changed. */
    public void set(int[] ids, int count) {
        if (count == uploadedCount && Arrays.equals(ids, 0, count, uploaded, 0, count)) return;

        int maxId = 0;
        for (int i = 0; i < count; i++) maxId = Math.max(maxId, ids[i]);
        int neededRows = maxId / ROW_WIDTH + 1;
        if (neededRows > rows) allocate(Math.max(neededRows, rows * 2));

        MemoryUtil.memSet(data, 0);
        for (int i = 0; i < count; i++) {
            if (ids[i] >= 0) data.put(ids[i], (byte) 0xFF);
        }

        glBindTexture(GL_TEXTURE_2D, texId);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, ROW_WIDTH, rows, GL_RED, GL_UNSIGNED_BYTE, data);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        glBindTexture(GL_TEXTURE_2D, 0);

        if (uploaded.length < count) uploaded = new int[Math.max(count, uploaded.length * 2)];
        System.arraycopy(ids, 0, uploaded, 0, count);
        uploadedCount = count;
    }

    private void allocate(int newRows) {
        rows = newRows;
        if (data != null) MemoryUtil.memFree(data);
        data = MemoryUtil.memCalloc(ROW_WIDTH * rows);

        glBindTexture(GL_TEXTURE_2D, texId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, ROW_WIDTH, rows, 0, GL_RED, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glBindTexture(GL_TEXTURE_2D, 0);
        uploadedCount = -1;   // new storage is undefined, force the next set() to upload
    }

    public int getTexId() {
        return texId;
    }

    public void destroy() {
        if (texId != 0) {
            glDeleteTextures(texId);
            texId = 0;
        }
        if (data != null) {
            MemoryUtil.memFree(data);
            data = null;
        }
    }
}
