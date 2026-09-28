package asset;

import java.util.List;

public interface AssetLoader<T> {

    public T load(byte[] bytes, AssetPoolHandler handler);

    public List<String> supportedExtensions();

}
