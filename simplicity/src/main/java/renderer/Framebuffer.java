package renderer;

import static org.lwjgl.opengl.GL33.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL33.glDeleteTextures;
import static org.lwjgl.opengl.GL33.glViewport;
import static org.lwjgl.opengl.GL33.GL_DEPTH_COMPONENT32;
import static org.lwjgl.opengl.GL33.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL33.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL33.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL33.GL_RENDERBUFFER;
import static org.lwjgl.opengl.GL33.glBindFramebuffer;
import static org.lwjgl.opengl.GL33.glBindRenderbuffer;
import static org.lwjgl.opengl.GL33.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL33.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL33.glDeleteRenderbuffers;
import static org.lwjgl.opengl.GL33.glFramebufferRenderbuffer;
import static org.lwjgl.opengl.GL33.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL33.glGenFramebuffers;
import static org.lwjgl.opengl.GL33.glGenRenderbuffers;
import static org.lwjgl.opengl.GL33.glRenderbufferStorage;
import static org.lwjgl.opengl.GL33.*;

public class Framebuffer {
    private int fboID = 0, rboID = 0, width = 0, height = 0;
    private Texture texture = null;

    public Framebuffer(int width, int height) {
        this(width, height, GL_RGB, GL_RGB, GL_UNSIGNED_BYTE, GL_LINEAR);
    }

    /** Framebuffer whose color texture uses the given storage and filter (see Texture). */
    public Framebuffer(int width, int height, int internalFormat, int format, int type, int filter) {
        this.width = width;
        this.height = height;
        // generate framebuffer
        fboID = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);

        // create texture to render data to, & attach it to our framebuffer
        this.texture = new Texture(width, height, internalFormat, format, type, filter);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.texture.getId(), 0);

        // create renderbuffer & store depth info
        rboID = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rboID);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT32, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rboID);

        // check for errors
        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Framebuffer is not complete");
            // assert false : "Error: Framebuffer is not complete.";
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);
        glViewport(0, 0, width, height);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public int getFboID() {
        return this.fboID;
    }

    public int getTexId() {
        return this.texture.getId();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void destroy() {
        glDeleteFramebuffers(fboID);
        glDeleteRenderbuffers(rboID);
        glDeleteTextures(texture.getId());
    }
}