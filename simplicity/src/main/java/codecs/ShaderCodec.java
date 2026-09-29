package codecs;

import java.nio.charset.StandardCharsets;
import java.util.List;

import asset.AssetLoader;
import asset.AssetPoolHandler;
import renderer.Shader;

public class ShaderCodec implements AssetLoader<Shader, Shader> {

    public static final List<String> EXTENSIONS = List.of("glsl");

    @Override
    public List<String> supportedExtensions() {
        return EXTENSIONS;
    }

    @Override
    public Shader decode(byte[] bytes) {
        Shader shader = new Shader();
        shader.parseShaderSource(new String(bytes, StandardCharsets.UTF_8));
        return shader;
    }

    @Override
    public Shader finish(Shader decoded, AssetPoolHandler handler) {
        if (!decoded.compile()) {
            decoded.dispose();
            throw new IllegalStateException("shader failed to compile or link");
        }
        return decoded;
    }
}
