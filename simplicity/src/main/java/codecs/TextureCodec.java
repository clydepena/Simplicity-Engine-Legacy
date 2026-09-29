package codecs;

import java.nio.*;
import java.util.List;
import static org.lwjgl.stb.STBImage.*;
import org.lwjgl.BufferUtils;
import asset.AssetLoader;
import asset.AssetPoolHandler;
import asset.Disposable;
import renderer.Texture;

public class TextureCodec implements AssetLoader<Texture, TextureCodec.TextureData> {

    public static final List<String> EXTENSIONS = List.of("png", "jpg", "jpeg", "bmp", "tga");

    public static class TextureData implements Disposable {
        public int width;
        public int height;
        public ByteBuffer pixels;

        public TextureData(ByteBuffer pixels, int width, int height) {
            this.pixels = pixels;
            this.width = width;
            this.height = height;
        }

        @Override
        public void dispose() {
            if (pixels == null) return;
            stbi_image_free(pixels);
            pixels = null;
        }
    }

    @Override
    public List<String> supportedExtensions() {
        return EXTENSIONS;
    }

    @Override
    public TextureData decode(byte[] bytes) {
        ByteBuffer encoded = BufferUtils.createByteBuffer(bytes.length).put(bytes).flip();
        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);
        stbi_set_flip_vertically_on_load_thread(1);         
        ByteBuffer pixels = stbi_load_from_memory(encoded, width, height, channels, 4);
        if (pixels == null) {
            throw new IllegalStateException("can't decode image: " + stbi_failure_reason());
        }
        return new TextureData(pixels, width.get(0), height.get(0));
    }

    @Override
    public Texture finish(TextureData decoded, AssetPoolHandler handler) {
        try {
            return new Texture(decoded.width, decoded.height, decoded.pixels);
        } finally {
            decoded.dispose();
        }
    }
}
