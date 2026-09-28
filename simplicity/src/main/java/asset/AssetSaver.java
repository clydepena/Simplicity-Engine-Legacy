package asset;

import java.util.List;


public interface AssetSaver<T> {

    public Class<T> savedType();

    public List<String> supportedExtensions();

    public byte[] encode(T rawAsset);
}
