package renderer;

import static org.lwjgl.opengl.GL11.*;
// import static org.lwjgl.opengl.GL46.*;
import java.nio.*;
import static org.lwjgl.system.MemoryUtil.NULL;
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
