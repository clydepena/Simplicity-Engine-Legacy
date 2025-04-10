package renderer;

import static org.lwjgl.opengl.GL11.*;
// import static org.lwjgl.opengl.GL46.*;
import java.nio.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.lwjgl.BufferUtils;
import util.IOHelper;

public class Texture {
    private String filepath;
    private transient int texID;
    private int width, height;
    
    public Texture() {
        texID = -1;
        width = -1;
        height = -1;
    }

    public Texture(int width, int height) {
        this.filepath = "Generated";

        // generate texture on GPU
        texID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texID);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, NULL);
          
    }

    public void initFromRes(String filepath) {
        this.filepath = filepath;

        // IntBuffer channels = null;
        // IntBuffer width = null;
        // IntBuffer height = null;
        // ByteBuffer image = null;

        // try (MemoryStack stack = MemoryStack.stackPush()) {
        //     channels = stack.mallocInt(1);
        //     width = stack.mallocInt(1);
        //     height = stack.mallocInt(1);

        //     stbi_set_flip_vertically_on_load(true);
        //     image = stbi_load_from_memory(AssetUtil.ResToByteBuffer(filepath), width, height, channels, 4);
        //     if (image == null) {
        //         System.out.println("Could not load " + filepath);
        //     }
        // } catch (Exception e) {

        // }

        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);
        stbi_set_flip_vertically_on_load(true);
        ByteBuffer image = stbi_load_from_memory(IOHelper.ResToByteBuffer(filepath), width, height, channels, 4);

        genTexture(image, width, height, channels);
    }
    
    public void initFromExternal(String filepath) {
        this.filepath = filepath;

        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);
        stbi_set_flip_vertically_on_load(true);
        ByteBuffer image = stbi_load(filepath, width, height, channels, 0);
        
        genTexture(image, width, height, channels);
    }

    private void genTexture(ByteBuffer image, IntBuffer width, IntBuffer height, IntBuffer channels) {

        // generate texture on GPU
        texID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texID);

        // set texture parameters
        
        // repeat image in both directions
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        
        // pixelate when stretching image
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        
        // pixelate when shrinking image
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        if(image != null) {
            this.width = width.get(0);
            this.height = height.get(0);

            if(channels.get(0) == 3) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width.get(0), height.get(0), 0, GL_RGB, GL_UNSIGNED_BYTE, image);
            } else if(channels.get(0) == 4) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            } else {
                assert false : "Error (Texture) Unknown number of channels '" + channels.get(0) + "'";
            }
        } else {
            assert false : "Error: (Texture) Could not load image '" + filepath + "'";
        }
        
        stbi_image_free(image);
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, texID);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0 );
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getId() {
        return this.texID;
    }

    public String getFilepath() {
        return this.filepath;
    }

    @Override
    public boolean equals(Object o) {
        if(o == null) return false;
        if(!(o instanceof Texture)) return false;
        Texture oTex = (Texture) o;
        return oTex.getWidth() == this.width && oTex.getHeight() == this.height && oTex.getId() == this.texID && oTex.getFilepath().equals(this.filepath);
    }

}
