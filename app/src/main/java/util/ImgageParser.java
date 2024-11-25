package util;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import static org.lwjgl.stb.STBImage.*;

import org.lwjgl.system.MemoryStack;

public class ImgageParser {

    private ByteBuffer image;
    private int width, heigh;

    ImgageParser(int width, int heigh, ByteBuffer image) {
        this.image = image;
        this.heigh = heigh;
        this.width = width;
    }

    public ByteBuffer getImg() {
        return image;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return heigh;
    }


    public static ImgageParser load_image(String path) {
        ByteBuffer image;
        int width, heigh;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer comp = stack.mallocInt(1);
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);

            image = stbi_load(path, w, h, comp, 4);
            if (image == null) {
                // throw new resource_error("Could not load image resources.");
            }
            width = w.get();
            heigh = h.get();
        }
        return new ImgageParser(width, heigh, image);
    }
}