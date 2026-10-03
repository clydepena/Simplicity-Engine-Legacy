package renderer;

import static org.lwjgl.opengl.GL11.*;
// import static org.lwjgl.opengl.GL46.*;
import java.nio.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.lwjgl.BufferUtils;
import util.IOHelper;
import asset.Disposable;

public class Texture implements Disposable {
    private String filepath;
    private transient int texID;
    private int width, height;
    
    public Texture() {
        texID = -1;
        width = -1;
        height = -1;
    }

    public Texture(int width, int height) {
        this(width, height, GL_RGB, GL_RGB, GL_UNSIGNED_BYTE, GL_LINEAR);
    }

    /** Empty texture with the given storage (e.g. GL_RGB32F / GL_RGB / GL_FLOAT for an id buffer) and min/mag filter. */
    public Texture(int width, int height, int internalFormat, int format, int type, int filter) {
        this.filepath = "Generated";
        this.width = width;
        this.height = height;

        // generate texture on GPU
        texID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texID);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);

        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, NULL);
    }

    /** A texture from RGBA pixels (4 bytes per pixel, bottom row first). The pixels are copied to the GPU, so the caller keeps ownership of the buffer. */
    public Texture(int width, int height, ByteBuffer rgba) {
        this.filepath = "Generated";
        upload(width, height, rgba);
    }

    // Legacy loading path, only used by util.AssetPool (moved to to-refactor/depreciated). Textures now load
    // through TextureCodec and the asset pools; these and their two helpers are kept commented out for reference.
//     /** Old loading path (util.AssetPool): an image inside the jar. */
//     public void initFromRes(String filepath) {
//         this.filepath = filepath;
//         loadWithStb(IOHelper.ResToByteBuffer(filepath));
//     }
// 
//     /** Old loading path (util.AssetPool): an image file on disk. */
//     public void initFromExternal(String filepath) {
//         this.filepath = filepath;
//         IntBuffer width = BufferUtils.createIntBuffer(1);
//         IntBuffer height = BufferUtils.createIntBuffer(1);
//         IntBuffer channels = BufferUtils.createIntBuffer(1);
//         stbi_set_flip_vertically_on_load_thread(1);
//         ByteBuffer image = stbi_load(filepath, width, height, channels, 4);
//         uploadFromStb(image, width.get(0), height.get(0));
//     }
// 
//     private void loadWithStb(ByteBuffer encoded) {
//         IntBuffer width = BufferUtils.createIntBuffer(1);
//         IntBuffer height = BufferUtils.createIntBuffer(1);
//         IntBuffer channels = BufferUtils.createIntBuffer(1);
//         stbi_set_flip_vertically_on_load_thread(1);
//         if (encoded == null) {   // the file wasn't found: stb's failure reason would be left over from an older call
//             System.err.println("Error: (Texture) Could not read image '" + filepath + "'");
//             upload(0, 0, null);
//             return;
//         }
//         ByteBuffer image = stbi_load_from_memory(encoded, width, height, channels, 4);
//         uploadFromStb(image, width.get(0), height.get(0));
//     }
// 
//     private void uploadFromStb(ByteBuffer image, int width, int height) {
//         if (image == null) {
//             System.err.println("Error: (Texture) Could not load image '" + filepath + "': " + stbi_failure_reason());
//             upload(0, 0, null);   // keeps the old behaviour: a valid, empty texture
//             return;
//         }
//         upload(width, height, image);
//         stbi_image_free(image);
//     }

    /**
     * The one GPU upload every image path uses. Always RGBA: stb is asked for 4 channels,
     * so it returns RGBA whatever the file had (its reported channel count is the file's, not the data's).
     */
    private void upload(int width, int height, ByteBuffer rgba) {
        this.width = width;
        this.height = height;

        // generate texture on GPU
        texID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texID);

        // repeat image in both directions
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        // pixelate when stretching and shrinking the image
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        if (rgba != null) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, rgba);
        }
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, texID);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0 );
    }

    @Override
    public void dispose() {
        if (texID <= 0) return;
        glDeleteTextures(texID);
        texID = -1;
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
        return oTex.getWidth() == this.width && oTex.getHeight() == this.height && oTex.getId() == this.texID && java.util.Objects.equals(oTex.getFilepath(), this.filepath);   // null-safe
    }

}
