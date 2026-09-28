package asset;

import java.util.List;

public interface AssetLoader<T> {

    public T load(byte[] bytes, AssetPool assetPool);

    /** Extensions without the dot, e.g. "png"; matched case-insensitively. */
    public List<String> supportedExtensions();

}
