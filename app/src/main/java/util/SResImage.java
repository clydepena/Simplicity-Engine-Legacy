package util;

import static org.lwjgl.stb.STBImage.stbi_load;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.system.MemoryStack;

public class SResImage {
    
    private ByteBuffer pixels;
    private String resFilepath;
    private int width, height;

    public SResImage(String resFilepath) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.resFilepath = resFilepath;
            IntBuffer comp = stack.mallocInt(1);
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
    
            this.pixels = stbi_load_from_memory(AssetUtil.ResToByteBuffer(resFilepath), w, h, comp, 4);
            if (pixels == null) {
                System.out.println("Could not load " + resFilepath);
            }
            this.width = w.get();
            this.height = h.get();
        } catch (Exception e) {
            this.width = 0;
            this.height = 0;
            this.resFilepath = null;
            this.pixels = null;
        }
    }
    
    public ByteBuffer getImg() {
        return this.pixels;
    }

    public String getresFilepath() {
        return this.resFilepath;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}