package renderer;

import static org.lwjgl.opengl.GL46.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.system.MemoryUtil;

/**
 * Per-entity-id bit flags that shaders set, for picking passes that must see every sprite, not just the topmost:
 * a shader storage buffer of one uint per entity id ({@code layout(std430) buffer { uint flags[]; }}).
 * Shaders set bits with atomicOr and must ignore ids >= flags.length().
 * Usage: clear(capacity), bind() around the draw, then read().
 */
public class IdFlagBuffer {

    private int ssbo = 0;
    private int capacity = 0;   // number of uints (entity ids 0 .. capacity-1)
    private int binding = -1;

    public IdFlagBuffer() {
        ssbo = glGenBuffers();
    }

    /** Makes room for entity ids 0 .. capacity-1 and zeroes every flag. */
    public void clear(int capacity) {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo);
        if (capacity > this.capacity) {
            this.capacity = Math.max(capacity, this.capacity * 2);
            glBufferData(GL_SHADER_STORAGE_BUFFER, (long) this.capacity * Integer.BYTES, GL_DYNAMIC_READ);
        }
        glClearBufferData(GL_SHADER_STORAGE_BUFFER, GL_R32UI, GL_RED_INTEGER, GL_UNSIGNED_INT, (ByteBuffer) null);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    /** Binds the buffer to the shader storage binding point used by the shader (layout(binding = n)). */
    public void bind(int binding) {
        this.binding = binding;
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, binding, ssbo);
    }

    public void unbind() {
        if (binding < 0) return;
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, binding, 0);
        binding = -1;
    }

    /** The flags written since clear(), indexed by entity id (length = the capacity passed to clear, or more). */
    public int[] read() {
        int[] flags = new int[capacity];
        if (capacity == 0) return flags;

        // shader writes to storage buffers aren't visible to buffer reads without a barrier
        glMemoryBarrier(GL_BUFFER_UPDATE_BARRIER_BIT);

        IntBuffer data = MemoryUtil.memAllocInt(capacity);
        try {
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, ssbo);
            glGetBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, data);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
            data.get(flags);
        } finally {
            MemoryUtil.memFree(data);
        }
        return flags;
    }

    public void destroy() {
        if (ssbo != 0) {
            glDeleteBuffers(ssbo);
            ssbo = 0;
        }
    }
}
